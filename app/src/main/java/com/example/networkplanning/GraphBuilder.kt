package com.example.networkplanning

import android.util.Log
import com.example.networkplanning.toStr
import dev.bandb.graphview.graph.Graph
import dev.bandb.graphview.graph.Node
import kotlin.math.round

data class MyEdge(var src:List<Work>, var dst:List<Work>, var value:Int, var work: Work?, var mode: Int=0){
    override fun toString(): String {
        return "src=${src.toStr()} dst=${dst.toStr()} value=${value} work=${work?.name}\n"
    }
    var _valuePessimistic:Int?=null
    var _valueOptimistic:Int?=null
    val valuePessimistic:Int
        get() = if (_valuePessimistic==null) value else _valuePessimistic!!
    val valueOptimistic:Int
        get() = if (_valueOptimistic==null) value else _valueOptimistic!!
    var name:String="NaN"

    val valueForGraph:Int
        get()=when(mode){
            1->value
            3-> round((valuePessimistic+valueOptimistic+4*value).toDouble()/6.0).toInt()
            2-> round((3*valuePessimistic+2*valueOptimistic).toDouble()/5.0).toInt()
            else->0
        }
}


class GraphBuilder2(workList: List<Work>, val mode:Int=1) {
    val nodes: MutableList<Node> = mutableListOf()
    val dataset = workList.toMutableList()
    val myEdges:MutableList<MyEdge> = mutableListOf()

    var vertices=dataset.map { it.requiredWorks }.toSet().toMutableList()

    lateinit var myNodes:List<Node>

    fun processWorks(){
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
        //sorting
        val sortedDataSet:MutableSet<Work> = mutableSetOf()
        while(sortedDataSet.size!=dataset.size){
            dataset.forEach { it->
                if(sortedDataSet.containsAll(it.requiredWorks) && !sortedDataSet.contains(it)) {
                    sortedDataSet.add(it)
                }
            }
        }
        dataset.removeAll { true }
        dataset.addAll(sortedDataSet)
        vertices=dataset.map { it.requiredWorks }.toSet().toMutableList()
    }






    val graph = Graph()
    init {
        Log.d("GB2","dataset size-"+dataset.size.toString())
        processWorks()
        Log.d("GB2","dataset size-"+dataset.size.toString())
        addInitialEvents()
        Log.d("GB2","nodes size-"+nodes.size.toString())
        processVertex()
        Log.d("GB2","nodes size-"+nodes.size.toString())
        addListedWorks()
        Log.d("GB2","dataset size-"+dataset.size.toString())
        addDummyWorks()
        checkForDoubleEdges()
        myNodes=createGraph(myEdges).toList()
    }





    private fun addInitialEvents(){
        for(i in 0 until vertices.size){
            nodes.add(Node("",works=vertices[i]))//добавили нужное количество вершин
        }
        nodes.add(Node("",works=dataset))//финальная вершина, все работы выполнены
    }

    private fun addListedWorks(){
        for(i in dataset.indices){
            myEdges.add(MyEdge(listOf(), listOf(),0,null))//добавили все ребра, но никуда не прикрепили
        }
        var k = 0
        var i = 0
        vertices.forEach{ works->
            val edges = dataset.filter { it.requiredWorks==works }.forEach {
                myEdges[i].src=works
                myEdges[i].value=it.duration ?: 0
                myEdges[i].work=it
                myEdges[i].name=it.name
                myEdges[i]._valueOptimistic=it.durationOptimistic
                myEdges[i]._valuePessimistic=it.durationPessimistic
                myEdges[i].mode=mode
                i++
            } //по src все норм, надо расставить dst
            k++
        }
        i=0
        for (myEdge in myEdges) {
            var minimalNode = nodes.filter { (it.works).contains(myEdge.work) }.minBy { it->
                (it.works).size
            }
            myEdge.dst=(minimalNode.works)
        }
    }

    //добаить фиктивные работы по принципу
    //проверяем все вершины на предмет того, что в них входит, если каждая из работ, то скип
    //если не все, то ищем нужные вершины и проводим линию с 0 duration
    private fun addDummyWorks(){
        for(node in nodes){
            Log.d("GB2dummy","${node.works.toStr()}")
            if(node.works.size==myEdges.filter { it.dst==node.works }.size){
                continue
            }
            val thisWorks=node.works.toMutableList()
//            Log.d("GB2works","this works")
//            Log.d("GB2works",thisWorks.toStr())
//            Log.d("GB2works",(myEdges.filter { it.dst==node.works }.map { it.work } as List<Work>).toStr())
            thisWorks.removeAll(myEdges.filter { it.dst==node.works }.map { it.work })//убрали все работы, которые уже входят сюда
            myEdges.filter { it.dst==node.works }.forEach { thisWorks.removeAll(it.src) }
//            Log.d("GB2works",thisWorks.toStr())
//            Log.d("GB2works","this works")
            while(true){
                if(thisWorks.isEmpty()) break
                val currentWork=thisWorks[0]
                if(currentWork.name=="dummyWork") break
                // Log.d("GB2works","current work="+currentWork.name)
                //ищем вершину, где максимальное количество работ, включающее currentWork, но меньше, чем у текущей вершины
                val maxNode = nodes.filter { it.works.contains(currentWork) && it.works.size<node.works.size }.maxBy { it.works.size }
                // Log.d("GB2works","maxNode="+maxNode.works.toStr())
                dataset.add(Work("dummyWork",0,requiredWorks= maxNode.works.toMutableList()))
                myEdges.add(MyEdge(maxNode.works,node.works,0, dataset.last()).apply { name="dummyWork" })
                // Log.d("GB22",nodes.filter { it.works.contains(currentWork) && it.works.size<node.works.size }.toString())
                thisWorks.remove(currentWork)
                thisWorks.removeAll(maxNode.works)
            }
        }
    }
    fun List<Work>.toStr():String{
        return this.joinToString ( ", " ){it.name}
    }
    fun MutableList<MyEdge>.toStr2():String{
        return this.joinToString ( ", " ){it.toString()}
    }
    private fun checkForDoubleEdges(){
        for(i in myEdges.indices){
            for(j in i+1 until myEdges.size){
                if(myEdges[i].dst==myEdges[j].dst && myEdges[i].src==myEdges[j].src){
                    nodes.add(Node("",works = myEdges[i].src.plus(myEdges[i].work!!).toList()))
                    myEdges.add(
                        MyEdge(myEdges[j].src,nodes.last().works,0,
                        Work("dummyWork",0, requiredWorks =  myEdges[j].src.toMutableList())
                        ).also { it.name=="dummyWork" })
                    myEdges[j].src=nodes.last().works
                    // Log.d("GB32","1111")
                }
            }
        }
    }

    private fun createGraph(edges: MutableList<MyEdge>):Set<Node>{
        var myNodes = setOf<Node>()
        edges.forEach { edge->
            val nodesrc=nodes.find { it.works == edge.src }!!
            val nodedst=nodes.find { it.works == edge.dst }!!//also filter out nodes
            myNodes=myNodes.plus(nodedst)
            myNodes=myNodes.plus(nodesrc)
            graph.addEdge(nodesrc,nodedst,edge.valueForGraph)
        }
        return myNodes
    }

    // превращаем все моменты типа (b4,b7),(b5,b7) в b7,(b4,b7),(b5,b7) для корректной обработки далее
    //
    private fun processVertex(){
        for(i in dataset.indices){
            for(j in i until dataset.size){//ищем пересечения множеств, если оно не равно 0 и его нет, то добавляем событие
                val intersect= dataset[i].requiredWorks.intersect(dataset[j].requiredWorks)
                if (intersect.isNotEmpty()){
                    if(dataset.filter { it.requiredWorks==intersect.toMutableList() }.isEmpty()){
                        nodes.add(Node("",works=intersect.toList()))
                    }
                }
            }
        }
    }
}