package dev.bandb.graphview.graph

import com.example.networkplanning.Work
import dev.bandb.graphview.util.Size
import dev.bandb.graphview.util.VectorF

data class Node(var data: Any, var completedWorks: Set<Int> = setOf(), var works: List<Work> = listOf(), var moveX:Float=0f, var moveY:Float=0f) {
    // TODO make private
    val position: VectorF = VectorF()
    val size: Size = Size()

    var height: Int
        get() = size.height
        set(value) {
            size.height = value
        }

    var width: Int
        get() = size.width
        set(value) {
            size.width = value
        }

    var x: Float
        get() = position.x
        set(value) {
            position.x = value+moveX/2
        }

    var y: Float
        get() = position.y
        set(value) {
            position.y = value+moveY/2
        }

    fun setSize(width: Int, height: Int) {
        this.width = width
        this.height = height
    }

    fun setPosition(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    fun setPosition(position: VectorF) {
        this.x = position.x
        this.y = position.y
    }
    fun addCompletedWork(edge: Triple<Int,Int,Int>){
        completedWorks.plus(edge.third)
    }
}
