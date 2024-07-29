package com.javax0.axsessgard.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.javax0.axsessgard.controller.ApplicationException
import com.javax0.axsessgard.initializer.KnownApplications
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class JwtService(private val algorithm: Algorithm) {

    fun verify(token: String): DecodedJWT {
        val issuer = JWT.decode(token).issuer
        val issuersAlgorithm = KnownApplications.algorithms[issuer]
            ?: throw ApplicationException(HttpStatus.UNAUTHORIZED, "Issuer $issuer is invalid")
        val verifier = JWT.require(issuersAlgorithm).build()
        return verifier.verify(token)
    }

    fun sign(builder: JWTCreator.Builder): String {
        return builder.sign(algorithm)
    }

}
