package com.example.roamly.classes.cl_menu

class Drinks constructor(
    val name: String,
    val calories: Double,
    val fat: Double,
    val carbohydrates: Double,
    val protein: Double,
    val ingredients: String,
    var cost: MutableList<Double>,
    var portionSize: MutableList<Int>
){
}