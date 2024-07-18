package com.javax0.axsessgard

import com.javax0.axsessgard.repository.ACERepository
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.transaction.annotation.Transactional

@SpringBootApplication
class Application {

    @Bean
    @Transactional
    fun init(aclRepository: ACLRepository, aceRepository: ACERepository, groupRepository: GroupRepository) =
        CommandLineRunner {
        }

}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}