package com.javax0.axsessgard.util

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.javax0.axsessgard.utils.AXSG_CONFIG_DIR
import com.javax0.axsessgard.utils.AlgorithmFactory
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*

class JwtTestUtil {

    companion object {
        private lateinit var algorithm: Algorithm
        private lateinit var issuer1Algorithm: Algorithm
        private lateinit var issuer2Algorithm: Algorithm

        fun setup() {
            val publicKeyPem =
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEzsLb427Ewa4dWp51L6ZcVrPpEYBzT6vcQaCmmR6BhmN1EoFxzFo3NiLmTh9CyonldHgI05ns8D54sn4jPnRJew=="
            val privateKeyPem =
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgncREArQ4aGEbETfG4Xnco73k3Z7nCYhDzfPUrpa5uJahRANCAATOwtvjbsTBrh1annUvplxWs+kRgHNPq9xBoKaZHoGGY3USgXHMWjc2IuZOH0LKieV0eAjTmezwPniyfiM+dEl7"

            val keyFactory = KeyFactory.getInstance("EC")
            val publicKey = keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyPem)))
            val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(privateKeyPem)))
            algorithm = AlgorithmFactory.createAlgorithm("ECDSA256", publicKey, privateKey)

            // set the environment variables (well, system properties)
            System.setProperty("AXSG_PUBLIC_KEY", publicKeyPem)
            System.setProperty("AXSG_PRIVATE_KEY", privateKeyPem)
            System.setProperty("AXSG_ALGO_TYPE", "EC")
            System.setProperty("AXSG_ALGO", "ECDSA256")

            System.setProperty(AXSG_CONFIG_DIR, "src/test/resources/config")

            val issuer1PublicKeyPem =
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo2SGwd5psDsfx1gwirzZP+udK1FlWl7t3Ho7tnZqJ+96oOgW/w3nKrXGU/SYbqOgdpB8D8A+Y4MqfCjmstOLFg=="
            val issuer1PrivateKeyPem =
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg9UPbdfQc/poGRgcgq6tyTsEEDmwP7SpCtQjcNFi2QXShRANCAASjZIbB3mmwOx/HWDCKvNk/650rUWVaXu3ceju2dmon73qg6Bb/DecqtcZT9Jhuo6B2kHwPwD5jgyp8KOay04sW"

            val issues1PublicKey =
                keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(issuer1PublicKeyPem)))
            val issuer1PrivateKey =
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(issuer1PrivateKeyPem)))
            issuer1Algorithm = AlgorithmFactory.createAlgorithm("ECDSA256", issues1PublicKey, issuer1PrivateKey)

            val issuer2PublicKeyPem =
                "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEo2SGwd5psDsfx1gwirzZP+udK1FlWl7t3Ho7tnZqJ+96oOgW/w3nKrXGU/SYbqOgdpB8D8A+Y4MqfCjmstOLFg=="
            val issuer2PrivateKeyPem =
                "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgbszCZsYgvbsn03t/T1vvyxQrJBo5ikxBghTTrkKT8BehRANCAAQsbsP0zG0dVd9U/8L9GsJ1V0+uqI6JCi+BwuDPI1sdkGxfF/IUmFhNY1IefuoIXV6ets9HXh7GFiHUGFc9UpbE"

            val issues2PublicKey =
                keyFactory.generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(issuer2PublicKeyPem)))
            val issuer2PrivateKey =
                keyFactory.generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(issuer2PrivateKeyPem)))
            issuer2Algorithm = AlgorithmFactory.createAlgorithm("ECDSA256", issues2PublicKey, issuer2PrivateKey)

        }

        fun createTestToken(issuer: String, subject: String, policy: String, roles: List<String>): String {
            if( issuer == "issuer1" ){
            return JWT.create()
                .withIssuer(issuer)
                .withSubject(subject)
                .withClaim("policy", policy)
                .withClaim("roles", roles)
                .withIssuedAt(Instant.now())
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(issuer1Algorithm)
            } else {
                return JWT.create()
                    .withIssuer(issuer)
                    .withSubject(subject)
                    .withClaim("policy", policy)
                    .withClaim("roles", roles)
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plusSeconds(3600))
                    .sign(issuer2Algorithm)

            }
        }
    }
}