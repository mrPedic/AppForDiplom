package com.example.roamly

class PointBuilder(
    val latitude: Double,
    val longitude: Double,
    val typeOfEstablishment : TypeOfEstablishment,
    val idEstablishment: Long
) {

}


val convertTypeToColor = mutableMapOf<TypeOfEstablishment, Any>(
    TypeOfEstablishment.Restaurant      to 0x80800000.toInt(),
    TypeOfEstablishment.Cafe            to 0x808B4513.toInt(),
    TypeOfEstablishment.CoffeeShop      to 0x808B4513.toInt(),
    TypeOfEstablishment.Bistro          to 0x80FF4500.toInt(),
    TypeOfEstablishment.Bar             to 0x80191970.toInt(),
    TypeOfEstablishment.Canteen         to 0x8090EE90.toInt(),
    TypeOfEstablishment.Bakery          to 0x80F5DEB3.toInt(),
    TypeOfEstablishment.Confectionery   to 0x80FFC0CB.toInt(),
    TypeOfEstablishment.FastFood        to 0x80FF0000.toInt(),
    TypeOfEstablishment.Pizzeria        to 0x80008000.toInt(),
    TypeOfEstablishment.BurgerJoint     to 0x80000000.toInt(),
    TypeOfEstablishment.SushiBar        to 0x80FFFFFF.toInt(),
    TypeOfEstablishment.FoodCourt       to 0x80FF0000.toInt(),
    TypeOfEstablishment.Gastrobar       to 0x80696969.toInt(),
    TypeOfEstablishment.HookahLounge    to 0x808A2BE2.toInt(),
)

enum class TypeOfEstablishment{
    Restaurant,
    Cafe,
    CoffeeShop,
    Bistro,
    Bar,
    Canteen,
    Bakery,
    Confectionery,
    FastFood,
    Pizzeria,
    BurgerJoint,
    SushiBar,
    FoodCourt,
    Gastrobar,
    HookahLounge
}

private fun ConvertFromEnToRuEstablishment(englishName : String): String {
    when(englishName){
        "Restaurant" -> return "Ресторан"
        "Cafe" -> return "Кафе"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        "Restaurant" -> return "Ресторан"
        else -> return ""
    }
}