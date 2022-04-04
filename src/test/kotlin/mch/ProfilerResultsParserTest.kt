package mch

import kotlin.test.Test

class ProfilerResultsParserTest {
    @Test
    fun `parsing does not fail`() {
        val profilerResults = ProfilerResultsParser(ProfilerResultsParserTest::class.java.getResource("/profiling.txt")!!.readText())
        println(profilerResults)
    }
}
