package com.javax0.axsessgard

import com.auth0.jwt.JWT
import com.javax0.axsessgard.model.ACE
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.model.Group
import com.javax0.axsessgard.model.PrincipalType
import com.javax0.axsessgard.repository.ACERepository
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
import com.javax0.axsessgard.service.AccessControlService
import com.javax0.axsessgard.util.JwtTestUtil
import com.javax0.axsessgard.util.TestContextInitializer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.io.ClassPathResource
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
    private lateinit var accessControlService: AccessControlService


    @Autowired
    private lateinit var aceRepository: ACERepository

    @Autowired
    private lateinit var aclRepository: ACLRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    companion object {
        private var initialized = false
    }

    @BeforeEach
    fun setup() {
        if( initialized ) return
        initialized = true
        var currentAcl: ACL? = null
        var currentGroup: Group? = null
        val resource = ClassPathResource("sample_data.txt")
        val lines = resource.inputStream.bufferedReader().readLines()
        for (line in lines) {
            if (line.startsWith("ACL:")) {
                currentAcl = ACL(name = line.substringAfter("ACL:"))
                currentAcl = aclRepository.save(currentAcl)
            }

            if (line.startsWith("ACE:")) {
                val (_, type, principalId, operations) = line.split(":")
                val ace = ACE(
                    principalId = principalId,
                    principalType = PrincipalType.valueOf(type),
                    operations = operations.split(",").toMutableSet(),
                )
                currentAcl!!.aces.add(ace)
                aceRepository.save(ace)
                currentAcl = aclRepository.save(currentAcl)

            }

            if (line.startsWith("GROUP:")) {
                currentGroup = Group(name = line.substringAfter("GROUP:"))
                groupRepository.save(currentGroup)
            }

            if (line.startsWith("MEMBER:")) {
                currentGroup?.members?.add(line.substringAfter("MEMBER:"))
                groupRepository.save(currentGroup!!)
            }
        }
    }

    @Test
    fun testGetPermissions() {
        val testToken = JwtTestUtil.createTestToken("issuer1" , "user1", "ReadOnlyAccess", listOf("ROLE_USER"))

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
    fun testAccessControl() {
        val hasPermission = accessControlService.hasPermission("user1", listOf("ROLE_USER"), "ReadOnlyAccess", "read")
        assertThat(hasPermission).isTrue()

        val permissions = accessControlService.permissions("user1", listOf("ROLE_USER"), "ReadOnlyAccess")
        assertThat(permissions).contains("read")
    }
}