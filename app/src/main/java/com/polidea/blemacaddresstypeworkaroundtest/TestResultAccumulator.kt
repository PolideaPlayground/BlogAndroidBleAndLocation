package com.polidea.blemacaddresstypeworkaroundtest


class TestResultAccumulator {

    private var totalTests = 0
    private val successesTimes = ArrayList<Long>(1000)

    fun add(measuredSingleResult: MeasuredSingleResult<TestResult>): TestResultAccumulator {
        val (result, executionTimeInMillis) = measuredSingleResult
        val (connectionSuccess, raceResult) = result

        totalTests += 1
        if (connectionSuccess) successesTimes.add(executionTimeInMillis)
        // TODO: if interested other metrics may be computed

        return this
    }

    override fun toString(): String {
        val averageValue = if (successesTimes.size > 0)
            "%.2f s".format(successesTimes.average() * 0.001)
        else
            "No successes yet"
        return "Total tests count: $totalTests.\n" +
                "Successful tests count: ${successesTimes.size}.\n" +
                "Average successful test time: $averageValue"
    }
}
