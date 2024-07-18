package com.javax0.axsessgard.util

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class TestContextInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {


    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        JwtTestUtil.setup()
    }
}

