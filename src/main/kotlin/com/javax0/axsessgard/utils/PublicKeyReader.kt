package com.javax0.axsessgard.utils

import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*


private const val KEY_FILE_ENV = "AXSG_KEY_FILE"

class PublicKeyReader {

    private data class IssuerConfig(
        val key: String, // the base 64 encoded public key without the leading "-----BEGIN PUBLIC KEY-----" and trailing "-----END PUBLIC KEY-----"
        val algo: String, // the name of the algorithm to use
        val algoType: String // the type of the algorithm to use, that is currently "EC" or "RSA"
    )

    fun loadIssuerConfigs(): Map<String, Algorithm> {
        val filePath = System.getenv(KEY_FILE_ENV) ?: System.getProperty(KEY_FILE_ENV)
        ?: throw IllegalStateException("$KEY_FILE_ENV environment variable not set")

        val json = File(filePath).readText()

        val type = object : TypeToken<Map<String, IssuerConfig>>() {}.type
        val map: Map<String, IssuerConfig> = Gson().fromJson(json, type)
        return map.map { (k, v) ->
            val keyFactory = KeyFactory.getInstance(v.algoType)
            val publicKeyBytes = Base64.getDecoder().decode(v.key)
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes)) as ECPublicKey
            k to AlgorithmFactory.createAlgorithm(v.algo, publicKey, null)
        }.toMap()
    }

}
