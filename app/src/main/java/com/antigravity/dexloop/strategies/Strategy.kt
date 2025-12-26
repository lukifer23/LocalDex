package com.antigravity.dexloop.strategies

import kotlinx.coroutines.flow.StateFlow

interface Strategy {
    val name: String
    val description: String
    val isRunning: StateFlow<Boolean>

    suspend fun start(): Result<Unit>
    suspend fun stop()
}
