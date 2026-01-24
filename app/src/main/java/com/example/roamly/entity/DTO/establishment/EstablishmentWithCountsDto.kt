package com.example.roamly.entity.DTO.establishment

import com.example.roamly.entity.classes.EstablishmentStatus
import com.example.roamly.entity.classes.TypeOfEstablishment

data class EstablishmentWithCountsDto(
    val id: Long,
    val name: String,
    val address: String,
    val type: TypeOfEstablishment,
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: EstablishmentStatus = EstablishmentStatus.ACTIVE,
    val rating: Double = 0.0,
    val dateOfCreation: String = "",
    val menuId: Long = -1,
    val createdUserId: Long = -1,
    val photoBase64s: List<String> = emptyList(),
    val operatingHoursString: String? = null,
    val pendingOrdersCount: Int = 0,
    val pendingBookingsCount: Int = 0
) {
    fun toDisplayDto(): EstablishmentDisplayDto {
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
            photoBase64s = this.photoBase64s,
            operatingHoursString = this.operatingHoursString
        )
    }
}