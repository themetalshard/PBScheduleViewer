package com.metalshard.hyperion.ui

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metalshard.hyperion.data.ScheduleRepository
import com.metalshard.hyperion.model.ScheduleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScheduleViewModel : ViewModel() {
    private val repository = ScheduleRepository()
    private val _schedule = MutableStateFlow<Map<String, List<ScheduleEvent>>>(emptyMap())
    val schedule: StateFlow<Map<String, List<ScheduleEvent>>> = _schedule

    val isCalendarView = mutableStateOf(true)
    val isLoading = mutableStateOf(true)
    val activeGroup = mutableStateOf("PBST")
    val selectedEvent = mutableStateOf<ScheduleEvent?>(null)

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            isLoading.value = true
            _schedule.value = repository.fetchSchedule()
            isLoading.value = false
        }
    }

    fun getActiveDates(): List<LocalDate> {
        val currentGroupEvents = _schedule.value[activeGroup.value] ?: return emptyList()
        return currentGroupEvents
            .map { Instant.ofEpochSecond(it.time).atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
    }

    fun getEventsForDate(date: LocalDate): List<ScheduleEvent> {
        val currentGroupEvents = _schedule.value[activeGroup.value] ?: return emptyList()
        return currentGroupEvents.filter {
            Instant.ofEpochSecond(it.time).atZone(ZoneId.systemDefault()).toLocalDate() == date
        }.sortedBy { it.time }
    }
}