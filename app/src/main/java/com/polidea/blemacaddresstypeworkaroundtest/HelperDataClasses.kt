package com.polidea.blemacaddresstypeworkaroundtest


enum class RaceResult {
    DeviceConnected, DeviceScanned, Timeout
}

data class TestResult(val connectionSuccess: Boolean, val raceResult: RaceResult)

data class MeasuredSingleResult<T>(val result: T, val executionTimeInMillis: Long)
