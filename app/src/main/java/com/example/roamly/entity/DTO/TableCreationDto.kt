package com.example.roamly.entity.DTO

import com.example.roamly.entity.TableUIModel
import com.google.gson.annotations.SerializedName

data class TableCreationDto(
    @SerializedName("name")
    val name: String,
    @SerializedName("description")
    val description: String,
    @SerializedName("maxCapacity")
    val maxCapacity: Int
    // establishmentId будет передан в пути URL, поэтому здесь не нужен
)

// Вам также понадобится функция маппинга из TableUIModel (из Compose-экрана)
fun TableUIModel.toCreationDto(): TableCreationDto {
    return TableCreationDto(
        name = this.name,
        description = this.description,
        maxCapacity = this.maxCapacity
    )
}