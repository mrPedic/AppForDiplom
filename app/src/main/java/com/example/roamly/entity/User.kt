package com.example.roamly.entity

data class User(
    val name:String = "",
    val id: Long = 0 ,
    val password: String = "",
    val login: String = "",
    var isLoggedIn : Boolean = false
)
