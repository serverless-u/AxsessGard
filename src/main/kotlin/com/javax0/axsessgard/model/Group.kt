package com.javax0.axsessgard.model

import jakarta.persistence.*

@Entity
@Table(name = "groups")
data class Group(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val name: String,

    @ElementCollection
    @CollectionTable(name = "group_members", joinColumns = [JoinColumn(name = "group_id")])
    @Column(name = "user_id")
    val members: MutableSet<String> = mutableSetOf()
)
