package com.javax0.axsessgard.initializer

import com.javax0.axsessgard.model.ACE
import com.javax0.axsessgard.model.ACL
import com.javax0.axsessgard.model.Group
import com.javax0.axsessgard.model.PrincipalType
import com.javax0.axsessgard.repository.ACERepository
import com.javax0.axsessgard.repository.ACLRepository
import com.javax0.axsessgard.repository.GroupRepository
import com.javax0.axsessgard.utils.AXSG_INIT_DATA
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream

/**
 * Initializes the database if the database is empty. The initialization is done from the file `sample_data.txt` that
 * is read from the classpath. The file contains lines that start with `ACL:`, `ACE:`, `GROUP:`, and `MEMBER:`. The
 * `ACL:` lines define the ACLs, the `ACE:` lines define the Access Control Entries, the `GROUP:` lines define the
 * groups, and the `MEMBER:` lines define the members of the groups.
 */
@Component
class DataInitializer @Autowired constructor(
    private val aclRepository: ACLRepository,
    private val aceRepository: ACERepository,
    private val groupRepository: GroupRepository
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        val dataFile = System.getenv(AXSG_INIT_DATA) ?: System.getProperty(AXSG_INIT_DATA) ?: null
        if (dataFile != null &&
            aclRepository.count() == 0L && aceRepository.count() == 0L && groupRepository.count() == 0L
        ) {
            val lines: List<String>
            FileInputStream(File(dataFile)).use { fis ->
                lines = fis.bufferedReader().readLines()
            }
            var currentAcl: ACL? = null
            var currentGroup: Group? = null
            for (line in lines) {
                if (line.startsWith("ACL:")) {
                    currentAcl = ACL(name = line.substringAfter(":"), policy = null, owner = null)
                    currentAcl = aclRepository.save(currentAcl)
                }
                if (line.startsWith("POLICY:")) {
                    currentAcl = (currentAcl ?: throw IllegalStateException("POLICY line without ACL")
                            ).copy(policy = line.substringAfter(":"))
                    currentAcl = aclRepository.save(currentAcl)
                }

                if (line.startsWith("OWNER:")) {
                    currentAcl = (currentAcl ?: throw IllegalStateException("OWNER line without ACL")
                            ).copy(owner = line.substringAfter(":"))
                    currentAcl = aclRepository.save(currentAcl)
                }

                if (line.startsWith("ACE:")) {
                    val (_, type, principalId, operations) = line.split(":")
                    val ace = ACE(
                        principalId = principalId,
                        principalType = PrincipalType.valueOf(type),
                        operations = operations.split(",").map { it.trim() }.toMutableSet(),
                    )
                    (currentAcl ?: throw IllegalStateException("ACE line without ACL")
                            ).aces.add(ace)
                    aceRepository.save(ace)
                    currentAcl = aclRepository.save(currentAcl)

                }

                if (line.startsWith("GROUP:")) {
                    currentGroup = Group(name = line.substringAfter(":"), policy = null, owner = null)
                    groupRepository.save(currentGroup)
                }

                if (line.startsWith("MEMBER:") || line.startsWith("MEMBERS:")) {
                    ( currentGroup ?: throw IllegalStateException("MEMBER line without GROUP" )
                            ).members.addAll(line.substringAfter(":").split(",").map { it.trim() })
                    groupRepository.save(currentGroup)
                }
            }
        }
    }
}
