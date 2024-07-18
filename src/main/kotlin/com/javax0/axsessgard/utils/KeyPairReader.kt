package com.javax0.axsessgard.utils

import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

private const val AXSG_PUBLIC_KEY = "AXSG_PUBLIC_KEY"
private const val AXSG_PRIVATE_KEY = "AXSG_PRIVATE_KEY"
private const val AXSG_ALGO_TYPE = "AXSG_ALGO_TYPE"
private const val AXSG_ALGO = "AXSG_ALGO"

class KeyPairReader {

    fun getAlgorithm(): Algorithm {
        val algoType = System.getenv(AXSG_ALGO_TYPE) ?: System.getProperty(AXSG_ALGO_TYPE) ?: "EC"
        val keyFactory = KeyFactory.getInstance(algoType)

        val publicKeyBase64 = System.getenv(AXSG_PUBLIC_KEY) ?: System.getProperty(AXSG_PUBLIC_KEY)
        ?: throw IllegalStateException("$AXSG_PUBLIC_KEY environment variable not set")
        val publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64)
        val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes)) as ECPublicKey

        val privateKeyBase64 = System.getenv(AXSG_PRIVATE_KEY) ?: System.getProperty(AXSG_PRIVATE_KEY)
        ?: throw IllegalStateException("$AXSG_PUBLIC_KEY environment variable not set")
        val privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64)
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateKeyBytes)) as ECPrivateKey

        val algo = System.getenv(AXSG_ALGO) ?: System.getProperty(AXSG_ALGO) ?: "ECDSA256"
        val algorithm = AlgorithmFactory.createAlgorithm(algo, publicKey, privateKey)
        return algorithm
    }

}