package com.javax0.axsessgard.config

import com.auth0.jwt.algorithms.Algorithm
import com.javax0.axsessgard.utils.AlgorithmFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Configuration
class AlgorithmConfig {

    @Bean
    fun algorithmFactory(): Algorithm {
        val decoder = Base64.getDecoder()
        val publicKeyBytes = decoder.decode(get("AXSG_PUBLIC_KEY"))
        val privateKeyBytes = decoder.decode(get("AXSG_PRIVATE_KEY"))
        val algoType = get("AXSG_ALGO_TYPE")
        val algo = get("AXSG_ALGO")

        val keyFactory = KeyFactory.getInstance(algoType)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes))
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes))
        return AlgorithmFactory.createAlgorithm(algo, publicKey, privateKey)
    }

    private fun get(s: String) =
        (System.getenv(s) ?: System.getProperty(s) ?: throw IllegalStateException("Key not found"))

}