package benchmarks

import org.openjdk.jmh.results.RunResult
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder
import org.openjdk.jmh.runner.options.TimeValue
import java.io.FileWriter

fun main(args: Array<String>) {
    val implementation = "persistentDequeWorstCase"
    val outputFile = "teamcityArtifacts/$implementation.csv"
    val options = OptionsBuilder()
            .forks(2)
            .jvmArgs("-Xms3072m", "-Xmx3072m")
            .warmupIterations(10)
            .measurementIterations(10)
            .warmupTime(TimeValue.milliseconds(500))
            .measurementTime(TimeValue.milliseconds(500))
            .addProfiler("gc")

    val runResults = Runner(options.build()).run()
    printResults(runResults, implementation, outputFile)
}

fun printResults(runResults: Collection<RunResult>, implementation: String, outputFile: String) {
    val csvHeader = "Implementation,Method,bufferSize,nodeChildCount,Score,Score Error,Allocation Rate"

    val fileWriter = FileWriter(outputFile)

    fileWriter.appendln(csvHeader)

    runResults.forEach {
        fileWriter.appendln(csvRowFrom(it, implementation))
    }

    fileWriter.flush()
    fileWriter.close()
}

fun csvRowFrom(result: RunResult, implementation: String): String {
    val nanosInMicros = 1000
    val method = result.primaryResult.getLabel()
    val bufferSize = result.params.getParam("bufferSize").toInt()
    val nodeChildCount = result.params.getParam("nodeChildCount").toInt()
    val score = result.primaryResult.getScore() * nanosInMicros
    val scoreError = result.primaryResult.getScoreError() * nanosInMicros
    val allocationRate = result.secondaryResults["·gc.alloc.rate.norm"]!!.getScore()

    return "$implementation,$method,$bufferSize,$nodeChildCount,%.3f,%.3f,%.3f"
            .format(score, scoreError, allocationRate)
}
