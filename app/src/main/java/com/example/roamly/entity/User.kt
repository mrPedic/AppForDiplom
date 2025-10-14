package com.example.roamly.entity

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long? = null,
    val name: String = "",
    val login: String = "",
    val password: String = "",
    var role: Role = Role.UnRegistered,
    var email: String = ""
)

enum class Role{
    Registered,
    UnRegistered,
    AdminOfInstitution,
    AdminOfApp
}