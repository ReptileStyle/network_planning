package com.example.networkplanning

import android.util.Log
import com.example.networkplanning.optimization.component.model.MonteCarloWork

import org.apache.commons.math3.distribution.BetaDistribution
import org.jgrapht.alg.shortestpath.AllDirectedPaths
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleDirectedWeightedGraph
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val TAG = "calcs2"

class GraphCalculations2(
    var graphCalculations: GraphCalculations,
    val graphBuilder: GraphBuilder2
) {
    val criticalPath = mutableListOf<List<Int>>()
    fun optimizeByOneDay(): Pair<Int, List<Work>> {//returns cost to optimize for 1 day and list of works to speed up
        val edgesNoReservedTime =
            graphCalculations.edgeData.filter { it.reservedTimeOptimization == 0 }.toMutableList()
        Log.d(TAG, edgesNoReservedTime.joinToString { it.name })
        val finishId = edgesNoReservedTime.maxBy { it.dstId }.dstId
        fun getMinimumCostToSpeedUp(vertexId: Int): Pair<Int, List<Work>> {
            Log.d("calcs2", "enter $vertexId")
            val edgesWithDstInVertexId = edgesNoReservedTime.filter { it.dstId == vertexId }
            edgesNoReservedTime.removeAll { it.dstId == vertexId }
            if (edgesWithDstInVertexId.isEmpty()) {
                return Pair(Int.MAX_VALUE, listOf()) //cant speed up start event
            } else {
                var sum = 0
                val works: MutableList<Work> = mutableListOf()
                edgesWithDstInVertexId.forEach {
                    val minCost = getMinimumCostToSpeedUp(it.srcId)
                    if (it.name == "b3") {
                        Log.d(
                            "calcs2",
                            "${it.work.durationPessimistic!!},${it.invested},${it.work.costToSpeedUp!!},${it.work.durationPessimistic!! - it.work.invested / it.work.costToSpeedUp!!}, ${it.work.durationOptimistic}"
                        )
                    }
                    if (it.work.durationPessimistic!! - it.invested / it.work.costToSpeedUp!! != it.work.durationOptimistic && it.speedUpCost < minCost.first) {
                        sum += it.speedUpCost
                        works.add(it.work)
                    } else {
                        sum += minCost.first
                        works.addAll(minCost.second)
                    }

                }

                return Pair(sum, works)
            }
        }

        val result = getMinimumCostToSpeedUp(finishId)
        Log.d("calcs2", "result ${result.first},${result.second.joinToString { it.name }}")
        return Pair(result.first, result.second)
    }


    fun firstOptimization(benefitOneDay: Int): Map<String, Int> {//returns investment map
        val map: MutableMap<String, Int> = mutableMapOf()
        while (true) {
            graphCalculations.recalculateReservedTimes(1)
            val result = optimizeByOneDay()
            if (result.first > benefitOneDay) break
            result.second.forEach {
                if (map.containsKey(it.name)) {
                    map[it.name] = map[it.name]!! + it.costToSpeedUp!!
                } else {
                    map[it.name] = it.costToSpeedUp!!
                }
            }
            graphCalculations.edgeData.forEach {
                it.invested = map[it.work.name] ?: 0
            }
//            graphCalculations.recalculateReservedTimes(1)
            Log.d("optimization", map.toString())
//            graphCalculations.calculateEarlyTimes(1)
        }
        return map
    }


    fun GraphCalculations.getEarlyTimeOfLastEvent(): Int {
        return this.nodeData.maxBy { it.earlyTime ?: 0 }.earlyTime!!
    }


    fun GraphCalculations.getCriticalPathsGraph(): SimpleDirectedWeightedGraph<Int, DefaultWeightedEdge> {
        this.recalculateReservedTimes(1)
        val graph =
            SimpleDirectedWeightedGraph<Int, DefaultWeightedEdge>(DefaultWeightedEdge::class.java)
        this.nodeData.forEach {
            graph.addVertex(it.number)
        }
        this.edgeData.filter { it.reservedTimeOptimization == 0 }
            .forEach { edge ->
                val weightedEdge = DefaultWeightedEdge()
                graph.addEdge(edge.src.number, edge.dst.number, weightedEdge)
                Log.d(
                    "newgraphcalc",
                    "${edge.work.name},${edge.invested},${edge.work.durationPessimistic!! - edge.invested / edge.work.costToSpeedUp!! != edge.work.durationOptimistic} "
                )
                graph.setEdgeWeight(
                    weightedEdge,
                    if (edge.work.durationPessimistic!! - edge.invested / edge.work.costToSpeedUp != edge.work.durationOptimistic)
                        edge.speedUpCost.toDouble() else Int.MAX_VALUE.toDouble(),
                )
            }
        return graph
    }

    fun getOptimizationGraphic2(benefitOneDay: Int): List<PlotInfo> {
        val list: MutableList<PlotInfo> = mutableListOf()
        val map = mutableMapOf<String, Int>() //save optimization config
        var criticalGraph = graphCalculations.getCriticalPathsGraph()
        val startId = criticalGraph.vertexSet().minBy { it }
        val finishId = criticalGraph.vertexSet().maxBy { it }
        Log.d("newgraphcalc", criticalGraph.toString())
        var allPaths =
            AllDirectedPaths(criticalGraph).getAllPaths(
                startId,
                finishId,
                true,
                Int.MAX_VALUE
            )
        val edgesToCheck: MutableSet<DefaultWeightedEdge> = mutableSetOf()
        allPaths.forEach {
            edgesToCheck.addAll(it.edgeList)
        }//create default set of edges to check
        fun getMinimalCostEdgeList(localEdgesToCheck: Set<DefaultWeightedEdge>): List<DefaultWeightedEdge> {
            val edgeList: MutableList<List<DefaultWeightedEdge>> = mutableListOf()
            for (edge in localEdgesToCheck) {
                val remainingEdgesToCheck = localEdgesToCheck.toMutableSet()
                for (path in allPaths) {
                    if (remainingEdgesToCheck.isEmpty()) break
                    if (path.edgeList.contains(edge)) {
                        remainingEdgesToCheck.removeAll(path.edgeList.toSet())
                    }
                }
                edgeList.add(
                    getMinimalCostEdgeList(remainingEdgesToCheck).plus(
                        edge
                    )
                )
            }
            return edgeList.minByOrNull {
                var sum = 0
                for (edge in it) {
                    if (criticalGraph.getEdgeWeight(edge).toInt() == Int.MAX_VALUE) {
                        sum = Int.MAX_VALUE
                        break
                    }
                    sum += criticalGraph.getEdgeWeight(edge).toInt()
                }
                sum
            } ?: listOf()
        }
        while (true) {
            val result = getMinimalCostEdgeList(edgesToCheck)
            if (result.sumOf { criticalGraph.getEdgeWeight(it) } >= Int.MAX_VALUE || result.isEmpty()) break
            //apply changes to graph
            result.forEach {
                val src = criticalGraph.getEdgeSource(it)
                val dst = criticalGraph.getEdgeTarget(it)
                val name =
                    graphCalculations.edgeData.find { it.src.number == src && it.dst.number == dst }!!.work.name
                if (map.containsKey(name)) {
                    map[name] = map[name]!! + criticalGraph.getEdgeWeight(it).toInt()
                } else {
                    map[name] = criticalGraph.getEdgeWeight(it).toInt()
                }
            }
            graphCalculations.edgeData.forEach {
                it.invested = map[it.work.name] ?: 0
            }
            graphCalculations.recalculateReservedTimes(1)
            list.add(
                PlotInfo(
                    days = graphCalculations.getEarlyTimeOfLastEvent(),
                    cost = graphCalculations.getEarlyTimeOfLastEvent() * benefitOneDay + map.map { it.value }
                        .sum(),
                    investmentMap = map.toMap()
                )
            )
            Log.d("newgraphcalc", map.toString())
            criticalGraph = graphCalculations.getCriticalPathsGraph()
            allPaths =
                AllDirectedPaths(criticalGraph).getAllPaths(
                    startId,
                    finishId,
                    true,
                    Int.MAX_VALUE
                )
            edgesToCheck.clear()
            criticalGraph.edgeSet().forEach {
                if (criticalGraph.getEdgeWeight(it).toInt() < Int.MAX_VALUE)
                    edgesToCheck.add(it)
            }
        }
        val minDays = list.minBy { it.days }
        val minCostMinDays = list.filter { it.days == minDays.days }.minBy { it.cost }
        list.removeAll { it.days == minDays.days && it.cost != minCostMinDays.cost }
        return list
    }

    private val betaAlpha = 3 - sqrt(2.0)
    private val betaBeta = 3 + sqrt(2.0)
    fun GraphCalculations.getEarlyTimeMonteCarloOfLastEvent(): Double {
        return this.nodeData.maxBy { it.earlyTimeMonteCarlo ?: 0.0 }.earlyTimeMonteCarlo!!
    }

    fun buildMonteCarloHist(monteCarloInfo: List<MonteCarloWork>): List<Pair<Int, Double>> {
        //generating sample
        val projectDurations = mutableListOf<Double>()

        for (i in 1..10000) {
            val workDurations: MutableMap<String, Double> = mutableMapOf()
            monteCarloInfo.forEach {
                workDurations[it.name] = it.duration.toDouble() + it.width!! * (BetaDistribution(
                    betaAlpha,
                    betaBeta
                ).sample().also { Log.d("asd", it.toString()) } - 0.3592455) //0.85355339
            } //вычесть моду распределения
            graphCalculations.nodeData.forEach {
                it.earlyTimeMonteCarlo = null
            }
            graphCalculations.edgeData.forEach { edge ->
                edge.monteCarloDuration = workDurations[edge.name] ?: 0.0
            }
            graphCalculations.calculateMonteCarloEarlyTime()
            projectDurations.add(
                graphCalculations.getEarlyTimeMonteCarloOfLastEvent()
                    .also { Log.d("generator", it.toString()) })
        }
        val rounded = projectDurations.map {
            it.roundToInt()
        }
        val countMap = mutableMapOf<Int, Int>()
        rounded.forEach {
            countMap[it] = (countMap[it] ?: 0) + 1
        }
        Log.d("calc2", rounded.toString())
        return countMap.toList().map { Pair(it.first, it.second.toDouble() / 10000.0) }
    }


    init {
    }
}


data class PlotInfo(
    val days: Int = 0,
    val cost: Int = 0,
    val investmentMap: Map<String, Int> = mapOf()
)