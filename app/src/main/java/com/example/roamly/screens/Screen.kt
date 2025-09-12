package com.example.roamly.screens

const val DETAIL_ARGUMENT_KEY = "id"
const val DETAIL_ARGUMENT_KEY2 = "name"
const val AUTHENTICATION_ROUTE = "authentication"
const val ROOT_ROUTE = "root"
const val HOME_ROUTE = "home"


sealed class Screen(val route: String) {
    object Home: Screen(route = "home_screen")
    object Detail:
        Screen(route = "detail_screen?id={id}&name={name}"){
        fun passId(id: Int = 0): String {
            return "detail_screen?id=$id"
        }

        fun passNameAndId(
            id: Int = 0,
            name: String = "GOYDA"
        ): String{
            return "detail_screen?id=$id&name=$name"
        }
    }

    object Login: Screen(route = "login_screen")

    object SingUp:  Screen(route = "sing_up_screen")
}