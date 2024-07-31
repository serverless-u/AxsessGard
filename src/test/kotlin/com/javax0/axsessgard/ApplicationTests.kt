package com.javax0.axsessgard

import com.javax0.axsessgard.util.TestContextInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration

@SpringBootTest
@ContextConfiguration(initializers = [TestContextInitializer::class])
class ApplicationTests {

    //@Test
    fun contextLoads() {
    }
}
