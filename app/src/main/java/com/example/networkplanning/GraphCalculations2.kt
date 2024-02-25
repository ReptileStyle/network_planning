package com.example.networkplanning

import android.util.Log
import dev.bandb.graphview.graph.Node
import org.apache.commons.math3.distribution.NormalDistribution
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

data class NodeData(
    val node: Node,
    val dstEdges: MutableList<EdgeData>,
    val srcEdges: MutableList<EdgeData>,
    var earlyTime: Int? = null,
    var lateTime: Int? = null,
) {
    override fun toString(): String {
        return "[node=$node,\ndstEdges=${dstEdges.joinToString(", ") { it.toString() }},\n" +
                "srcEdges=${srcEdges.joinToString(", ") { it.toString() }},\n" +
                "earlyTime=$earlyTime]"
    }

    val reservedTime: Int
        get() = lateTime!! - earlyTime!!
    var number: Int = -1
    var earlyTimeMonteCarlo: Double? = null
}

data class EdgeData(
    val valueAverage: Int,
    val valuePessimistic: Int,
    val valueOptimistic: Int,
    val dst: NodeData,
    val src: NodeData,
    val mode: Int = 0,//0-обычный режим, 1-двухпараметрический, 2-трехпараметрический
    val speedUpCost: Int,
    val work: Work
) {
    override fun toString(): String {
        return "$name=(${src.number},${dst.number})"
    }

    val reservedTime: Int
        get() = dst.lateTime!! - src.earlyTime!! - value
    val reservedTimeIndependent: Int
        get() = dst.earlyTime!! - src.lateTime!! - value
    val value: Int
        get() = when (mode) {
            1 -> valueAverage
            2 -> round((valuePessimistic * 3 + valueOptimistic * 2).toDouble() / 5).toInt()
            3 -> round((valueAverage * 4 + valuePessimistic + valueOptimistic).toDouble() / 6).toInt()
            else -> throw java.lang.Exception("wrong mode")
        }
    val dispersion: Double
        get() = when (mode) {
            1 -> 0.0
            2, 3 -> ((valueOptimistic - valuePessimistic).toDouble() / 6).pow(2)
            else -> throw java.lang.Exception("wrong mode")
        }

    val earlyTime: Int
        get() = src.earlyTime!!
    val lateTime: Int
        get() = dst.lateTime!!
    var name: String = ""
    val srcId: Int
        get() = src.number
    val dstId: Int
        get() = dst.number
    val valueOptimization: Int
        get() {
//        Log.d("work","${this.name},${this.valuePessimistic},${this.invested},${invested/(work.costToSpeedUp?:Int.MAX_VALUE)}")
            return valuePessimistic - invested / (work.costToSpeedUp ?: Int.MAX_VALUE)
        }
    val reservedTimeOptimization: Int
        get() = dst.lateTime!! - src.earlyTime!! - valueOptimization
    var invested = 0
    var monteCarloDuration = 0.0

}

data class ArcInfo(
    val criticalLength: Int,
    val arcLength: Int
) {
    val reservedTime: Int
        get() = criticalLength - arcLength
    val tension: Double
        get() = arcLength.toDouble() / criticalLength
}

class GraphCalculations(
    val myEdges: List<MyEdge>,
    val nodes: List<Node>,
    val mode: Int = 1,
    val optimization: Boolean = false
) {

    //считаем наиболее ранний срок наступления событий
    val nodeData: MutableList<NodeData> = mutableListOf()
    val edgeData: MutableList<EdgeData> = mutableListOf()
    val criticalPaths: MutableList<MutableList<Int>> =
        mutableListOf(mutableListOf())//может быть не один критический путь

    fun calculateEarlyTimes(mode: Int = 0) {
        if (mode == 0) {
            for (node in nodeData) {
                if (node.dstEdges.isEmpty()) {
                    node.earlyTime = 0
                    continue
                } //начальное событие
                try {
                    node.earlyTime = node.dstEdges.map { it.src.earlyTime!! + it.value }.max()
                } catch (e: java.lang.Exception) {
                    continue //удивительный по своей дибильности, но рабочий алгоритм
                }
            }
            if (nodeData.map { it.earlyTime }.contains(null)) calculateEarlyTimes()
        } else {
            for (node in nodeData) {
                if (node.dstEdges.isEmpty()) {
                    node.earlyTime = 0
                    continue
                } //начальное событие
                try {
                    Log.d("critical", node.number.toString())
                    Log.d("critical", node.dstEdges.joinToString { it.name })
                    Log.d(
                        "critical",
                        node.dstEdges.joinToString { it.monteCarloDuration.toString() })

                    node.earlyTime =
                        node.dstEdges.map { it.src.earlyTime!! + it.valueOptimization }.max()
                } catch (e: java.lang.Exception) {
                    continue //удивительный по своей дибильности, но рабочий алгоритм
                }
            }
            if (nodeData.map { it.earlyTime }.contains(null)) calculateEarlyTimes(mode)
        }
        Log.d("earlyTimes", nodeData.joinToString(",") { it.earlyTime.toString() })
    }

    fun calculateMonteCarloEarlyTime() {

        for (node in nodeData) {
            if (node.dstEdges.isEmpty()) {
                node.earlyTimeMonteCarlo = 0.0
                continue
            } //начальное событие
            try {
                node.earlyTimeMonteCarlo =
                    node.dstEdges.map { it.src.earlyTimeMonteCarlo!! + it.monteCarloDuration!! }
                        .max()
            } catch (e: java.lang.Exception) {
                continue //удивительный по своей дибильности, но рабочий алгоритм
            }
        }
        if (nodeData.map { it.earlyTimeMonteCarlo }.contains(null)) calculateMonteCarloEarlyTime()
    }

    fun calculateLateTimes(mode: Int = 0) {
        if (mode == 0) {
            for (node in nodeData) {
                if (node.srcEdges.isEmpty()) {
                    node.lateTime = nodeData.map { it.earlyTime!! }.max()
                    continue
                } //начальное событие
                try {
                    node.lateTime = node.srcEdges.map { it.dst.lateTime!! - it.value }.min()
                } catch (e: java.lang.Exception) {
                    continue //удивительный по своей дибильности, но рабочий алгоритм
                }
            }
            if (nodeData.map { it.lateTime }.contains(null)) calculateLateTimes()
        } else {
            for (node in nodeData) {
                if (node.srcEdges.isEmpty()) {
                    node.lateTime = nodeData.map { it.earlyTime!! }.max()
                    continue
                } //начальное событие
                try {
                    node.lateTime =
                        node.srcEdges.map { it.dst.lateTime!! - it.valueOptimization }.min()
                    Log.d(
                        "lateTimes",
                        node.srcEdges.joinToString { "${it.name}-${it.dst.lateTime}-${it.valueOptimization}" })
                } catch (e: java.lang.Exception) {
                    continue //удивительный по своей дибильности, но рабочий алгоритм
                }
            }
            if (nodeData.map { it.lateTime }.contains(null)) calculateLateTimes(mode)
        }
        Log.d("lateTimes", nodeData.joinToString(",") { it.lateTime.toString() })
    }

    fun recalculateReservedTimes(mode: Int = 0) {
        nodeData.map {
            it.earlyTime = null
            it.lateTime = null
        }
        calculateEarlyTimes(mode)
        calculateLateTimes(mode)

//        migrateEdges()
    }

    fun getCriticalPaths(node: NodeData = nodeData.last()): List<Int> {//в первый вызов кидаем последний элемент массива nodeData
        if (node == nodeData.last()) {
            criticalPaths.clear()
            criticalPaths.add(mutableListOf())
            criticalPaths[0].add(nodeData.last().number)
        }
        node.dstEdges.forEach {
            if (it.src.earlyTime!! + it.value == node.earlyTime!!) {
                criticalPaths[0].add(it.src.number!!)
                getCriticalPaths(it.src)
            }
        }
        return criticalPaths[0].reversed()
    }//искать путь не обязательно, достаточно проверить, что первый и последний нод имеют запас времени 0!!!

    fun getArcInfo(nums: List<Int>): ArcInfo {
        //должна проверить, есть ли такая дуга
        if (nums[0] < 0) throw java.lang.Exception("invalid argument")
        if (nums[0] >= nodeData.size) throw java.lang.Exception("invalid argument")
        for (i in 0..nums.size - 2) {
            if (edgeData.find { it.src == nodeData[nums[i]] && it.dst == nodeData[nums[i + 1]] } == null) throw java.lang.Exception(
                "invalid argument"
            )
        }
        if (!(nodeData[nums[0]].reservedTime == 0 && nodeData[nums.last()].reservedTime == 0)) throw java.lang.Exception(
            "invalid argument"
        )

        val criticalLength = nodeData[nums.last()].earlyTime!! - nodeData[nums[0]].earlyTime!!
        var arcLength = 0
        for (i in 0..nums.size - 2) {
            arcLength += edgeData.find { it.src == nodeData[nums[i]] && it.dst == nodeData[nums[i + 1]] }!!.value
        }

        return ArcInfo(criticalLength, arcLength)
    }

    fun getCriticalPathDispersion(): Double {
        val criticalPath = nodeData.filter { it.reservedTime == 0 }.sortedBy { it.number }
        var dispersion = 0.0
        for (i in 0..criticalPath.size - 2) {
            dispersion += edgeData.find { it.src == criticalPath[i] && it.dst == criticalPath[i + 1] }!!.dispersion
        }
        return dispersion
    }//не учтено то, что может быть несколько критических путей

    fun probabilityUnderTimeOf(time: Int): Double {
        if (getCriticalPathDispersion() != 0.0)
            return 0.5 + laplaceFunction(
                (time - nodeData.last().earlyTime!!).toDouble() / sqrt(
                    getCriticalPathDispersion()
                )
            )
        else
            return if (time < nodeData.last().earlyTime!!) 0.0 else 1.0
    }

    fun timeFromProbability(prob: Double): Int {
        if (prob == 1.0) {
            when (mode) {
                1 -> return nodeData.last().earlyTime!!
                else -> throw ArithmeticException("Infinity")
            }
        }
        Log.d("GCprob", NormalDistribution().inverseCumulativeProbability(prob).toString())
        return round(
            nodeData.last().earlyTime!! + NormalDistribution().inverseCumulativeProbability(
                prob
            ) * sqrt(getCriticalPathDispersion())
        ).toInt()
    }

    fun getInterval(prob: Double): Pair<Int, Int> {
        val x = round(
            nodeData.last().earlyTime!! - NormalDistribution().inverseCumulativeProbability(prob) * sqrt(
                getCriticalPathDispersion()
            )
        ).toInt()
        val y = round(
            nodeData.last().earlyTime!! + NormalDistribution().inverseCumulativeProbability(prob) * sqrt(
                getCriticalPathDispersion()
            )
        ).toInt()
        return Pair(x, y)
    }

    fun laplaceFunction(x: Double): Double {//не уверен, что так
        return NormalDistribution().cumulativeProbability(x) - 0.5
    }

    init {
        if (optimization) {
            nodeDataInit()
            calculateEarlyTimes(1)
        } else {
            nodeDataInit()
            calculateEarlyTimes()
            calculateLateTimes()
            for (i in edgeData.indices) {
                Log.d(
                    "GCedge",
                    "${edgeData[i].valueOptimistic}-${edgeData[i].valueAverage}-${edgeData[i].valuePessimistic}=${edgeData[i].value}"
                )
            }
        }

    }

    fun test() {
        //GraphBuilder2().createGraph()

        // Log.d("GCarc",getArcInfo(0,3,5).toString())
        Log.d("GC", nodeData.joinToString("\n") { it.toString() })
        Log.d("GC", "early time :" + nodeData.map { it.earlyTime }.toString())
        Log.d("GC", "late time :" + nodeData.map { it.lateTime }.toString())
        Log.d("GClaplace", laplaceFunction(0.0).toString())
        Log.d("GClaplace", laplaceFunction(0.1).toString())
        Log.d("GClaplace", laplaceFunction(0.2).toString())
        Log.d("GClaplace", laplaceFunction(0.05).toString())
        Log.d("GClaplace", laplaceFunction(0.15).toString())

        Log.d("GCnormal", NormalDistribution().cumulativeProbability(0.002).toString())
        Log.d("GCnormal", NormalDistribution().cumulativeProbability(0.002).toString())
        Log.d("GCnormal", NormalDistribution().cumulativeProbability(0.002).toString())
        Log.d("GCnormal", NormalDistribution().cumulativeProbability(0.002).toString())
    }


    //utility functions

    fun enumerateNodes(num: Int = 0) {
        val nodeToEnumerate = nodeData.find {
            it.number == -1 &&
                    it.dstEdges.map { it.src.number }.find { it == -1 } == null
        } ?: return//находит начальное событие
        nodeToEnumerate.number = num
        nodeToEnumerate.node.data = num

        enumerateNodes(num + 1)
    }


    fun nodeDataInit() {
        Log.d("GC", "nodes size = " + nodes.size.toString())
        for (node in nodes) {
            if (myEdges.filter { it.dst == node.works || it.src == node.works }.isNotEmpty())
                nodeData.add(NodeData(node, mutableListOf(), mutableListOf()))
        }
        migrateEdges()
        //nodeData.sortBy { it -> it.node.works.size }
        enumerateNodes()
        nodeData.sortBy { it.number }
    }

    fun migrateEdges() {
        edgeData.clear()
        for (myEdge in myEdges) {
            val nodeSrc = nodeData.find { it.node == nodes.find { it.works == myEdge.src }!! }!!
            val nodeDst = nodeData.find { it.node == nodes.find { it.works == myEdge.dst }!! }!!
            edgeData.add(
                EdgeData(
                    myEdge.value,
                    myEdge.valuePessimistic,
                    myEdge.valueOptimistic,
                    nodeDst,
                    nodeSrc,
                    mode,
                    myEdge?.work?.costToSpeedUp ?: Int.MAX_VALUE,
                    myEdge.work!! //dont know about null
                ).also { it ->
                    it.name = myEdge.name
                    nodeSrc.srcEdges.add(it)
                    nodeDst.dstEdges.add(it)
                }
            )
        }
    }
}