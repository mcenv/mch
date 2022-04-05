package mch

class ProfilerResultsParser private constructor(text: String) {
    private val lines: List<String> = text.lines()
    private var line: Int = 0
    private var character: Int = 0

    private fun parseProfilerResults(): ProfilerResults {
        !"---- Minecraft Profiler Results ----"
        skipLine(3)

        !"Version: "
        val version = remaining()
        skipLine()

        !"Time span: "
        val timeSpan = parseLong()
        !" ms"
        skipLine()

        !"Tick span: "
        val tickSpan = parseLong()
        !" ticks"
        skipLine(3)

        !"--- BEGIN PROFILE DUMP ---"
        skipLine(2)
        val profilerResults = mutableMapOf<String, ProfilerResult>().also { profilerResults ->
            while (!currentLine().startsWith("---")) {
                parseProfilerResult().let { profilerResults[it.name] = it }
                skipLine()
            }
        }
        !"--- END PROFILE DUMP ---"
        skipLine(2)

        !"--- BEGIN COUNTER DUMP ---"
        skipLine(2)
        val counterResults = mutableMapOf<String, CounterResult>().also { counterResults ->
            while (!currentLine().startsWith("---")) {
                !"-- Counter: "
                val name = readUntil(' ')
                !" --"
                skipLine()
                counterResults[name] = parseCounterResult()
                skipLine()
            }
        }
        !"--- END COUNTER DUMP ---"
        skipLine(3)

        require(lines.size == line)

        return ProfilerResults(version, timeSpan, tickSpan, profilerResults, counterResults)
    }

    private fun parseProfilerResult(): ProfilerResult {
        val indent = parseIndent() + 1
        return if (peek() == '#') { // TODO: parse from right
            !'#'
            val name = readUntil(' ')
            skip()
            val totalCount = parseLong()
            !'/'
            val averageCount = remaining().toLong()
            ProfilerResult.Counter(name, totalCount, averageCount)
        } else {
            val name = readUntil('(')
            skip()
            val totalCount = parseLong()
            !'/'
            val averageCount = parseLong()
            !')'
            !' '
            !'-'
            !' '
            val percentage = parsePercentage()
            !'/'
            val globalPercentage = parsePercentage()
            val children = mutableMapOf<String, ProfilerResult>().also { children ->
                while (peekIndent()?.let { it == indent } == true) {
                    skipLine()
                    parseProfilerResult().let { children[it.name] = it }
                }
            }
            ProfilerResult.Time(name, totalCount, averageCount, percentage, globalPercentage, children)
        }
    }

    private fun parseCounterResult(): CounterResult {
        val indent = parseIndent() + 1
        val parts = remaining().split(' ')
        val name = parts.dropLast(3).joinToString(" ")
        val (totalSelf, totalTotal) = parts[parts.size - 3].let { part ->
            require(part.startsWith("total:"))
            part.substring("total:".length).split('/').map { it.toLong() }
        }
        require("average:" == parts[parts.size - 2])
        val (averageSelf, averageTotal) = parts[parts.size - 1].split('/').map { it.toLong() }
        val children = mutableMapOf<String, CounterResult>().also { children ->
            while (peekIndent()?.let { it == indent } == true) {
                skipLine()
                parseCounterResult().let { children[it.name] = it }
            }
        }
        return CounterResult(name, totalSelf, totalTotal, averageSelf, averageTotal, children)
    }

    private fun parseLong(): Long {
        val start = character
        while (peek().isDigit()) {
            skip()
        }
        return currentLine().substring(start, character).toLong()
    }

    private fun parsePercentage(): Double {
        val string = readUntil('%')
        skip()
        return string.toDouble()
    }

    private fun peekIndent(): Int? {
        skipLine()
        return if (currentLine().length <= character) {
            null
        } else if (peek() == '[') {
            currentLine().substring(1, 3).toInt()
        } else {
            null
        }.also {
            --line
            character = 0
        }
    }

    private fun parseIndent(): Int {
        !'['
        val indent = currentLine().substring(1, 3).toInt()
        skip(2)
        !']'
        !' '
        repeat(indent) { !"|   " }
        return indent
    }

    private fun readUntil(suffix: Char): String {
        val start = character
        while (peek() != suffix) {
            skip()
        }
        return currentLine().substring(start, character)
    }

    private operator fun Char.not() {
        require(peek() == this)
        skip()
    }

    private operator fun String.not() {
        require(remaining().startsWith(this))
        skip(length)
    }

    private fun peek(): Char = currentLine()[character]

    private fun remaining(): String = currentLine().substring(character)

    private fun currentLine(): String = lines[line]

    private fun skip(count: Int = 1) {
        character += count
    }

    private fun skipLine(count: Int = 1) {
        line += count
        character = 0
    }

    companion object {
        operator fun invoke(text: String): ProfilerResults = ProfilerResultsParser(text).parseProfilerResults()
    }
}
