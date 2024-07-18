package com.javax0.axsessgard.service

import com.javax0.axsessgard.model.PrincipalType
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
import org.springframework.stereotype.Service

@Service
class AccessControlService(
    private val aclRepository: ACLRepository,
    private val groupRepository: GroupRepository
) {
    fun hasPermission(userId: String, roles: List<String>, aclName: String, operation: String): Boolean {
        val acl = aclRepository.findByName(aclName) ?: return false

        return acl.aces.any { ace ->
            when (ace.principalType) {
                PrincipalType.USER -> ace.principalId == userId && ace.operations.contains(operation)
                PrincipalType.ROLE -> roles.contains(ace.principalId) && ace.operations.contains(operation)
                PrincipalType.GROUP -> {
                    val group = groupRepository.findByName(ace.principalId)
                    group?.members?.contains(userId) == true && ace.operations.contains(operation)
                }
            }
        }
    }

    fun permissions(userId: String, roles: List<String>, aclName: String): List<String> {
        val allPermissions = mutableListOf<String>()

        val acl = aclRepository.findByName(aclName) ?: return emptyList()

        for (ace in acl.aces) {
            when (ace.principalType) {
                PrincipalType.USER -> {
                    if (ace.principalId == userId) {
                        allPermissions.addAll(ace.operations)
                    }
                }

                PrincipalType.ROLE -> {
                    if (roles.contains(ace.principalId)) {
                        allPermissions.addAll(ace.operations)
                    }
                }

                PrincipalType.GROUP -> {
                    val group = groupRepository.findByName(ace.principalId)
                    if (group?.members?.contains(userId) == true) {
                        allPermissions.addAll(ace.operations)
                    }
                }
            }
        }
        return allPermissions
    }
}