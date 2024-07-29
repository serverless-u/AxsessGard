package com.javax0.axsessgard.initializer

import com.auth0.jwt.algorithms.Algorithm
import com.javax0.axsessgard.utils.AlgorithmFactory
import com.javax0.axsessgard.utils.Configuration
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

@Component
class KnownApplications : CommandLineRunner {
    companion object {
        val algorithms = mutableMapOf<String, Algorithm>()
        val trusted = mutableSetOf<String>()
    }

    override fun run(vararg args: String?) {
        val file = File(Configuration.DIRECTORY, "applications")
        file.listFiles()?.iterator()?.forEach {
            it.useLines(Charsets.UTF_8) { lines ->
                var id: String? = null
                var algoType = "EC"
                var algo = "ECDSA256"
                lines.forEach { fullLine ->
                    val line = fullLine.trim()
                    when {
                        // empty and comment lines
                        line.isBlank() || line.startsWith("#") || line.startsWith("//") || line.startsWith("--") -> {}
                        line.startsWith("ID:") -> {
                            id = line.substring(3).trim()
                        }

                        line.startsWith("ALGO:") -> {
                            algo = line.substring(5).trim()
                        }

                        line.startsWith("ALGO_TYPE:") -> {
                            algoType = line.substring(10).trim()
                        }

                        line.equals("TRUSTED") -> {
                            trusted.add(id ?: throw IllegalStateException("ID not set for application"))
                        }

                        line.startsWith("KEY:") -> {
                            val key = line.substring(4).trim()
                            val keyFactory = KeyFactory.getInstance(algoType)
                            val publicKeyBytes = Base64.getDecoder().decode(key)
                            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicKeyBytes)) as ECPublicKey
                            id?.let { id ->
                                if (algorithms.containsKey(id)) {
                                    throw IllegalStateException("Application $id multiple defined")
                                }
                                algorithms[id] = AlgorithmFactory.createAlgorithm(algo, publicKey, null)
                            } ?: throw IllegalStateException("ID not set for application key")
                        }

                        else -> {
                            throw IllegalStateException("Unknown line: $line in the file '${file.absolutePath}'")
                        }
                    }
                }
            }
        }
    }
}
