package com.javax0.axsessgard.utils

import com.auth0.jwt.algorithms.Algorithm
import java.security.PrivateKey
import java.security.PublicKey
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.staticFunctions
import kotlin.reflect.jvm.jvmErasure

object AlgorithmFactory {
    private val algorithms: Map<String, (PublicKey, PrivateKey?) -> Algorithm> by lazy {
        Algorithm::class.staticFunctions
            .filter { method ->
                method.parameters.size == 2 &&
                        method.parameters[0].type.jvmErasure.isSubclassOf(java.security.PublicKey::class) &&
                        method.parameters[1].type.jvmErasure.isSubclassOf(java.security.PrivateKey::class) &&
                        method.returnType.jvmErasure == Algorithm::class
            }
            .associate { method ->
                method.name to { pub: PublicKey, priv: PrivateKey? -> method.call(pub, priv) as Algorithm }
            }
    }

    fun createAlgorithm(name: String, publicKey: PublicKey, privateKey: PrivateKey?): Algorithm {
        return algorithms[name]?.invoke(publicKey, privateKey)
            ?: throw IllegalArgumentException("Unsupported algorithm: $name")
    }

    fun getSupportedAlgorithms(): Set<String> = algorithms.keys
}