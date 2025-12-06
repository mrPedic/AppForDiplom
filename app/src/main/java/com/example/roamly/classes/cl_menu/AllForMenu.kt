package com.example.roamly.classes.cl_menu

data class MenuOfEstablishment(
    // ID меню - это внешний ключ к заведению. Сервер знает,
    // что меню с таким ID принадлежит заведению с таким же ID.
    val establishmentId: Long,
    var foodGroups: MutableList<FoodGroup> = mutableListOf(),
    var drinksGroups: MutableList<DrinksGroup> = mutableListOf()
)

data class DrinkOption(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому напитку относится эта опция
    val drinkId: Long? = null,
    var sizeMl: Int,
    var cost: Double
)

interface MenuItem {
    var id: Long? // ID присваивает сервер
    var name: String? // <-- ИСПРАВЛЕНО (было String)
    var caloriesPer100g: Double
    var fatPer100g: Double
    var carbohydratesPer100g: Double
    var proteinPer100g: Double
    var ingredients: String? // <-- ИСПРАВЛЕНО (было String)
    var photoBase64: String?
}

data class Food(
    override var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какой группе еды относится это блюдо
    var foodGroupId: Long? = null,
    override var name: String?, // <-- ИСПРАВЛЕНО
    override var caloriesPer100g: Double,
    override var fatPer100g: Double,
    override var carbohydratesPer100g: Double,
    override var proteinPer100g: Double,
    override var ingredients: String?, // <-- ИСПРАВЛЕНО
    var cost: Double,
    var weight: Int,
    override var photoBase64: String? = null
) : MenuItem

data class Drink(
    override var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какой группе напитков относится этот напиток
    var drinkGroupId: Long? = null,
    override var name: String?, // <-- ИСПРАВЛЕНО (Это устранит ошибку в Drink.copy)
    override var caloriesPer100g: Double,
    override var fatPer100g: Double,
    override var carbohydratesPer100g: Double,
    override var proteinPer100g: Double,
    override var ingredients: String?, // <-- ИСПРАВЛЕНО
    // При получении с сервера DrinkOption будут иметь drinkId = Drink.id
    var options: MutableList<DrinkOption>,
    override var photoBase64: String? = null
) : MenuItem


data class FoodGroup(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому меню относится эта группа (равен establishmentId)
    val establishmentId: Long? = null,
    var name: String?, // <-- ИСПРАВЛЕНО
    // Items теперь просто список, т.к. Food содержит foodGroupId
    var items: MutableList<Food> = mutableListOf()
)

data class DrinksGroup(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому меню относится эта группа (равен establishmentId)
    val establishmentId: Long? = null,
    var name: String?, // <-- ИСПРАВЛЕНО
    // Items теперь просто список, т.к. Drink содержит drinkGroupId
    var items: MutableList<Drink> = mutableListOf()
)