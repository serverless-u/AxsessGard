package com.javax0.axsessgard.repository

import com.javax0.axsessgard.model.ACE
import com.javax0.axsessgard.model.ACL
import org.springframework.data.jpa.repository.JpaRepository

interface ACLRepository : JpaRepository<ACL, Long> {
    fun findByName(name: String): ACL?
}

interface ACERepository : JpaRepository<ACE, Long>
