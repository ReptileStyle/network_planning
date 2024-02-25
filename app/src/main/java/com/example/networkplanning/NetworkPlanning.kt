package com.example.networkplanning

import android.util.Log
import androidx.annotation.VisibleForTesting
import dev.bandb.graphview.graph.Node

interface INetworkPlanning {

}
//object NetworkPlanning: INetworkPlanning {
//    var myEdges: List<MyEdge> = listOf()
//    var nodes: List<Node> = listOf()
//    var mode: Int = 1
//    var optimization: Boolean = false
//
//    //считаем наиболее ранний срок наступления событий
//    private val nodeData: MutableList<NodeData> = mutableListOf()
//    private val edgeData: MutableList<EdgeData> = mutableListOf()
//    private val criticalPaths: MutableList<MutableList<Int>> =
//        mutableListOf(mutableListOf())//может быть не один критический путь
//
//    fun addEdge(src: List<Work>, dst: List<Work>) {
//
//    }
//
//
//    fun calculateGraph(): GraphData {
//        nodeDataInit()
//        if (optimization) {
//            calculateEarlyTimes(1)
//        } else {
//            calculateEarlyTimes()
//            calculateLateTimes()
//            for (i in edgeData.indices) {
//                Log.d(
//                    "GCedge",
//                    "${edgeData[i].valueOptimistic}-${edgeData[i].valueAverage}-${edgeData[i].valuePessimistic}=${edgeData[i].value}"
//                )
//            }
//        }
//    }
//
//    fun nodeDataInit() {
//        Log.d("GC", "nodes size = " + nodes.size.toString())
//        for (node in nodes) {
//            if (myEdges.filter { it.dst == node.works || it.src == node.works }.isNotEmpty())
//                nodeData.add(NodeData(node, mutableListOf(), mutableListOf()))
//        }
//        migrateEdges()
//        //nodeData.sortBy { it -> it.node.works.size }
//        enumerateNodes()
//        nodeData.sortBy { it.number }
//    }
//}

object GraphBuilder {
    @VisibleForTesting
    var dataset: MutableList<Work> = mutableListOf()

    val nodes: MutableList<Node> = mutableListOf()
    val myEdges:MutableList<MyEdge> = mutableListOf()
    val mode = 1 // todo: rewrite
    fun addWork(work: Work) {
        dataset.add(work)
    }

    fun replaceWorkList(works: List<Work>) {
        dataset = works.toMutableList()
    }

    fun createGraph(): Set<Node> {
        Log.d("GB2", "dataset size-" + dataset.size.toString())
        sortUnsortedDataset()
        Log.d("GB2", "nodes size-" + nodes.size.toString())
        processVertex()
        Log.d("GB2", "nodes size-" + nodes.size.toString())
        addListedWorks()
        Log.d("GB2", "dataset size-" + dataset.size.toString())
        addDummyWorks()
        checkForDoubleEdges()


        var myNodes = setOf<Node>()
        myEdges.forEach { edge ->
            val nodesrc = nodes.find { it.works == edge.src }!!
            val nodedst = nodes.find { it.works == edge.dst }!! //also filter out nodes
            myNodes = myNodes.plus(nodedst)
            myNodes = myNodes.plus(nodesrc)
//                graph.addEdge(nodesrc, nodedst, edge.valueForGraph)
        }
        return myNodes
    }

    fun sortUnsortedDataset() {
        // вычисляет абсолютный список требуемых работ для каждой работы
        dataset.forEach {
                work->
            val workSet:MutableSet<Work> = mutableSetOf()
            work.requiredWorks.forEach { secondWork->
                workSet.addAll(secondWork.requiredWorks)
            }
            workSet.addAll(work.requiredWorks)
            work.requiredWorks.removeAll { true }
            work.requiredWorks.addAll(workSet)
        }
        // сортируем работы так, что если все работы списка перед работой А выполнены, то А можно начинать
        val sortedDataSet:MutableSet<Work> = mutableSetOf()
        while(sortedDataSet.size != dataset.size) {
            dataset.forEach { it->
                if (sortedDataSet.containsAll(it.requiredWorks) && !sortedDataSet.contains(it)) {
                    sortedDataSet.add(it)
                }
            }
        }
        dataset = sortedDataSet.toMutableList()
        initVertices(dataset)
    }

    private fun initVertices(works: List<Work>) {
        val vertices = works.map { it.requiredWorks }.toSet().toMutableList()
        for (i in 0 until vertices.size) {
            nodes.add(Node("", works = vertices[i]))//добавили нужное количество вершин
        }
        nodes.add(Node("", works = dataset))//финальная вершина, все работы выполнены
    }

    // превращаем все моменты типа (b4,b7),(b5,b7) в b7,(b4,b7),(b5,b7) для корректной обработки далее
    @VisibleForTesting
    fun processVertex() {
        for (i in dataset.indices) {
            for (j in i until dataset.size) {//ищем пересечения множеств, если оно не равно 0 и его нет, то добавляем событие
                val intersect = dataset[i].requiredWorks.intersect(dataset[j].requiredWorks.toSet())
                if (intersect.isNotEmpty()) {
                    if (dataset.none { it.requiredWorks == intersect.toMutableList() }) {
                        nodes.add(Node("", works = intersect.toList()))
                    }
                }
            }
        }
    }

    fun addListedWorks() {
        for (i in dataset.indices) {
            myEdges.add(
                MyEdge(
                    listOf(),
                    listOf(),
                    0,
                    null
                )
            )//добавили все ребра, но никуда не прикрепили
        }
        var k = 0
        var i = 0

        nodes.map { it.works }.dropLast(1).forEach { works -> // mark: добавил сюда дропласт
            val edges = dataset.filter { it.requiredWorks == works }.forEach {
                myEdges[i].src = works
                myEdges[i].value = it.duration ?: 0
                myEdges[i].work = it
                myEdges[i].name = it.name
                myEdges[i]._valueOptimistic = it.durationOptimistic
                myEdges[i]._valuePessimistic = it.durationPessimistic
                myEdges[i].mode = mode
                i++
            } // по src все норм, надо расставить dst
            k++
        }
        i = 0
        for (myEdge in myEdges) {
            val minimalNode = nodes.filter { (it.works).contains(myEdge.work) }.minBy { it ->
                (it.works).size
            }
            myEdge.dst = (minimalNode.works)
        }
    }

    //добаить фиктивные работы по принципу
    //проверяем все вершины на предмет того, что в них входит, если каждая из работ, то скип
    //если не все, то ищем нужные вершины и проводим линию с 0 duration
    fun addDummyWorks() {
        for (node in nodes) {
            if (node.works.size == myEdges.filter { it.dst == node.works }.size) {
                continue
            }
            val thisWorks = node.works.toMutableList()
            thisWorks.removeAll(myEdges.filter { it.dst == node.works }
                .map { it.work }.toSet())//убрали все работы, которые уже входят сюда
            myEdges.filter { it.dst == node.works }.forEach { thisWorks.removeAll(it.src) }
            while (true) {
                if (thisWorks.isEmpty()) break
                val currentWork = thisWorks[0]
                if (currentWork.name == "dummyWork") break

                //ищем вершину, где максимальное количество работ, включающее currentWork, но меньше, чем у текущей вершины
                val maxNode =
                    nodes.filter { it.works.contains(currentWork) && it.works.size < node.works.size }
                        .maxBy { it.works.size }

                dataset.add(Work("dummyWork", 0, requiredWorks = maxNode.works.toMutableList()))
                myEdges.add(MyEdge(maxNode.works, node.works, 0, dataset.last()).apply {
                    name = "dummyWork"
                })

                thisWorks.remove(currentWork)
                thisWorks.removeAll(maxNode.works)
            }
        }
    }

    private fun checkForDoubleEdges() {
        for (i in myEdges.indices) {
            for (j in i + 1 until myEdges.size) {
                if (myEdges[i].dst == myEdges[j].dst && myEdges[i].src == myEdges[j].src) {
                    nodes.add(Node("", works = myEdges[i].src.plus(myEdges[i].work!!).toList()))
                    myEdges.add(
                        MyEdge(
                            myEdges[j].src, nodes.last().works, 0,
                            Work("dummyWork", 0, requiredWorks = myEdges[j].src.toMutableList())
                        ))
                    myEdges[j].src = nodes.last().works
                    // Log.d("GB32","1111")
                }
            }
        }
    }
}

data class GraphData(
    val nodeData: MutableList<NodeData> = mutableListOf(),
    val edgeData: MutableList<EdgeData> = mutableListOf(),
    val criticalPaths: MutableList<MutableList<Int>> = mutableListOf(mutableListOf())
)
