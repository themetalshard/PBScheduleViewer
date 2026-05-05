package com.metalshard.hyperion

import com.metalshard.hyperion.model.ScheduleEvent
import kotlin.text.contains
import kotlin.text.equals
import kotlin.text.lowercase

data class ScheduleAction(val label: String, val actionType: String, val xxEvent: String)

object EventScheduler {
    fun getSupportedActions(eventType: String, activeGroup: String): List<ScheduleAction> {
        val type = eventType.lowercase()

        if (type.contains("response") || type.contains("patrol raid") || type.contains("patrol support")) {
            return emptyList()
        }

        return when {
            type == "joint patrol" || type.contains("joint patrol") -> listOf(
                ScheduleAction("Schedule PR for TMS", "jointpr", "jointpr")
            )
            type.contains("patrol") -> {
                if (type.contains("pet") || activeGroup.equals("PET", ignoreCase = true)) {
                    listOf(
                        ScheduleAction("Schedule support for PBST", "support", "support"),
                        ScheduleAction("Schedule PR for TMS", "petpr", "petpr")
                    )
                } else if (type.contains("pbst") || activeGroup.equals("PBST", ignoreCase = true)) {
                    listOf(
                        ScheduleAction("Schedule support for PET", "support", "support"),
                        ScheduleAction("Schedule PR for TMS", "pbstpr", "pbstpr")
                    )
                } else emptyList()
            }
            type.contains("raid") -> listOf(
                ScheduleAction("Schedule response for PBST/PET", "rr", "rr")
            )
            else -> emptyList()
        }
    }

    fun generateCommand(
        event: ScheduleEvent,
        activeGroup: String,
        eventType: String,
        actionType: String,
        notes: String
    ): String {
        val xxEvent = when {
            eventType.equals("Joint Patrol", ignoreCase = true) -> "jointpr"
            eventType.contains("Patrol", ignoreCase = true) -> {
                when {
                    eventType.contains("PET", ignoreCase = true) || activeGroup.equals("PET", ignoreCase = true) -> {
                        if (actionType == "petpr") "petpr" else "support"
                    }
                    eventType.contains("PBST", ignoreCase = true) || activeGroup.equals("PBST", ignoreCase = true) -> {
                        if (actionType == "pbstpr") "pbstpr" else "support"
                    }
                    else -> "support"
                }
            }
            eventType.contains("Raid", ignoreCase = true) -> "rr"
            else -> ""
        }
        return "/eventschedule timestamp unix:${event.time} length:${event.duration} event:$xxEvent notes:$notes"
    }
}