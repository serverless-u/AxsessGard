package com.javax0.axsessgard

import com.auth0.jwt.JWT
import com.google.gson.Gson
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.util.JwtTestUtil
import com.javax0.axsessgard.util.TestContextInitializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import kotlin.reflect.jvm.javaType
import kotlin.reflect.typeOf

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [TestContextInitializer::class])
class ApiControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Test
    fun testGetPermissions1() {
        val testToken = JwtTestUtil.createTestToken("issuer1", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $testToken")
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:$port/axsg/permissions",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user1")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("read")
    }

    @Test
    fun `Get an ACL which is not protected by any other ACL`() {
        val headers = HttpHeaders()
        headers.set("X-User-ID", "user1")
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:$port/axsg/acl/ReadOnlyAccess",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()
        val acl = Gson().fromJson(response.body, ACL::class.java)
        assertThat(acl).isEqualTo(
            Gson().fromJson(
                "{\"id\":1,\"name\":\"ReadOnlyAccess\"," +
                        "\"aces\":[" +
                        "{\"id\":1,\"principalId\":\"user1\",\"principalType\":\"USER\"," +
                        "\"operations\":[\"read\"]}," +
                        "{\"id\":3,\"principalId\":\"developers\",\"principalType\":\"GROUP\"," +
                        "\"operations\":[\"read\",\"write\",\"delete\"]}," +
                        "{\"id\":2,\"principalId\":\"ROLE_ADMIN\",\"principalType\":\"ROLE\",\"operations\":[\"read\",\"write\"]}]}",
                ACL::class.java
            )
        )

    }

    private fun get(
        url: String,
        user: String = "user1",
        headers: Map<String, String> = mapOf()
    ): ResponseEntity<String> {
        val httpHeaders = HttpHeaders()
        httpHeaders.set("X-User-ID", user)
        headers.forEach { (k, v) -> httpHeaders.set(k, v) }

        return restTemplate.exchange(
            url,
            HttpMethod.GET,
            HttpEntity<String>(httpHeaders),
            String::class.java
        )
    }

    private fun assert4XX(response1: ResponseEntity<String>) {
        assertThat(response1.statusCode.is4xxClientError).isTrue()
    }

    private fun assert2XX(response2: ResponseEntity<String>) {
        assertThat(response2.statusCode.is2xxSuccessful).isTrue()
    }

    private inline fun <reified T> assertResponse(response3: ResponseEntity<String>, expected: String) {
        val acl = Gson().fromJson<T>(response3.body, typeOf<T>().javaType)
        assertThat(acl).isEqualTo(
            Gson().fromJson<T>(expected, typeOf<T>().javaType)
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
        assertResponse<ACL>(
            response3,
            "{\"id\":2,\"name\":\"FullAccess\", \"policy\": \"ReadOnlyAccess\", " +
                    "\"aces\":[" +
                    "{\"id\":4,\"principalId\":\"user2\",\"principalType\":\"USER\"," +
                    "\"operations\":[\"read\",\"write\",\"delete\"]}," +
                    "{\"id\":5,\"principalId\":\"ROLE_SUPERADMIN\",\"principalType\":\"ROLE\"," +
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
    fun testGetPermissions2() {
        val testToken = JwtTestUtil.createTestToken("issuer2", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $testToken")
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:$port/axsg/permissions",
            HttpMethod.GET,
            entity,
            String::class.java
        )

        assertThat(response.statusCode.is2xxSuccessful).isTrue()

        // Decode and verify the response JWT
        val responseJwt = JWT.decode(response.body)
        assertThat(responseJwt.subject).isEqualTo("user1")
        assertThat(responseJwt.getClaim("permissions").asList(String::class.java)).contains("read")
    }


    //@Test
    fun testGetPermissionsFromDocker() {
        val testToken = JwtTestUtil.createTestToken("issuer2", "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

        val headers = HttpHeaders()
        headers.set("Authorization", "Bearer $testToken")
        val entity = HttpEntity<String>(headers)

        val response = restTemplate.exchange(
            "http://localhost:8080/axsg/permissions",
            HttpMethod.GET,
            entity,
            String::class.java
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