package com.javax0.axsessgard.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

class JwtTestUtil {

    companion object {
        private lateinit var algorithm: Algorithm
        private lateinit var publicKey: ECPublicKey
        private lateinit var privateKey: ECPrivateKey

        fun setup() {
            val publicKeyPem = JwtTestUtil::class.java.getResource("/public_key.pem")?.readText()
                ?: throw IllegalStateException("Public key not found")
            val privateKeyPem = JwtTestUtil::class.java.getResource("/private_key_pkcs8.pem")?.readText()
                ?: throw IllegalStateException("Private key not found")

            val keyFactory = KeyFactory.getInstance("EC")

            publicKey = keyFactory.generatePublic(
                X509EncodedKeySpec(
                    Base64.getDecoder().decode(
                        publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                            .replace("-----END PUBLIC KEY-----", "")
                            .replace("\n", "")
                    )
                )
            ) as ECPublicKey

            privateKey = keyFactory.generatePrivate(
                PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(
                        privateKeyPem.replace("-----BEGIN PRIVATE KEY-----", "")
                            .replace("-----END PRIVATE KEY-----", "")
                            .replace("\n", "")
                    )
                )
            ) as ECPrivateKey

            algorithm = Algorithm.ECDSA256(publicKey, privateKey)

            // Set the public key in the system property
            val publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.encoded)
            System.setProperty("AXSG_PUBLIC_KEY", publicKeyBase64)

            // Set the private key in the system property
            val privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.encoded)
            System.setProperty("AXSG_PRIVATE_KEY", privateKeyBase64)

            System.setProperty("AXSG_KEY_FILE", "src/test/resources/issuers.json")
        }

        fun createTestToken(issuer: String, subject: String, policy: String, roles: List<String>): String {
            return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withClaim("policy", policy)
                .withClaim("roles", roles)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(algorithm)
        }
    }
}