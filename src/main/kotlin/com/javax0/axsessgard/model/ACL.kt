package com.javax0.axsessgard.model

import jakarta.persistence.*

@Entity
@Table(name = "acls")
data class ACL(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,

    @Column(unique = false, nullable = true)
    val policy: String?,

    @Column(unique = false, nullable = true)
    val owner: String?,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "acl_id")
    val aces: MutableSet<ACE> = mutableSetOf()
)
