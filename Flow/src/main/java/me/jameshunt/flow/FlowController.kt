package me.jameshunt.flow

import me.jameshunt.flow.promise.DeferredPromise
import me.jameshunt.flow.promise.Promise
import me.jameshunt.flow.promise.always

typealias ViewId = Int

abstract class FlowController<Input, Output> {

    interface State

    data class InitialState<Input>(val arg: Input) : State

    private lateinit var currentState: State

    private val resultPromise: DeferredPromise<FlowResult<Output>> =
        DeferredPromise()

    val childFlows: MutableList<FlowController<*, *>> = mutableListOf()

    fun resume() = resume(currentState)

    protected abstract fun resume(currentState: State)

    protected abstract fun onStart(state: InitialState<Input>)

    protected open fun onBack() {
        this.resultPromise.resolve(FlowResult.Back)
    }

    protected open fun onDone(arg: Output) {
        this.resultPromise.resolve(FlowResult.Completed(arg))
    }

    protected fun <T : State> State.transition(to: T, transition: (T) -> Unit) {
        if (currentState != this) throw IllegalStateException("already transitioned, current state is $currentState, from: $this, to: $to")

        currentState = to
        transition(to)
    }

    // internal use
    fun launchFlow(arg: Input): Promise<FlowResult<Output>> {
        currentState = InitialState(arg)
        this.onStart(currentState as InitialState<Input>)
        return this.resultPromise.promise
    }

    protected fun <NewInput, NewOutput> flow(
        controller: Class<FragmentFlowController<NewInput, NewOutput>>,
        viewId: ViewId,
        arg: NewInput
    ): Promise<FlowResult<NewOutput>> {
        val flowController = controller
            .getDeclaredConstructor(ViewId::class.java)
            .newInstance(viewId)

        childFlows.add(flowController)

        return flowController.launchFlow(arg).always {
            childFlows.remove(flowController)
        }
    }

    abstract fun handleBack()
}