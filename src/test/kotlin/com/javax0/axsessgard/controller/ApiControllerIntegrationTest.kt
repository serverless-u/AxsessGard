package com.javax0.axsessgard

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.model.ACE
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.util.JwtTestUtil
import com.javax0.axsessgard.util.TestContextInitializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.MultiValueMap
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [TestContextInitializer::class])
class ApiControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun testGetPermissions1() {
        val testToken = JwtTestUtil.createTestToken("issuer1", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val response = get(
            "http://localhost:$port/axsg/permissions",
            headers = mapOf("Authorization" to "Bearer $testToken"),
            user = null
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user1")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("read")
    }

    @Test
    fun `Get an ACL which is not protected by any other ACL`() {

        val response = get(
            "http://localhost:$port/axsg/acl/ReadOnlyAccess",
            headers = mapOf("X-User-ID" to "user1")
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        val acl = Gson().fromJson(response.body, ACL::class.java)
        assertThat(acl).isEqualTo(
            Gson().fromJson(
                "{\"id\":1,\"name\":\"ReadOnlyAccess\"," +
                        "\"aces\":[" +
                        "{\"id\":1,\"principalId\":\"user1\"," +
                        "\"operations\":[\"read\"]}," +
                        "{\"id\":3,\"principalId\":\"developers\"," +
                        "\"operations\":[\"read\",\"write\",\"delete\"]}," +
                        "{\"id\":2,\"principalId\":\"ROLE_ADMIN\",\"operations\":[\"read\",\"write\"]}]}",
                ACL::class.java
            )
        )

    }

    private fun get(
        url: String,
        user: String? = "user1",
        headers: Map<String, String> = mapOf()
    ): ResponseEntity<String> {
        return send(url, user, headers, null, "GET")
    }


    private fun put(
        url: String,
        user: String? = "user1",
        headers: Map<String, String> = mapOf(),
        acl: ACL
    ): ResponseEntity<String> {
        return send(url, user, headers, acl, "PUT")
    }

    private fun post(
        url: String,
        user: String? = "user1",
        headers: Map<String, String> = mapOf(),
        acl: ACL
    ): ResponseEntity<String> {
        return send(url, user, headers, acl, "POST")
    }


    private fun send(
        url: String,
        user: String? = "user1",
        headers: Map<String, String> = mapOf(),
        acl: ACL?,
        method: String = "POST"
    ): ResponseEntity<String> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection

        connection.requestMethod = method
        connection.doOutput = true

        // Set headers
        if (user != null) {
            connection.setRequestProperty("X-User-ID", user)
        }
        headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }

        if (acl != null) {
            // Write ACL to request body
            connection.setRequestProperty("Content-Type", "application/json")
            val json = Gson().toJson(acl)
            val writer = OutputStreamWriter(connection.outputStream)
            writer.write(json) // Assuming ACL has a proper toString() method
            writer.flush()
            writer.close()
        }
        // Get response
        val responseCode = connection.responseCode
        val responseBody =
            if (responseCode == 200) BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() } else ""

        connection.disconnect()
        val responseHeaders: MultiValueMap<String, String> = HttpHeaders()
        connection.headerFields.filter { it.key != null }
            .forEach { (k, v) -> responseHeaders.addAll(k, v) }
        return ResponseEntity(responseBody, responseHeaders, HttpStatus.valueOf(responseCode))
    }


    private fun assert4XX(response1: ResponseEntity<String>) {
        assertThat(response1.statusCode.is4xxClientError).isTrue()
    }

    private fun assert2XX(response2: ResponseEntity<String>) {
        assertThat(response2.statusCode.is2xxSuccessful).isTrue()
    }

    private fun assertACL(response3: ResponseEntity<String>, expected: String) {
        val acl = Gson().fromJson(response3.body, ACL::class.java)
        val maskedAcl = acl.copy(
            id = 0,
            aces = acl.aces.map { it.copy(id = 0) }.toMutableSet()
        )
        assertThat(maskedAcl).isEqualTo(
            Gson().fromJson(expected, ACL::class.java)
        )
    }

    private fun getLocation(response: ResponseEntity<String>): String {
        return response.headers["Location"]?.get(0) ?: throw IllegalStateException("Location header is missing")
    }

    private fun getRequestAuthorization(response: ResponseEntity<String>): String {
        return response.headers["X-Request-Authorization"]?.get(0)
            ?: throw IllegalStateException("Location header is missing")
    }

    @Test
    fun `Get an ACL protected`() {
        val response1 = get("http://localhost:$port/axsg/acl/FullAccess")
        assert4XX(response1)
        val authRequest = getRequestAuthorization(response1)
        val location = getLocation(response1)

        val response2 = get(location, headers = mapOf("Authorization" to "Bearer $authRequest"))
        assert2XX(response2)

        val response3 = get(
            "http://localhost:$port/axsg/acl/FullAccess",
            headers = mapOf("Authorization" to "Bearer ${response2.body}")
        )
        assert2XX(response3)
        assertACL(
            response3,
            "{\"id\":0,\"name\":\"FullAccess\", \"policy\": \"ReadOnlyAccess\", " +
                    "\"aces\":[" +
                    "{\"id\":0,\"principalId\":\"user2\"," +
                    "\"operations\":[\"read\",\"write\",\"delete\"]}," +
                    "{\"id\":0,\"principalId\":\"ROLE_SUPERADMIN\"," +
                    "\"operations\":[\"read\",\"admin\",\"write\",\"delete\"]}]}"
        )
    }

    @Test
    fun `Refused for ACL having a permission for a different resource`() {
        val response1 = get("http://localhost:$port/axsg/acl/DummyAccess")
        assert4XX(response1)
        val authRequest = getRequestAuthorization(response1)
        val location = getLocation(response1)

        val response2 = get(location, headers = mapOf("Authorization" to "Bearer $authRequest"))
        assert2XX(response2)

        val response3 = get(
            "http://localhost:$port/axsg/acl/FullAccess",
            headers = mapOf("Authorization" to "Bearer ${response2.body}")
        )
        assert4XX(response3)
    }


    @Test
    fun `Refused for ACL which is user has no right to read`() {
        val response1 = get("http://localhost:$port/axsg/acl/FullAccess", "user2")

        assert4XX(response1)

        val authRequest = getRequestAuthorization(response1)
        val location = getLocation(response1)

        val response2 = get(
            location,
            "user2",
            headers = mapOf("Authorization" to "Bearer $authRequest")
        )
        assert2XX(response2)

        val response3 = get(
            "http://localhost:$port/axsg/acl/FullAccess",
            "user2",
            headers = mapOf("Authorization" to "Bearer ${response2.body}")
        )

        assert4XX(response3)
    }

    @Test
    fun `Refused for ACL when the token is for a different user`() {
        val response1 = get("http://localhost:$port/axsg/acl/FullAccess", "user1")
        assert4XX(response1)
        val authRequest = getRequestAuthorization(response1)
        val location = getLocation(response1)

        val response2 = get(
            location,
            "user1",
            headers = mapOf("Authorization" to "Bearer $authRequest")
        )
        assert2XX(response2)

        val response3 = get(
            "http://localhost:$port/axsg/acl/FullAccess",
            "user2",
            headers = mapOf("Authorization" to "Bearer ${response2.body}")
        )

        assert4XX(response3)
    }

    @Test
    fun `Post a new ACL and then Update it using PUT`() {
        val response1 = post(
            "http://localhost:$port/axsg/acl",
            "user2",
            acl = ACL(
                name = "NewAccess", policy = "ReadOnlyAccess", owner = "user1", aces = mutableSetOf(
                    ACE(
                        principalId = "user2",
                        operations = mutableSetOf("read", "write", "delete")
                    ),
                    ACE(
                        principalId = "developers",
                        operations = mutableSetOf("read", "write")
                    ),
                )
            )
        )
        assert4XX(response1)
        val authRequest = getRequestAuthorization(response1)
        val location = getLocation(response1)

        val response2 = get(
            location,
            "user1",
            headers = mapOf("Authorization" to "Bearer $authRequest")
        )
        assert2XX(response2)

        val response3 = post(
            "http://localhost:$port/axsg/acl",
            "user2",
            headers = mapOf("Authorization" to "Bearer ${response2.body}"),
            acl = ACL(
                name = "NewAccess", policy = "FullAccess", owner = "user1", aces = mutableSetOf(
                    ACE(
                        principalId = "user2",
                        operations = mutableSetOf("read", "write", "delete", "horwardan")
                    ),
                    ACE(
                        principalId = "developers",
                        operations = mutableSetOf("read", "write")
                    ),
                )
            )
        )
        assert2XX(response3)

        val response4 = put(
            "http://localhost:$port/axsg/acl/NewAccess",
            "user2",
            headers = mapOf("Authorization" to "Bearer ${response2.body}"),
            acl = ACL(
                name = "NewAccess", policy = "ReadOnlyAccess", owner = "user1", aces = mutableSetOf(
                    ACE(
                        principalId = "user2",
                        operations = mutableSetOf("read", "write", "delete")
                    ),
                    ACE(
                        principalId = "developers",
                        operations = mutableSetOf("read", "write")
                    ),
                )
            )
        )

        assert4XX(response4)
        val authRequest1 = getRequestAuthorization(response4)
        val location1 = getLocation(response4)

        val response5 = get(
            location1,
            "user1",
            headers = mapOf("Authorization" to "Bearer $authRequest1")
        )
        assert2XX(response5)

        val response6 = put(
            "http://localhost:$port/axsg/acl/NewAccess",
            "user2",
            headers = mapOf("Authorization" to "Bearer ${response5.body}"),
            acl = ACL(
                name = "NewAccess", policy = "ReadOnlyAccess", owner = "user1", aces = mutableSetOf(
                    ACE(
                        principalId = "user2",
                        operations = mutableSetOf("read", "write", "delete", "horwarden")
                    ),
                    ACE(
                        principalId = "developers",
                        operations = mutableSetOf("read", "write")
                    ),
                )
            )
        )
        assert2XX(response6)

        val testToken = JwtTestUtil.createTestToken("issuer2", "user2", "NewAccess", listOf())

        val response = get(
            "http://localhost:$port/axsg/permissions",
            headers = mapOf("Authorization" to "Bearer $testToken")
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user2")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("horwarden")


    }

    @Test
    fun `Get Permission URL and JWT for POST`() {

        val response = get("http://localhost:$port/axsg/post/acl")

        assert4XX(response)
        assertThat(getLocation(response)).isNotNull()
    }

    @Test
    fun `Get Permission URL and JWT for PUT unprotected`() {

        val response = get("http://localhost:$port/axsg/put/acl/ReadOnlyAccess")

        assert2XX(response) // not protected
    }

    @Test
    fun `Get Permission URL and JWT for PUT protected`() {

        val response = get("http://localhost:$port/axsg/put/acl/FullAccess")

        assert4XX(response)
        assertThat(getLocation(response)).isNotNull()
    }


    @Test
    fun testGetPermissions2() {
        val testToken = JwtTestUtil.createTestToken("issuer2", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val response = get(
            "http://localhost:$port/axsg/permissions",
            headers = mapOf("Authorization" to "Bearer $testToken")
        )

        assert2XX(response)

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user1")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("read")
    }


    //@Test
    fun testGetPermissionsFromDocker() {
        val testToken = JwtTestUtil.createTestToken("issuer2", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val response = get(
            "http://localhost:8080/axsg/permissions",
            headers = mapOf("Authorization" to "Bearer $testToken")
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user1")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("read")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("write")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("delete")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java).size).isEqualTo(3)
    }

}