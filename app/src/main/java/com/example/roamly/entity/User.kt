package com.example.roamly.entity

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("Id") val id: Long? = null,
    @SerializedName("Name") val name: String = "",
    @SerializedName("Login") val login: String = "",
    @SerializedName("Password") val password: String = "",
    @SerializedName("Role") var role: Role = Role.UnRegistered,
    @SerializedName("Email") var email: String = ""
)

enum class Role{
    Registered,
    UnRegistered,
    AdminOfInstitution,
    AdminOfApp
}