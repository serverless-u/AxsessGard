package com.javax0.axsessgard.model

import jakarta.persistence.*


@Entity
@Table(name = "aces")
data class ACE(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val principalId: String,

    @ElementCollection
    @CollectionTable(name = "ace_operations", joinColumns = [JoinColumn(name = "ace_id")])
    @Column(name = "operation")
    val operations: MutableSet<String> = mutableSetOf(),
)

