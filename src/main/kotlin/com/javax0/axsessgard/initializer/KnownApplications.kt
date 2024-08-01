package com.javax0.axsessgard.initializer

import com.auth0.jwt.algorithms.Algorithm
import com.javax0.axsessgard.utils.AlgorithmFactory
import com.javax0.axsessgard.utils.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * Read the files from the applications folder under the configuration directory and initialize the known applications.
 * Each application should have a unique ID, an algorithm type, an algorithm, and a public key.
 * Each of these values are specified in the file in the format:
 *
 * ```
 *   KEY: value
 * ```
 *
 * The possible keys are:
 *
 * * `ID` is the name of the application
 * * `ALGO` is the algorithm to use, default is `ECDSA256` or the algorithm specified the last time in the given file
 * * `ALGO_TYPE` is the type of the algorithm, default is `EC` or the algorithm specified the last time in the given file
 * * `KEY` is the public key of application base 64 encoded
 *
 * Lines starting with `//`, `#` and empty lines are ignored.
 */
@Component
class KnownApplications (private val algorithm: Algorithm): CommandLineRunner {
    companion object {
        val algorithms = mutableMapOf<String, Algorithm>()
    }
    @Value("\${axsg.issuer}")
    private lateinit var issuer: String

    override fun run(vararg args: String?) {
        algorithms[issuer] = algorithm
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
