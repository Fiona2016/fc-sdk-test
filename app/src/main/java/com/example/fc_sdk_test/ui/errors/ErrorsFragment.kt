package com.example.fc_sdk_test.ui.errors

import com.datadog.android.rum.RumErrorSource
import com.example.fc_sdk_test.ui.RumDemoFragment

/**
 * RUM error-reporting scenarios. All are NON-FATAL [RumMonitor.addError] / [addErrorWithStacktrace]
 * calls (fatal crashes stay in the Reflow screen): every [RumErrorSource], with and without a
 * throwable, an injected stacktrace, a deep call stack and a range of exception types.
 */
class ErrorsFragment : RumDemoFragment() {

    override val title: String = "Errors (非致命上报)"

    /** Custom exception used to exercise non-builtin throwable types. */
    private class BusinessException(message: String) : Exception(message)

    override fun buildScenarios() {
        section("addError — 基础")
        button("带 throwable (SOURCE)") {
            rum.addError(
                "Test error with throwable",
                RumErrorSource.SOURCE,
                RuntimeException("boom from ErrorsFragment"),
                mapOf("error_type" to "manual_with_throwable")
            )
            show("✓ addError(SOURCE, throwable)")
        }
        button("仅消息 (无 throwable)") {
            rum.addError("Test error message only", RumErrorSource.SOURCE, null)
            show("✓ addError(SOURCE, throwable=null)")
        }

        section("addError — 各 RumErrorSource")
        button("NETWORK source") {
            rum.addError("Simulated network error", RumErrorSource.NETWORK, java.io.IOException("timeout"))
            show("✓ addError(NETWORK)")
        }
        button("AGENT source") {
            rum.addError("Simulated agent error", RumErrorSource.AGENT, null)
            show("✓ addError(AGENT)")
        }
        button("CONSOLE source") {
            rum.addError("Simulated console error", RumErrorSource.CONSOLE, null)
            show("✓ addError(CONSOLE)")
        }
        button("LOGGER source") {
            rum.addError("Simulated logger error", RumErrorSource.LOGGER, null)
            show("✓ addError(LOGGER)")
        }
        button("WEBVIEW source") {
            rum.addError("Simulated webview error", RumErrorSource.WEBVIEW, null)
            show("✓ addError(WEBVIEW)")
        }

        section("addErrorWithStacktrace — 注入堆栈")
        button("自定义堆栈字符串") {
            val stack = """
                java.lang.IllegalStateException: injected stacktrace
                    at com.example.app.PaymentService.charge(PaymentService.kt:42)
                    at com.example.app.CheckoutViewModel.pay(CheckoutViewModel.kt:88)
                    at com.example.app.CheckoutFragment.onPayClick(CheckoutFragment.kt:120)
            """.trimIndent()
            rum.addErrorWithStacktrace(
                "Error with injected stacktrace",
                RumErrorSource.SOURCE,
                stack,
                mapOf("error_type" to "injected_stacktrace")
            )
            show("✓ addErrorWithStacktrace()")
        }

        section("深层调用栈")
        button("5 层嵌套异常") {
            try {
                level1()
            } catch (e: Exception) {
                rum.addError("Deep stack trace error", RumErrorSource.SOURCE, e)
                show("✓ addError(深 5 层堆栈)")
            }
        }

        section("不同异常类型")
        button("NullPointerException") { reportThrown { val s: String? = null; s!!.length } }
        button("IndexOutOfBoundsException") { reportThrown { listOf(1)[5] } }
        button("ArithmeticException") { reportThrown { @Suppress("DIVISION_BY_ZERO") val x = 1 / 0; x } }
        button("NumberFormatException") { reportThrown { "not-a-number".toInt() } }
        button("自定义 BusinessException") { reportThrown { throw BusinessException("订单状态非法") } }
    }

    /** Runs [block], catches whatever it throws and reports it as a non-fatal RUM error. */
    private fun reportThrown(block: () -> Any?) {
        try {
            block()
            show("(未抛出异常)")
        } catch (e: Throwable) {
            rum.addError(e.javaClass.simpleName, RumErrorSource.SOURCE, e)
            show("✓ addError(${e.javaClass.simpleName})")
        }
    }

    private fun level1(): Nothing = level2()
    private fun level2(): Nothing = level3()
    private fun level3(): Nothing = level4()
    private fun level4(): Nothing = level5()
    private fun level5(): Nothing = throw IllegalStateException("error from the 5th nested level")
}
