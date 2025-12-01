package com.example.roamly.entity

import android.graphics.Color
import com.example.roamly.entity.DTO.EstablishmentDisplayDto

data class EstablishmentEntity(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val description: String,
    val rating: Double = 0.0,
    val dateOfCreation: String,
    val menuId: Long = -1,
    val createdUserId: Long,
    val status: EstablishmentStatus,
    val type: TypeOfEstablishment,
    val photoBase64s: List<String> = emptyList(),
    val operatingHoursString: String? = null
)

enum class EstablishmentStatus {
    // На рассмотрении администрации
    PENDING_APPROVAL,
    // Отклонено
    REJECTED,
    // Временно неактивно
    DISABLED,
    ACTIVE
}


enum class TypeOfEstablishment{
    Restaurant,
    Cafe,
    Pub,
    Canteen,
    FastFood,
    CoffeeHouse,
    Pizzeria,
    Bakery,
    SushiBar,
    GrillBar,
    Confectionery,
    Diner,
    TeaHouse,
    PancakeHouse,
    IceCreamParlor,
    FoodTruck,
    Gastropub
}

/**
 * Логика определения цвета для PointBuilder
 * @param typeOfEstablishment тип заведения
 */

fun convertTypeToColor(typeOfEstablishment: TypeOfEstablishment): Int {
    return when(typeOfEstablishment){
        TypeOfEstablishment.Restaurant -> Color.parseColor("#8B0000")
        TypeOfEstablishment.Cafe -> Color.parseColor("#A0522D")
        TypeOfEstablishment.Pub -> Color.parseColor("#000080")
        TypeOfEstablishment.Canteen -> Color.parseColor("#6B8E23")
        TypeOfEstablishment.FastFood -> Color.parseColor("#FF4500")
        TypeOfEstablishment.CoffeeHouse -> Color.parseColor("#D2B48C")
        TypeOfEstablishment.Pizzeria -> Color.parseColor("#FF8C00")
        TypeOfEstablishment.Bakery -> Color.parseColor("#FFD700")
        TypeOfEstablishment.SushiBar -> Color.parseColor("#F0F8FF")
        TypeOfEstablishment.GrillBar -> Color.parseColor("#36454F")
        TypeOfEstablishment.Confectionery -> Color.parseColor("#FF69B4")
        TypeOfEstablishment.Diner -> Color.parseColor("#40E0D0")
        TypeOfEstablishment.TeaHouse -> Color.parseColor("#00A86B")
        TypeOfEstablishment.PancakeHouse -> Color.parseColor("#FFCC99")
        TypeOfEstablishment.IceCreamParlor -> Color.parseColor("#B57EDC")
        TypeOfEstablishment.FoodTruck -> Color.parseColor("#CCFF00")
        TypeOfEstablishment.Gastropub -> Color.parseColor("#6B8E23")
        else -> Color.parseColor("#6B8E23")
    }
}

fun convertTypeToWord(typeOfEstablishment: TypeOfEstablishment): String{
    return when(typeOfEstablishment){
        TypeOfEstablishment.Restaurant -> "Ресторан"
        TypeOfEstablishment.Cafe -> "Кафе"
        TypeOfEstablishment.Pub -> "Бар" // Или "Паб"
        TypeOfEstablishment.Canteen -> "Столовая"
        TypeOfEstablishment.FastFood -> "Фастфуд"
        TypeOfEstablishment.CoffeeHouse -> "Кофейня"
        TypeOfEstablishment.Pizzeria -> "Пиццерия"
        TypeOfEstablishment.Bakery -> "Пекарня / Булочная"
        TypeOfEstablishment.SushiBar -> "Суши-бар"
        TypeOfEstablishment.GrillBar -> "Гриль-бар / Стейкхаус"
        TypeOfEstablishment.Confectionery -> "Кондитерская"
        TypeOfEstablishment.Diner -> "Закусочная / Бистро"
        TypeOfEstablishment.TeaHouse -> "Чайная"
        TypeOfEstablishment.PancakeHouse -> "Блинная"
        TypeOfEstablishment.IceCreamParlor -> "Кафе-мороженое"
        TypeOfEstablishment.FoodTruck -> "Фудтрак / Киоск"
        TypeOfEstablishment.Gastropub -> "Гастропаб"
        else -> "Неизвестный тип заведения"
    }
}

fun EstablishmentEntity.toDisplayDto(): EstablishmentDisplayDto {
    return EstablishmentDisplayDto(
        id = this.id,
        name = this.name,
        description = this.description,
        address = this.address,
        latitude = this.latitude,
        longitude = this.longitude,
        status = this.status,
        rating = this.rating,
        dateOfCreation = this.dateOfCreation,
        type = this.type,
        menuId = this.menuId,
        createdUserId = this.createdUserId,
//        createdId = this.createdUserId,
        photoBase64s = this.photoBase64s,
        operatingHoursString = this.operatingHoursString,
    )

}
