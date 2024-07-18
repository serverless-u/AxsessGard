package com.javax0.axsessgard.service

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.javax0.axsessgard.utils.KeyPairReader
import com.javax0.axsessgard.utils.PublicKeyReader
import org.springframework.stereotype.Service
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec

@Service
class JwtService {

    final val algorithm: Algorithm
    final var issuers: Map<String, Algorithm>

    init {
        algorithm = KeyPairReader().getAlgorithm()
        issuers = PublicKeyReader().loadIssuerConfigs()
    }

    fun decodeAndVerifyJwt(token: String): DecodedJWT {
        val issuer = JWT.decode(token).issuer
        val verifier = JWT.require(issuers[issuer]).build()
        return verifier.verify(token)
    }

    fun sign(builder: JWTCreator.Builder): String {
        return builder.sign(algorithm)
    }

    private fun generateECKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("EC")
        val ecSpec = ECGenParameterSpec("secp256r1")
        keyPairGenerator.initialize(ecSpec)
        return keyPairGenerator.generateKeyPair()
    }
}
