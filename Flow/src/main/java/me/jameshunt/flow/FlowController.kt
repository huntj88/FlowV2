package me.jameshunt.flow

import com.inmotionsoftware.promisekt.DeferredPromise
import com.inmotionsoftware.promisekt.Promise

abstract class FlowController<Input, Output> {

    interface State

    data class InitialState<Input>(val input: Input) : State

    private val resultPromise: DeferredPromise<Output> = DeferredPromise()

    internal val childFlows: MutableList<FlowController<*, *>> = mutableListOf()

    protected abstract fun onStart(state: InitialState<Input>)

    internal fun onDone(output: Output) {
        this.resultPromise.resolve(output)
    }

    // internal to this instance use
    internal open fun launchFlow(input: Input): Promise<Output> {
        this.onStart(InitialState(input))
        return this.resultPromise.promise
    }
}
