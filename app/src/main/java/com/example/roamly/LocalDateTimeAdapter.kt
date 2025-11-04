package com.example.roamly

import com.google.gson.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.annotation.RequiresApi
import android.os.Build

/**
 * Адаптер для Gson, позволяющий корректно сериализовать и десериализовать
 * объекты LocalDateTime в/из строки в формате ISO 8601, который возвращает сервер: YYYY-MM-DDTHH:MM:SS.
 */
@RequiresApi(Build.VERSION_CODES.O)
class LocalDateTimeAdapter : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {

    // ⭐ ЯВНО ОПРЕДЕЛЯЕМ ФОРМАТ, КОТОРЫЙ ИСПОЛЬЗУЕТ СЕРВЕР.
    // Если сервер возвращает "2025-11-05T08:30:00", этот паттерн идеален.
    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    /**
     * Десериализация (Чтение JSON -> Объект Kotlin)
     */
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime {
        val dateString = json.asString
        if (dateString.isNullOrBlank()) {
            throw JsonParseException("Date string is null or blank.")
        }

        return try {
            // Используем явный форматтер для парсинга
            LocalDateTime.parse(dateString, formatter)
        } catch (e: DateTimeParseException) {
            // Для более информативного сообщения об ошибке
            throw JsonParseException(
                "Failed to parse LocalDateTime: $dateString. Check if server date format matches expected patterns.",
                e
            )
        }
    }

    /**
     * Сериализация (Объект Kotlin -> Запись JSON)
     */
    override fun serialize(
        src: LocalDateTime,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        // Используем явный форматтер для форматирования
        return JsonPrimitive(src.format(formatter))
    }
}