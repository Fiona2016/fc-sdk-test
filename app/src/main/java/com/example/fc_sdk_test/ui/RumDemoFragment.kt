package com.example.fc_sdk_test.ui

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.datadog.android.rum.GlobalRumMonitor
import com.datadog.android.rum.RumMonitor
import com.example.fc_sdk_test.databinding.FragmentRumDemoBinding

/**
 * Base class for the RUM scenario demo screens. Each subclass declares a [title] and builds a list
 * of labelled buttons in [buildScenarios]; every button triggers one RUM API call and reports the
 * outcome via [show] (page result area + toast + logcat). All screens share [fragment_rum_demo.xml].
 */
abstract class RumDemoFragment : Fragment() {

    private var _binding: FragmentRumDemoBinding? = null
    protected val binding get() = _binding!!

    /** Shortcut to the global RUM monitor used by every scenario. */
    protected val rum: RumMonitor get() = GlobalRumMonitor.get()

    /** Title shown at the top of the screen. */
    protected abstract val title: String

    /** Build the buttons for this screen via [section] / [button]. */
    protected abstract fun buildScenarios()

    /** Tag used for logcat output; overridable per screen. */
    protected open val logTag: String get() = "RumDemo:${javaClass.simpleName}"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRumDemoBinding.inflate(inflater, container, false)
        binding.demoHeader.text = title
        buildScenarios()
        return binding.root
    }

    /** Adds a bold section header to the button list. */
    protected fun section(label: String) {
        val tv = TextView(requireContext()).apply {
            text = label
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(20), 0, dp(4))
        }
        binding.demoContainer.addView(tv)
    }

    /** Adds a full-width button that runs [onClick] when tapped. */
    protected fun button(label: String, onClick: () -> Unit) {
        val btn = Button(requireContext()).apply {
            text = label
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                try {
                    onClick()
                } catch (e: Exception) {
                    show("✗ 执行失败: ${e.message}")
                    Log.e(logTag, "scenario '$label' failed", e)
                }
            }
        }
        binding.demoContainer.addView(btn)
    }

    /** Reports an outcome to the result area, a toast and logcat. */
    protected fun show(message: String) {
        _binding?.demoResult?.text = message
        Log.d(logTag, message)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** Hook for subclasses to stop background work; called before the binding is released. */
    protected open fun onCleanup() {}

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        onCleanup()
        super.onDestroyView()
        _binding = null
    }
}
