package com.javax0.axsessgard.controller

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.service.AccessControlService
import com.javax0.axsessgard.service.JwtService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
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
     * Get the content of the ACL.
     *
     * If the acl is not protected by any policy, then anyone can read it, even unauthenticated users.
     *
     * If the acl is protected by a policy, then the user has to be identified.
     * The service checks the policy and if the user has `read` right then it will return the content of the ACL.
     *
     */
    @GetMapping("/axsg/acl/{id}")
    fun getAxl(
        @RequestHeader("x-user-id", required = false) userId: String?,
        @RequestHeader("Authorization", required = false) authHeader: String?,
        @PathVariable id: String
    ): String {
        val acl = accessControlService.acl(id) ?: throw ApplicationException(HttpStatus.NOT_FOUND, "ACL $id not found")
        val policy = acl.policy
        val permissions : List<String>
        if (policy != null) {
            if (userId == null) throw ApplicationException(HttpStatus.UNAUTHORIZED, "Unknown user")
            if (authHeader == null) {
                val roles = if (acl.owner == userId) listOf("owner") else listOf()
                throw RequestAuthorization("", policy, roles)
            }
            val jwtString = authHeader.replace(BEARER_PATTERN, "")
            val jwt = jwtService.verify(jwtString)
            if( jwt.subject != userId ) throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied, user mismatch")
            if( jwt.getClaim("policy").asString() != policy ) throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied, policy mismatch")
            permissions = jwt.getClaim("permissions")?.asList(String::class.java) ?: listOf()
        } else permissions = listOf("read")
        if (permissions.contains("read")) {
            return Gson().toJson(acl)
        } else {
            throw ApplicationException(HttpStatus.UNAUTHORIZED, "Access denied")
        }
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