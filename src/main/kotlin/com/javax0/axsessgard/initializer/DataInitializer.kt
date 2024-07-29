package com.javax0.axsessgard.initializer

import com.javax0.axsessgard.model.ACE
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.model.Group
import com.javax0.axsessgard.model.PrincipalType
import com.javax0.axsessgard.repository.ACERepository
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class DataInitializer @Autowired constructor(
    private val aclRepository: ACLRepository,
    private val aceRepository: ACERepository,
    private val groupRepository: GroupRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        if (aclRepository.count() == 0L && aceRepository.count() == 0L && groupRepository.count() == 0L) {
            var currentAcl: ACL? = null
            var currentGroup: Group? = null
            val resource = ClassPathResource("sample_data.txt")
            val lines = resource.inputStream.bufferedReader().readLines()
            for (line in lines) {
                if (line.startsWith("ACL:")) {
                    currentAcl = ACL(name = line.substringAfter("ACL:"), policy = null, owner = null)
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
                    currentGroup = Group(name = line.substringAfter("GROUP:"), policy = null, owner = null)
                    groupRepository.save(currentGroup)
                }

                if (line.startsWith("MEMBER:")) {
                    currentGroup?.members?.add(line.substringAfter("MEMBER:"))
                    groupRepository.save(currentGroup!!)
                }
            }
        }
    }
}
