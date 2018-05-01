package benchmarks

import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.TimeUnit

@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
open class WorstCase {
    @Param("8", "16", "32", "64", "128", "256", "512", "1024")
    var bufferSize: Int = 0

    @Param("2", "3", "4", "8", "16", "32", "64")
    var nodeChildCount: Int = 0

    private var preparedThisLevel = BufferNode(null, null, 0)
    private var preparedNextLevel = BufferNode(null, null, 0)

    @Setup(Level.Trial)
    fun prepare() {
        val thisValue = "some element"
        var thisLevel = BufferNode(thisValue, null, 1)
        val nextValue = Array<Any?>(nodeChildCount, { thisValue })
        var nextLevel = BufferNode(nextValue, null, 1)
        repeat(this.bufferSize - 1) {
            thisLevel = BufferNode(thisValue, thisLevel, thisLevel.size + 1)
            nextLevel = BufferNode(nextValue, nextLevel, nextLevel.size + 1)
        }
        this.preparedThisLevel = thisLevel
        this.preparedNextLevel = nextLevel
    }

    @Benchmark
    fun addAndMaxBalance(bh: Blackhole) {
        if (nodeChildCount >= bufferSize) {
            throw IllegalStateException()
        }

        val thisBuffer = preparedThisLevel
        val nextBuffer = BufferNode("next element", null, 1)
        val moveCount = (thisBuffer.size - 1) / nodeChildCount * nodeChildCount
        val leaveCount = thisBuffer.size - moveCount
        val newNextBuffer = thisBuffer.pop(leaveCount).moveToNextLevelBuffer(nextBuffer, nodeChildCount)
        val newThisBuffer = thisBuffer.removeBottom(moveCount)
        bh.consume(newNextBuffer)
        bh.consume(newThisBuffer)
    }

    @Benchmark
    fun addAndMinBalance(bh: Blackhole) {
        if (nodeChildCount >= bufferSize) {
            throw IllegalStateException()
        }

        val thisBuffer = preparedThisLevel
        val nextBuffer = BufferNode("next element", null, 1)
        val leaveCount = thisBuffer.size - nodeChildCount
        val newNextBuffer = thisBuffer.pop(leaveCount).moveToNextLevelBuffer(nextBuffer, nodeChildCount)
        val newThisBuffer = thisBuffer.removeBottom(nodeChildCount)
        bh.consume(newNextBuffer)
        bh.consume(newThisBuffer)
    }

    @Benchmark
    fun removeAndMaxBalance(bh: Blackhole) {
        if (nodeChildCount >= bufferSize) {
            throw IllegalStateException()
        }

        val nextBuffer = preparedNextLevel
        val moveCount = bufferSize / nodeChildCount
        val newThisBuffer = nextBuffer.moveToUpperLevelBuffer(moveCount, nodeChildCount)
        val newNextBuffer = nextBuffer.pop(moveCount)
        bh.consume(newNextBuffer)
        bh.consume(newThisBuffer)
    }

    @Benchmark
    fun removeAndMinBalance(bh: Blackhole) {
        if (nodeChildCount >= bufferSize) {
            throw IllegalStateException()
        }

        val nextBuffer = preparedNextLevel
        val newThisBuffer = nextBuffer.moveToUpperLevelBuffer(1, nodeChildCount)
        val newNextBuffer = nextBuffer.pop(1)
        bh.consume(newNextBuffer)
        bh.consume(newThisBuffer)
    }
}

private class BufferNode(private val value: Any?, private val next: BufferNode?, val size: Int) {
    fun pop(count: Int): BufferNode {
        var result = this
        repeat(count) {
            result = result.next!!
        }
        return result
    }

    fun moveToNextLevelBuffer(nextBuffer: BufferNode, nodeChildCount: Int): BufferNode {
        val nextValue = arrayOfNulls<Any?>(nodeChildCount)
        var node: BufferNode? = this
        repeat(nodeChildCount) {
            nextValue[nodeChildCount - it - 1] = node!!.value
            node = node!!.next
        }
        val newNextBuffer = BufferNode(nextValue, nextBuffer, nextBuffer.size + 1)
        if (node == null) {
            return newNextBuffer
        }
        return node!!.moveToNextLevelBuffer(newNextBuffer, nodeChildCount)
    }

    fun moveToUpperLevelBuffer(count: Int, nodeChildCount: Int): BufferNode {
        val thisValue = this.value as Array<Any?>
        var upperBuffer = if (count == 1) null else this.next!!.moveToUpperLevelBuffer(count - 1, nodeChildCount)
        for (upperValue in thisValue) {
            upperBuffer = BufferNode(upperValue, upperBuffer, (upperBuffer?.size ?: 0) + 1)
        }
        return upperBuffer!!
    }

    fun removeBottom(count: Int): BufferNode? {
        if (count == this.size) {
            return null
        }
        val newNext = this.next!!.removeBottom(count)
        return BufferNode(this.value, newNext, this.size - count)
    }
}