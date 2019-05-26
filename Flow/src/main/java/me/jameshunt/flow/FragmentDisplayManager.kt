package me.jameshunt.flow

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import me.jameshunt.flow.util.logFlow

internal class FragmentDisplayManager(private val fragmentManager: FragmentManager) {

    fun <FragInput, FragOutput, FragmentType : FlowUI<FragInput, FragOutput>> show(
        fragmentProxy: FragmentProxy<FragInput, FragOutput, FragmentType>,
        viewId: ViewId
    ): FragmentType {
        // save state of fragment that was already in the view
        (fragmentManager.findFragmentById(viewId) as? FlowFragment<*, *>)?.proxy?.saveState()

        val fragment = fragmentProxy.fragment?.get() ?: fragmentProxy.clazz.newInstance()

        fragmentProxy.bind(fragment)

        return when (fragment) {
            is DialogFragment -> showDialog(fragment, fragmentProxy.tag)
            is Fragment -> showFragment(fragment, viewId, fragmentProxy.tag)
            else -> throw NotImplementedError()
        }
    }

    private fun <FragInput, FragOutput, FragmentType : FlowUI<FragInput, FragOutput>> showFragment(
        fragment: FragmentType,
        viewId: ViewId,
        tag: String
    ): FragmentType {
        fragmentManager
            .beginTransaction()
            .setCustomAnimations(R.anim.abc_grow_fade_in_from_bottom, R.anim.abc_shrink_fade_out_from_bottom)
            .replace(viewId, fragment as Fragment, tag)
            .commit()

        return fragment
    }

    private fun <FragInput, FragOutput, FragmentType : FlowUI<FragInput, FragOutput>> showDialog(
        dialogFragment: FragmentType,
        tag: String
    ): FragmentType {
        (dialogFragment as DialogFragment).show(fragmentManager, tag)
        return dialogFragment
    }

    fun saveAll() {
        fragmentManager.fragments.forEach {
            (it as FlowUI<*, *>).proxy!!.saveState()
        }
    }

    fun remove(activeFragment: FragmentProxy<*, *, *>?) {
        activeFragment?.fragment?.get()
            ?.let { fragmentManager.beginTransaction().remove(it as Fragment).commit() }
            ?: logFlow("no active fragment")
    }

    fun removeAll(blocking: Boolean = false) {
        fragmentManager.beginTransaction()
            .also { transaction ->
                fragmentManager.fragments.forEach {
                    transaction.remove(it)
                }
            }
            .let {
                when (blocking) {
                    true -> it.commitNow()
                    false -> it.commit()
                }
            }
    }

    fun getVisibleFragmentBehindDialog(viewId: ViewId): FragmentProxy<*, *, *>? {
        // needed because when a dialog fragment is active, we need to know what the fragment behind the dialog is
        return (fragmentManager.findFragmentById(viewId) as? FlowFragment<*, *>)?.proxy
    }
}