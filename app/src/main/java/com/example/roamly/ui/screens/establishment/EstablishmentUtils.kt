package com.example.roamly.ui.screens.establishment

import android.util.Log


/**
 * Функция: Конвертирует строку с сервера обратно в Map<День, Часы> для UI.
 * ИСПРАВЛЕННАЯ ВЕРСИЯ: Гарантирует наличие всех 7 дней, сохраняя часы работы.
 */
fun convertHoursStringToMap(hoursString: String?): Map<String, String> {

    Log.d("HoursInputCheck", "Входящая строка расписания: '$hoursString'")

    // 1. Парсим пришедшую строку в Map<День, Часы (с пробелами)>
    val rawMap = if (hoursString.isNullOrBlank()) {
        emptyMap()
    } else {
        hoursString.split("|")
            .mapNotNull { dayEntry ->

                // ⭐ КЛЮЧЕВОЕ ИСПРАВЛЕНИЕ: Используем split с ограничением в 2 элемента
                val parts = dayEntry.split(":", limit = 2)

                if (parts.size == 2) {
                    val day = parts[0].trim()

                    // parts[1] теперь содержит полную строку времени, например "08:30-18:00"
                    var hours = parts[1].trim()

                    // ⭐ ВОССТАНАВЛИВАЕМ ПРОБЕЛЫ (08:30 - 18:00)
                    if (hours.contains("-")) {
                        hours = hours.replace("-", " - ")
                    }

                    // Возвращаем (День) -> (Часы или Закрыто)
                    day to hours
                } else {
                    Log.w("HoursParser", "Некорректная запись расписания: $dayEntry. Размер частей: ${parts.size}")
                    null
                }
            }
            .toMap(mutableMapOf())
    }

    // 2. Создаем финальную Map, гарантируя наличие всех 7 дней и корректных часов
    val fullMap = mutableMapOf<String, String>()

    DAYS_OF_WEEK.forEach { day ->
        val hours = rawMap[day]

        val finalHours = when {
            // Используем часы из rawMap, если они есть и не содержат "Закрыто"
            !hours.isNullOrBlank() && !hours.contains("Закрыто", ignoreCase = true) -> hours
            // В противном случае, это "Закрыто"
            else -> "Закрыто"
        }

        fullMap[day] = finalHours
    }

    Log.d("OpStatusMap", "Финальное расписание: $fullMap")

    return fullMap.toMap()
}

/**
 * Функция 1: Конвертирует Map<День, Часы> в строку для отправки на сервер.
 */
fun convertHoursMapToString(hoursMap: Map<String, String>): String {
    // Используем полный список дней, чтобы гарантировать порядок и наличие
    return DAYS_OF_WEEK.joinToString("|") { day ->
        val hours = hoursMap[day] ?: "" // Если дня нет в карте, отправляем пустую строку
        // ⭐ Убираем пробелы при сохранении
        val cleanedHours = hours.replace(" ", "").trim()
        "${day}:${cleanedHours}"
    }
}