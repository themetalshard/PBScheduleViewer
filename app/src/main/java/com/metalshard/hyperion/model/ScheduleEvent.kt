package com.metalshard.hyperion.model

import com.google.gson.annotations.SerializedName

data class ScheduleEvent(
    @SerializedName("EventType") val eventType: String,
    @SerializedName("Time") val time: Long,
    @SerializedName("Duration") val duration: Int,
    @SerializedName("Trainer") val trainer: String?,
    @SerializedName("TrainerId") val trainerId: Long?,
    @SerializedName("TrainingID") val uuid: String?,
    @SerializedName("TrainerCommsId") val discordId: String?, // Updated key
    @SerializedName("Notes") val notes: String?,
    @SerializedName("EventColor") val eventColor: List<Int>?
)