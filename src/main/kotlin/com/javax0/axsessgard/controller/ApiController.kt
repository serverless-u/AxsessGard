package com.javax0.axsessgard.controller

import com.auth0.jwt.JWT
import com.javax0.axsessgard.service.AccessControlService
import com.javax0.axsessgard.service.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class ApiController(
    private val jwtService: JwtService,
    private val accessControlService: AccessControlService
) {


    @Value("\${axsg.issuer}")
    private lateinit var issuer: String

    @Value("\${axsg.ttl}")
    private lateinit var ttlString: String

    private val ttl: Int
        get() = ttlString.toInt()

    @GetMapping("/axsg/permissions")
    fun getData(@RequestHeader("Authorization") authHeader: String): String {
        val jwtString = authHeader.replace("Bearer ", "")
        val jwt = jwtService.decodeAndVerifyJwt(jwtString)
        val policy = jwt.getClaim("policy").asString()
        val roles = jwt.getClaim("roles").asList(String::class.java)
        val permissions = accessControlService.permissions(jwt.subject, roles, policy)
        val response = JWT.create().withIssuer(issuer)
            .withIssuedAt(Instant.now())
            .withExpiresAt(Instant.now().plusSeconds(ttl.toLong()))
            .withSubject(jwt.subject)
            .withClaim("permissions", permissions)
            .sign(jwtService.algorithm)
        return response
    }
}