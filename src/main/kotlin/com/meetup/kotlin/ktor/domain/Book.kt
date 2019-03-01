package com.meetup.kotlin.ktor.domain

data class Book(
    val id: String? = null,
    val title: String,
    @Transient val version: Int = 1
)