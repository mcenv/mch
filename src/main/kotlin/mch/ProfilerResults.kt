package mch

data class ProfilerResults(
    val version: String,
    val timeSpan: Long,
    val tickSpan: Long,
    val profilerResults: Map<String, ProfilerResult>,
    val counterResults: Map<String, CounterResult>,
)

sealed class ProfilerResult {
    abstract val name: String

    data class Counter(
        override val name: String,
        val totalCount: Long,
        val averageCount: Long,
    ) : ProfilerResult()

    data class Time(
        override val name: String,
        val totalCount: Long,
        val averageCount: Long,
        val percentage: Double,
        val globalPercentage: Double,
        val children: Map<String, ProfilerResult>,
    ) : ProfilerResult()
}

data class CounterResult(
    val name: String,
    val totalSelf: Long,
    val totalTotal: Long,
    val averageSelf: Long,
    val averageTotal: Long,
    val children: Map<String, CounterResult>,
)
