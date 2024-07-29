package com.javax0.axsessgard

import com.auth0.jwt.JWT
import com.javax0.axsessgard.repository.ACERepository
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
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
import org.springframework.test.context.ContextConfiguration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [TestContextInitializer::class])
class ApiControllerIntegrationTest {

    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    @Autowired
    private lateinit var aceRepository: ACERepository

    @Autowired
    private lateinit var aclRepository: ACLRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

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


    @Test
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