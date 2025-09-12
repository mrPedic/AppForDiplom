package com.example.roamly.fetcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object GistFetcher {
    private const val GIST_RAW_URL = "https://gist.githubusercontent.com/mrPedic/0e49796782fe065f86c7befac3d8aad1/raw/server_url.json"

    suspend fun fetchBaseUrl(): String? {
        return withContext(Dispatchers.IO) {
            var connection: HttpsURLConnection? = null
            try {
                connection = URL(GIST_RAW_URL).openConnection() as HttpsURLConnection
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                json.getString("server_url")
            } catch (e: Exception) {
                e.printStackTrace()
                null
            } finally {
                connection?.disconnect()
            }
        }
    }
}
