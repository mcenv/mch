package mch.profiling

class ProfilerResultsParser private constructor(private val text: String) {
    private var cursor: Int = 0

    private fun parseProfilerResults(): ProfilerResults {
        !"---- Minecraft Profiler Results ----\n"

        parseComment()
        !'\n'

        !"Version: "
        val version = readUntil('\n')
        skip()

        !"Time span: "
        val timeSpan = parseLong()
        !" ms\n"

        !"Tick span: "
        val tickSpan = parseLong()
        !" ticks\n"

        parseComment()
        !'\n'

        !"--- BEGIN PROFILE DUMP ---\n\n"
        val profilerResults = mutableMapOf<String, ProfilerResult>().also { profilerResults ->
            while (!startsWith("---")) {
                parseProfilerResult().let { profilerResults[it.name] = it }
                !'\n'
            }
        }
        !"--- END PROFILE DUMP ---\n\n"

        !"--- BEGIN COUNTER DUMP ---\n\n"
        val counterResults = mutableMapOf<String, CounterResult>().also { counterResults ->
            while (!startsWith("---")) {
                !"-- Counter: "
                val name = readUntil(' ')
                !" --\n"
                counterResults[name] = parseCounterResult()
                !"\n\n\n"
            }
        }
        !"--- END COUNTER DUMP ---\n\n"

        require(text.length == cursor)

        return ProfilerResults(version, timeSpan, tickSpan, profilerResults, counterResults)
    }

    private fun parseProfilerResult(): ProfilerResult {
        val indent = parseIndent() + 1
        return if (peek() == '#') { // TODO: parse from right
            skip()
            val name = readUntil(' ')
            skip()
            val totalCount = parseLong()
            !'/'
            val averageCount = readUntil('\n').toLong()
            ProfilerResult.Counter(name, totalCount, averageCount)
        } else {
            val name = readUntil('(')
            skip()
            val totalCount = parseLong()
            !'/'
            val averageCount = parseLong()
            !") - "
            val percentage = parsePercentage()
            !'/'
            val globalPercentage = parsePercentage()
            val children = mutableMapOf<String, ProfilerResult>().also { children ->
                while (peekIndent()?.let { it == indent } == true) {
                    !'\n'
                    parseProfilerResult().let { children[it.name] = it }
                }
            }
            ProfilerResult.Time(name, totalCount, averageCount, percentage, globalPercentage, children)
        }
    }

    private fun parseCounterResult(): CounterResult {
        val indent = parseIndent() + 1
        val parts = readUntil('\n').split(' ')
        val name = parts.dropLast(3).joinToString(" ")
        val (totalSelf, totalTotal) = parts[parts.size - 3].let { part ->
            require(part.startsWith("total:"))
            part.substring("total:".length).split('/').map { it.toLong() }
        }
        require("average:" == parts[parts.size - 2])
        val (averageSelf, averageTotal) = parts[parts.size - 1].split('/').map { it.toLong() }
        val children = mutableMapOf<String, CounterResult>().also { children ->
            while (peekIndent()?.let { it == indent } == true) {
                !'\n'
                parseCounterResult().let { children[it.name] = it }
            }
        }
        return CounterResult(name, totalSelf, totalTotal, averageSelf, averageTotal, children)
    }

    private fun parseLong(): Long {
        val start = cursor
        while (peek().isDigit()) {
            skip()
        }
        return text.substring(start, cursor).toLong()
    }

    private fun parsePercentage(): Double {
        val string = readUntil('%')
        skip()
        return string.toDouble()
    }

    private fun peekIndent(): Int? {
        val start = cursor
        skipLine()
        return if (cursor < text.length && peek() == '[') {
            text.substring(cursor + 1, cursor + 3).toInt()
        } else {
            null
        }.also {
            cursor = start
        }
    }

    private fun parseIndent(): Int {
        !'['
        val indent = text.substring(cursor, cursor + 2).toInt()
        skip(2)
        !"] "
        repeat(indent) {
            !"|   "
        }
        return indent
    }

    private fun parseComment(): String {
        !"// "
        val comment = readUntil('\n')
        skip()
        return comment
    }

    private fun readUntil(suffix: Char): String {
        val start = cursor
        skipUntil(suffix)
        return text.substring(start, cursor)
    }

    private operator fun Char.not() {
        require(peek() == this)
        skip()
    }

    private operator fun String.not() {
        require(this@ProfilerResultsParser.startsWith(this))
        skip(length)
    }

    private fun startsWith(prefix: String): Boolean = text.startsWith(prefix, cursor)

    private fun skipLine() {
        skipUntil('\n')
        skip()
    }

    private fun skipUntil(suffix: Char) {
        while (peek() != suffix) {
            skip()
        }
    }

    private fun skip(count: Int = 1) {
        cursor += count
    }

    private fun peek(): Char = text[cursor]

    companion object {
        operator fun invoke(text: String): ProfilerResults = ProfilerResultsParser(text).parseProfilerResults()
    }
}
