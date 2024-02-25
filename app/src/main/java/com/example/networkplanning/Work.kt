package com.example.networkplanning


data class Work(
    val name: String, val duration: Int? = null, val requiredWorks: MutableList<Work>,
    var durationPessimistic:Int?=null,
    var durationOptimistic:Int?=null,
    val costToSpeedUp: Int? = null,
    var invested:Int = 0
) {
//    val requiredWorks=requiredWorks.toMutableList()

    var requiredWorksForTable: List<Work>


//    constructor(
//        name: String,
//        durationOptimistic: Int,
//        durationAverage: Int,
//        durationPessimistic: Int,
//        requiredWorks: List<Work>
//    ) : this(name, durationAverage, requiredWorks) {
//        _durationPessimistic = durationPessimistic
//        _durationOptimistic = durationOptimistic
//    }
//
//    constructor(
//        name: String,
//        durationOptimistic: Int,
//        durationPessimistic: Int,
//        requiredWorks: List<Work>
//    ) : this(name, durationOptimistic, requiredWorks) {
//        _durationPessimistic = durationPessimistic
//        _durationOptimistic = durationOptimistic
//    }


    init {
        requiredWorksForTable = requiredWorks.toList()
    }
}

fun List<Work>.toStr(): String {
    return this.joinToString(", ") { it.name }
}