package com.example.networkplanning

import org.junit.Test

import org.junit.Assert.*
import org.junit.Before

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class GraphBuilderTest {
    @Before
    fun setUp() {
        GraphBuilder.replaceWorkList(exampleWorkList)
    }
    @Test
    fun sortUnsortedDatasetTest() {
        GraphBuilder.sortUnsortedDataset()

        assertEquals("[[], [b1], [b3], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6, b4, b7], [b1, b3, b2, b5, b6, b10], [b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11]]", GraphBuilder.nodes.map { it.works.map { it.name } }.toString())

        assertEquals("[b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11]", GraphBuilder.dataset.map { it.name }.toString())

        assertEquals("[[], [], [], [b1], [b1], [b3], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6, b4, b7], [b3], [b1, b3, b2, b5, b6, b10]]",GraphBuilder.dataset.map { it.requiredWorks.map { it.name }}.toString())
    }

    @Test
    fun processVertexTest() {
        GraphBuilder.replaceWorkList(exampleWorkList2)
        GraphBuilder.sortUnsortedDataset()
        GraphBuilder.processVertex()

        assertEquals("[[], [b1], [b3], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6, b7], [b1, b3, b2, b5, b6, b4, b7], [b1, b3, b2, b5, b6, b10], [b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11]]", GraphBuilder.nodes.map { it.works.map { it.name } }.toString())

        GraphBuilder.replaceWorkList(exampleWorkList)
        GraphBuilder.sortUnsortedDataset()
        GraphBuilder.processVertex()

        assertEquals("[[], [b1], [b3], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6, b4, b7], [b1, b3, b2, b5, b6, b10], [b1, b2, b3, b4, b5, b6, b7, b8, b9, b10, b11]]", GraphBuilder.nodes.map { it.works.map { it.name } }.toString())
    }

    @Test
    fun dummyWorksTest() {
        GraphBuilder.replaceWorkList(exampleWorkList)
        GraphBuilder.sortUnsortedDataset()
        GraphBuilder.processVertex()
        GraphBuilder.addListedWorks()
        GraphBuilder.addDummyWorks()

        assertEquals("[[], [], [], [b1], [b1], [b3], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6], [b1, b3, b2, b5, b6, b4, b7], [b3], [b1, b3, b2, b5, b6, b10], [b1, b3, b2, b5, b6]]", GraphBuilder.dataset.map { it.requiredWorks.map { it.name }}.toString())
    }
}