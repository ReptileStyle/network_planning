package com.example.networkplanning

//val work1 = Work("b1",3, durationOptimistic =  5, durationPessimistic =  8, requiredWorks =  mutableListOf(), costToSpeedUp = 6)
//val work2 = Work("b2",4, durationOptimistic =  9, durationPessimistic =  10, requiredWorks =   mutableListOf(), costToSpeedUp = 8)
//val work3 = Work("b3",1, durationOptimistic =  2, durationPessimistic =  6, requiredWorks =  mutableListOf(), costToSpeedUp = 4)
//val work4 = Work("b4",1, durationOptimistic =  7, durationPessimistic =  9, requiredWorks =  mutableListOf(work1), costToSpeedUp = 6)
//val work5 = Work("b5",1, durationOptimistic =  4, durationPessimistic =  5, requiredWorks =  mutableListOf(work1), costToSpeedUp = 3)
//val work6 = Work("b6",1, durationOptimistic =  1, durationPessimistic =  2, requiredWorks =  mutableListOf(work3), costToSpeedUp = 2)
//val work7 = Work("b7",1, durationOptimistic =  2, durationPessimistic =  4, requiredWorks =  mutableListOf(work2,work5,work6), costToSpeedUp = 3)
//val work8 = Work("b8",4, durationOptimistic =  5, durationPessimistic =  13, requiredWorks =   mutableListOf(work2,work5,work6), costToSpeedUp = 9)
//val work9 = Work("b9",1, durationOptimistic =  2, durationPessimistic =  8, requiredWorks =  mutableListOf(work4,work7), costToSpeedUp = 5)
//val work10 = Work("b10",6, durationOptimistic =  8, durationPessimistic =  17, requiredWorks =   mutableListOf(work3), costToSpeedUp = 10)
//val work11 = Work("b11",2, durationOptimistic =  8, durationPessimistic =  10, requiredWorks =   mutableListOf(work2,work5,work6,work10), costToSpeedUp = 7)


val work1 = Work("b1",3, durationOptimistic =  3, durationPessimistic =  8, requiredWorks =  mutableListOf(), costToSpeedUp = 6)
val work2 = Work("b2",4, durationOptimistic =  4, durationPessimistic =  10, requiredWorks =   mutableListOf(), costToSpeedUp = 8)
val work3 = Work("b3",1, durationOptimistic =  1, durationPessimistic =  6, requiredWorks =  mutableListOf(), costToSpeedUp = 4)
val work4 = Work("b4",1, durationOptimistic =  1, durationPessimistic =  9, requiredWorks =  mutableListOf(work1), costToSpeedUp = 6)
val work5 = Work("b5",1, durationOptimistic =  1, durationPessimistic =  5, requiredWorks =  mutableListOf(work1), costToSpeedUp = 3)
val work6 = Work("b6",1, durationOptimistic =  1, durationPessimistic =  2, requiredWorks =  mutableListOf(work3), costToSpeedUp = 2)
val work7 = Work("b7",1, durationOptimistic =  1, durationPessimistic =  4, requiredWorks =  mutableListOf(work2,work5,work6), costToSpeedUp = 3)
val work8 = Work("b8",4, durationOptimistic =  4, durationPessimistic =  13, requiredWorks =   mutableListOf(work2,work5,work6), costToSpeedUp = 9)
val work9 = Work("b9",1, durationOptimistic =  1, durationPessimistic =  8, requiredWorks =  mutableListOf(work4,work7), costToSpeedUp = 5)
val work10 = Work("b10",6, durationOptimistic =  6, durationPessimistic =  17, requiredWorks =   mutableListOf(work3), costToSpeedUp = 10)
val work11 = Work("b11",2, durationOptimistic =  2, durationPessimistic =  10, requiredWorks =   mutableListOf(work2,work5,work6,work10), costToSpeedUp = 7)

//val work12 = Work("b12",7, listOf(work9,work7,work6,work10))
//val work13 = Work("b13",7, listOf(work6,work10))
//val work14 = Work("b14",7, listOf(work6,work10))
//val work15 = Work("b15",7, listOf(work14))
//val work16 = Work("b16",7, listOf(work15))
//val work17 = Work("b17",7, listOf(work14,work15))
//val work18 = Work("b18",7, listOf(work13))
//val work19 = Work("b19",7, listOf(work13,work16))
//val work20 = Work("b20",7, listOf(work19))
//val work21 = Work("b21",7, listOf(work20))
//val work22 = Work("b22",7, listOf(work21))

val exampleWorkList:MutableList<Work> = mutableListOf(
    work1,
    work2,
    work3,
    work4,
    work5,
    work6,
    work7,
    work8,
    work9,
    work10,
    work11,
)

val exampleWorkList2:MutableList<Work> = mutableListOf(
    work1,
    work2,
    work3,
    work4,
    work5,
    work6,
    work7,
    work8.copy(requiredWorks = mutableListOf(work2,work5,work7)),
    work9,
    work10,
    work11,
)