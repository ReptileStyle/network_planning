package com.example.networkplanning.optimization

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmanager.ui.core.UiEvent
import com.example.projectmanager.ui.data.GraphBuilder2
import com.example.projectmanager.ui.data.GraphCalculations
import com.example.networkplanning.optimization.component.model.MonteCarloWork
import com.example.projectmanager.ui.renameme.Work
import com.example.projectmanager.ui.util.new2.GraphCalculations2
import com.patrykandpatrick.vico.core.entry.entryModelOf
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class OptimizationViewModel : ViewModel() {
    var state by mutableStateOf(OptimizationState())
        private set

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun setDataset(workList: List<Work>) {
        state = state.copy(workList = workList)
    }


    fun onEvent(event: OptimizationEvent) {
        when (event) {
            is OptimizationEvent.OnEditWork -> {
                val workList = state.workList.toMutableList()
                workList[event.index] = event.newWork
                state = state.copy(workList = workList)
            }
            OptimizationEvent.OnOptimizeButtonClick -> {
                optimize()
            }
            is OptimizationEvent.OnBenefitChange -> {
                state = state.copy(benefitForOneDay = event.value)
            }
            OptimizationEvent.OnHidePlotButtonClick -> {
                state = state.copy(isPlotVisible = false)
            }
            OptimizationEvent.OnShowPlotButtonClick -> {
                state = state.copy(isPlotVisible = true)
            }
            is OptimizationEvent.OnSelectInvestmentVariant -> {
                val workList = state.workList.toMutableList()
                val map = state.plotInfoList[event.index].investmentMap
                workList.forEach {
                    val value = map[it.name] ?: 0
                    it.invested =
                        value.also { Log.d("viewModel", value.toString()) }
                }
                state = state.copy(
                    workList = workList,
                    isPlotVisible = false,
                    selectedVariant = event.index
                )
                state = state.copy(workCostsMonteCarlo = workList.map {
                    MonteCarloWork(
                        name=it.name,
                        it.durationPessimistic!! - it.invested/it.costToSpeedUp!!,
                        6.0
                    )
                })
            }
            OptimizationEvent.OnChooseMonteCarloMode -> {
                state = state.copy(isMonteCarlo = true)
            }
            OptimizationEvent.OnBuildMonteCarloPlot -> {
                viewModelScope.launch {
                    val graph = GraphBuilder2(state.workList)
                    val calculator =
                        GraphCalculations(myEdges = graph.myEdges, nodes = graph.myNodes)
                    val calc2 = GraphCalculations2(graphCalculations = calculator, graph)
                    val plotInfo = calc2.buildMonteCarloHist(state.workCostsMonteCarlo)
                    val model = entryModelOf(
                        *(plotInfo.toTypedArray())
                    )
                    state=state.copy(monteCarloPlotInfo = model)
                }
            }
            is OptimizationEvent.OnEditMonteCarlo -> {
                val monteCarloList = state.workCostsMonteCarlo.toMutableList()
                monteCarloList[event.index] = event.value
                state = state.copy(workCostsMonteCarlo = monteCarloList)
            }
        }
    }

    private fun optimize() {
        val benefit = state.benefitForOneDay
        if (benefit != null) {
            viewModelScope.launch {
                val graph = GraphBuilder2(state.workList)
                val calculator =
                    GraphCalculations(myEdges = graph.myEdges, nodes = graph.myNodes)
                val calc2 = GraphCalculations2(graphCalculations = calculator, graph)
                val plotList = calc2.getOptimizationGraphic2(benefit)
                val chartEntryModel = entryModelOf(
                    *(plotList.map { Pair(it.days, it.cost) }.toTypedArray())
                )
                plotList.forEach {
                    Log.d("viewModel", "${it.days} ${it.cost}")
                }
                state = state.copy(
                    plotInfoList = plotList,
                    plotModel = chartEntryModel,
                    isPlotVisible = true
                )
            }
        } else {
            viewModelScope.launch {
                _uiEvent.send(UiEvent.Message("Заполните поле с выгодой"))
            }
        }
    }
}