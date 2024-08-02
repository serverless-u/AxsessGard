package com.javax0.axsessgard.service

import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.repository.ACLRepository
import org.springframework.stereotype.Service

@Service
class AccessControlService(
    private val aclRepository: ACLRepository
) {

    fun find(query:String): List<ACL> {
        return aclRepository.findByNameStartingWith(query)
    }

    fun update(acl: ACL): ACL {
        return aclRepository.save(acl)
    }

    fun delete(acl: ACL) {
        return aclRepository.delete(acl)
    }

    fun acl(id: String): ACL? {
        return aclRepository.findByName(id)
    }

    fun permissions(userId: String, roles: List<String>, aclName: String): List<String> {
        val allPermissions = mutableSetOf<String>()

        val acl = aclRepository.findByName(aclName) ?: return emptyList()

        for (ace in acl.aces) {
            if (ace.principalId == userId) {
                allPermissions.addAll(ace.operations)
            }
            if (roles.contains(ace.principalId)) {
                allPermissions.addAll(ace.operations)
            }
        }
        return allPermissions.toList()
    }
}