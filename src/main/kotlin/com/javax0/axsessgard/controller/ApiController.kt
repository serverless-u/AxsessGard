package com.javax0.axsessgard.controller

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.service.AccessControlService
import com.javax0.axsessgard.service.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.Instant

@RestController
class ApiController(
    private val jwtService: JwtService,
    private val accessControlService: AccessControlService
) {

    companion object {
        private val BEARER_PATTERN = Regex("Bearer\\s+")
    }

    @Value("\${axsg.issuer}")
    private lateinit var issuer: String

    @Value("\${axsg.ttl}")
    private lateinit var ttlString: String

    private val ttl: Int
        get() = ttlString.toInt()

    /**
     * Get the permissions for the user identified by the JWT.
     * The permissions are calculated based on the roles and the policy.
     * The policy is taken from the JWT.
     * The roles are taken from the JWT.
     *
     * This API is open and can be called by anyone, even unauthenticated users.
     * The only requirement is that the `Authorization` header contains a valid JWT
     * signed by one of the authorized issuers.
     *
     */
    @GetMapping("/axsg/permissions")
    fun getPermissions(@RequestHeader("Authorization") authHeader: String): String {
        val jwtString = authHeader.replace(BEARER_PATTERN, "")
        val jwt = jwtService.verify(jwtString)
        val policy = jwt.getClaim("policy").asString()
        val roles = jwt.getClaim("roles").asList(String::class.java)
        val permissions = accessControlService.permissions(jwt.subject, roles, policy)
        return jwtService.sign(
            JWT.create().withIssuer(issuer)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(ttl.toLong()))
                .withSubject(jwt.subject)
                .withClaim("policy", policy)
                .withClaim("permissions", permissions)
        )
    }

    /**
     * Get the permission list from the JWT following the `Bearer` keyword provided in the `Authorization` header.
     */
    private fun getPermissions(
        authHeader: String?,
        userId: String?,
        policy: String,
        roles: List<String>
    ): List<String> {
        userId ?: throw RequestAuthorization(policy, roles)
        authHeader ?: throw RequestAuthorization(policy, roles)
        val jwtStrings = authHeader.replace(BEARER_PATTERN, "")
        for (jwtString in jwtStrings.split(",")) {
            val jwt = jwtService.verify(jwtString)
            val permissions = jwt.getClaim("permissions")
            val jwtPolicy = jwt.getClaim("policy")?.asString() ?: ""
            if (jwt.subject == userId && jwtPolicy == policy && permissions != null) {
                return permissions.asList(String::class.java)
            }
        }
        throw RequestAuthorization(policy, roles)
    }

    /**
     * Get the content of the ACL.
     *
     * If the acl is not protected by any policy, then anyone can read it, even unauthenticated users.
     *
     * If the acl is protected by a policy, then the user has to be identified.
     * The service checks the policy and if the user has `read` right then it will return the content of the ACL.
     *
     */
    @GetMapping("/axsg/acl/{name}")
    fun getAcl(
        @RequestHeader("x-user-id", required = false) userId: String?,
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @PathVariable name: String
    ): String {
        val acl =
            accessControlService.acl(name) ?: throw ApplicationException(HttpStatus.NOT_FOUND, "ACL $name not found")
        val policy = acl.policy
        val permissions =
            if (policy != null) {
                userId ?: throw ApplicationException(HttpStatus.UNAUTHORIZED, "Unknown user")
                val roles = if (acl.owner == userId) listOf("owner") else listOf()
                getPermissions(authHeader, userId, policy, roles)
            } else {
                listOf("read")
            }
        if (permissions.contains("read")) {
            return Gson().toJson(acl)
        } else {
            throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied")
        }
    }


    @GetMapping("/axsg/put/acl/{name}")
    fun getPutPermission(
        @RequestHeader("x-user-id", required = false) userId: String?,
        @PathVariable name: String
    ): ResponseEntity<String> {
        val acl =
            accessControlService.acl(name) ?: throw ApplicationException(HttpStatus.NOT_FOUND, "ACL $name not found")
        val policy = acl.policy
            if (policy != null) {
                userId ?: throw ApplicationException(HttpStatus.UNAUTHORIZED, "Unknown user")
                val roles = if (acl.owner == userId) listOf("owner") else listOf()
                throw RequestAuthorization(policy, roles)
            }
        return ResponseEntity<String>(HttpStatus.OK)
    }

    @GetMapping("/axsg/post/acl")
    fun getPostPermission(
        @RequestHeader("x-user-id", required = false) userId: String?
    ): ResponseEntity<String> {
            throw RequestAuthorization("createAcl", listOf())
    }

    /**
     * Update an existing ACL.
     * You cannot PUT and ACL if it does not exist yet.
     * To do that, you have to use POST.
     */
    @PutMapping("/axsg/acl/{name}")
    fun putAcl(
        @RequestHeader("x-user-id", required = false) userId: String?,
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @PathVariable name: String,
        @RequestBody aclNew: ACL
    ): String {
        val acl =
            accessControlService.acl(name) ?: throw ApplicationException(HttpStatus.NOT_FOUND, "ACL $name not found")
        val policy = acl.policy
        val permissions =
            if (policy != null) {
                if (userId == null) throw ApplicationException(HttpStatus.UNAUTHORIZED, "Unknown user")
                val roles = if (acl.owner == userId) listOf("owner") else listOf()
                getPermissions(authHeader, userId, policy, roles)
            } else {
                listOf("write")
            }
        permissions.contains("write") || throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied")
        accessControlService.update(aclNew.copy(id = acl.id, name = name))
        return ResponseEntity<String>(HttpStatus.OK).toString()
    }

    @PostMapping("/axsg/acl")
    fun postAcl(
        @RequestHeader("x-user-id", required = false) userId: String?,
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @RequestBody aclNew: ACL
    ): String {
        val permissions = getPermissions(authHeader, userId, "createAcl", listOf())
        permissions.contains("create") || throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied")
        accessControlService.update(aclNew.copy(id = 0))
        return ResponseEntity<String>(HttpStatus.OK).toString()
    }

    @GetMapping("/echo")
    fun echoRequest(@RequestHeader headers: Map<String, String>, request: HttpServletRequest): Map<String, Any> {
        val requestInfo = mutableMapOf<String, Any>()

        // Add request headers
        val headerMap = headers.mapKeys { it.key.lowercase() } // Normalize header keys to lowercase
        requestInfo["headers"] = headerMap

        // Add request method and URL
        requestInfo["method"] = request.method
        requestInfo["requestURL"] = request.requestURL.toString()
        requestInfo["requestURI"] = request.requestURI

        // Add query parameters
        val queryParams = request.parameterMap.mapValues { it.value.joinToString(", ") }
        requestInfo["queryParams"] = queryParams

        return requestInfo
    }
}