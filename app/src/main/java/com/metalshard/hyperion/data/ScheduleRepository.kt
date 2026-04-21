package com.metalshard.hyperion.data

import com.metalshard.hyperion.model.ScheduleEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class ScheduleRepository {
    private val url = "https://pbsv.themetalshard.space/api/"
    private val gson = Gson()

    suspend fun fetchSchedule(): Map<String, List<ScheduleEvent>> = withContext(Dispatchers.IO) {
        try {
            val jsonText = URL(url).readText()

            val listType = object : TypeToken<List<Map<String, Any>>>() {}.type
            val rawList: List<Map<String, Any>> = gson.fromJson(jsonText, listType)

            val finalMap = mutableMapOf<String, List<ScheduleEvent>>()

            rawList.forEach { groupMap ->
                val groupId = groupMap["id"] as? String ?: "unknown"

                val eventsInGroup = mutableListOf<ScheduleEvent>()

                groupMap.forEach { (key, value) ->
                    if (key != "id") {
                        try {
                            val eventJson = gson.toJson(value)
                            val event = gson.fromJson(eventJson, ScheduleEvent::class.java)
                            eventsInGroup.add(event)
                        } catch (e: Exception) {
                        }
                    }
                }
                finalMap[groupId] = eventsInGroup.sortedBy { it.time }
            }
            finalMap
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }
}