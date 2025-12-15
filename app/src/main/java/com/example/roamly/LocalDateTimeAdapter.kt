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

    // Форматы в порядке приоритета
    private val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME, // С миллисекундами
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"), // Без миллисекунд
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS") // Явно с микросекундами
    )

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime {
        val dateString = json.asString
        if (dateString.isNullOrBlank()) {
            throw JsonParseException("Date string is null or blank.")
        }

        // Пробуем все форматы по порядку
        for (formatter in formatters) {
            try {
                return LocalDateTime.parse(dateString, formatter)
            } catch (e: DateTimeParseException) {
                // Пробуем следующий формат
            }
        }

        throw JsonParseException("Could not parse date: $dateString")
    }

    override fun serialize(
        src: LocalDateTime,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }
}