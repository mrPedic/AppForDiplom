package com.example.roamly.classes.cl_menu

// =================================================================
// ⭐ 1. Главный класс меню
// ID меню - это ID заведения (1:1 связь)
// =================================================================
data class MenuOfEstablishment(
    // ID меню - это внешний ключ к заведению. Сервер знает,
    // что меню с таким ID принадлежит заведению с таким же ID.
    val establishmentId: Long,
    var foodGroups: MutableList<FoodGroup> = mutableListOf(),
    var drinksGroups: MutableList<DrinksGroup> = mutableListOf()
)

// =================================================================
// ⭐ 2. Класс для представления цены/объема напитка
// =================================================================
data class DrinkOption(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому напитку относится эта опция
    val drinkId: Long? = null,
    var sizeMl: Int,
    var cost: Double
)

// =================================================================
// ⭐ 3. Базовый интерфейс для элементов меню
// =================================================================
interface MenuItem {
    var id: Long? // ID присваивает сервер
    var name: String
    var caloriesPer100g: Double
    var fatPer100g: Double
    var carbohydratesPer100g: Double
    var proteinPer100g: Double
    var ingredients: String
    var photoBase64: String?
}

// =================================================================
// ⭐ 4. Класс "Еда" (Food)
// =================================================================
data class Food(
    override var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какой группе еды относится это блюдо
    var foodGroupId: Long? = null,
    override var name: String,
    override var caloriesPer100g: Double,
    override var fatPer100g: Double,
    override var carbohydratesPer100g: Double,
    override var proteinPer100g: Double,
    override var ingredients: String,
    var cost: Double,
    var weight: Int,
    override var photoBase64: String? = null
) : MenuItem

// =================================================================
// ⭐ 5. Класс "Напиток" (Drink)
// =================================================================
data class Drink(
    override var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какой группе напитков относится этот напиток
    var drinkGroupId: Long? = null,
    override var name: String,
    override var caloriesPer100g: Double,
    override var fatPer100g: Double,
    override var carbohydratesPer100g: Double,
    override var proteinPer100g: Double,
    override var ingredients: String,
    // При получении с сервера DrinkOption будут иметь drinkId = Drink.id
    var options: MutableList<DrinkOption>,
    override var photoBase64: String? = null
) : MenuItem

// =================================================================
// ⭐ 6. Классы для групп (FoodGroup и DrinksGroup)
// =================================================================
data class FoodGroup(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому меню относится эта группа (равен establishmentId)
    val establishmentId: Long? = null,
    var name: String,
    // Items теперь просто список, т.к. Food содержит foodGroupId
    var items: MutableList<Food> = mutableListOf()
)

data class DrinksGroup(
    var id: Long? = null, // ID присваивает сервер
    // Внешний ключ: к какому меню относится эта группа (равен establishmentId)
    val establishmentId: Long? = null,
    var name: String,
    // Items теперь просто список, т.к. Drink содержит drinkGroupId
    var items: MutableList<Drink> = mutableListOf()
)