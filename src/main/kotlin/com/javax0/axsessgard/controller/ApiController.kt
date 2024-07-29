package com.javax0.axsessgard.controller

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.initializer.KnownApplications
import com.javax0.axsessgard.service.AccessControlService
import com.javax0.axsessgard.service.JwtService
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
                .withClaim("permissions", permissions)
        )
    }


    /**
     * Get the content of the ACL.
     *
     * If the acl is not protected by any policy, then anyone can read it, even unauthenticated users.
     *
     * If the acl is protected by a policy, then the user has to provide a JWT token signed by a trusted application
     * to identify the user.
     * The service checks the policy and if the user has `read` right then it will retun the content of the ACL.
     *
     */
    @GetMapping("/axsg/acl/{id}")
    fun getAxl(@RequestHeader("Authorization") authHeader: String?, @PathVariable id: String): String {
        val acl = accessControlService.acl(id) ?: throw ApplicationException(HttpStatus.NOT_FOUND, "ACL $id not found")
        val permissions =
            if (acl.policy != null) {
                if (authHeader == null) {
                    throw ApplicationException(HttpStatus.UNAUTHORIZED, "Authorization header is missing")
                }
                val jwtString = authHeader.replace(BEARER_PATTERN, "")
                val issuer = JWT.decode(jwtString).issuer
                if (!KnownApplications.trusted.contains(issuer)) {
                    throw ApplicationException(HttpStatus.UNAUTHORIZED, "Issuer $issuer is not trusted")
                }
                val jwt = jwtService.verify(jwtString)
                val subject = jwt.subject
                val roles = if (acl.owner == subject) listOf("owner") else listOf()
                accessControlService.permissions(jwt.subject, roles, acl.policy)
            } else listOf("read")
        if (permissions.contains("read")) {
            return Gson().toJson(acl)
        } else {
            throw ApplicationException(HttpStatus.UNAUTHORIZED,"Access denied")
        }
    }
}