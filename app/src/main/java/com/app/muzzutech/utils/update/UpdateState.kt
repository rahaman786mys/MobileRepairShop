package com.app.muzzutech.utils.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global singleton to track update availability across the session
 */
object UpdateState {
    private val _availableUpdate = MutableStateFlow<VersionInfo?>(null)
    val availableUpdate: StateFlow<VersionInfo?> = _availableUpdate

    private val _isSnoozed = MutableStateFlow(false)
    val isSnoozed: StateFlow<Boolean> = _isSnoozed

    fun setAvailableUpdate(info: VersionInfo?) {
        _availableUpdate.value = info
    }

    fun snooze() {
        _isSnoozed.value = true
    }

    fun unsnooze() {
        _isSnoozed.value = false
    }
}
