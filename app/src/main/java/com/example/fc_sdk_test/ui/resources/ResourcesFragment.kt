package com.example.fc_sdk_test.ui.resources

import android.os.Handler
import android.os.Looper
import com.datadog.android.rum.RumErrorSource
import com.datadog.android.rum.RumResourceKind
import com.datadog.android.rum.RumResourceMethod
import com.example.fc_sdk_test.ui.RumDemoFragment

/**
 * Manual RUM resource tracking via [startResource] / [stopResource] / [stopResourceWithError].
 * This is distinct from the Network Trace screen, which relies on the okhttp auto-instrumentation.
 * Each button opens a resource, then closes it after a simulated latency.
 */
class ResourcesFragment : RumDemoFragment() {

    override val title: String = "Resources (手动追踪)"
    private val handler = Handler(Looper.getMainLooper())

    override fun buildScenarios() {
        section("成功资源 — 各 RumResourceKind")
        kindButton("IMAGE 资源", "https://example.com/avatar.png", RumResourceKind.IMAGE, 24_500)
        kindButton("JS 资源", "https://example.com/app.js", RumResourceKind.JS, 88_000)
        kindButton("CSS 资源", "https://example.com/style.css", RumResourceKind.CSS, 12_300)
        kindButton("FONT 资源", "https://example.com/font.woff2", RumResourceKind.FONT, 45_000)
        kindButton("FETCH 资源", "https://api.example.com/v1/users", RumResourceKind.FETCH, 3_200)
        kindButton("XHR 资源", "https://api.example.com/v1/orders", RumResourceKind.XHR, 5_600)
        kindButton("NATIVE 资源", "content://local/db/query", RumResourceKind.NATIVE, 1_024)

        section("不同 HTTP 方法")
        button("POST 资源 (201)") {
            val key = newKey()
            rum.startResource(key, RumResourceMethod.POST, "https://api.example.com/v1/orders")
            show("⏳ POST 资源开始…")
            handler.postDelayed({
                rum.stopResource(key, 201, 640, RumResourceKind.FETCH, mapOf("method" to "POST"))
                show("✓ stopResource(POST, 201)")
            }, 600)
        }

        section("失败 / 错误状态资源")
        button("失败资源 (stopResourceWithError 500)") {
            val key = newKey()
            rum.startResource(key, RumResourceMethod.GET, "https://api.example.com/v1/broken")
            show("⏳ 失败资源开始…")
            handler.postDelayed({
                rum.stopResourceWithError(
                    key,
                    500,
                    "Internal Server Error while loading resource",
                    RumErrorSource.NETWORK,
                    java.io.IOException("HTTP 500"),
                    mapOf("retryable" to false)
                )
                show("✓ stopResourceWithError(500, NETWORK)")
            }, 600)
        }
        button("404 资源 (stopResource 404)") {
            val key = newKey()
            rum.startResource(key, RumResourceMethod.GET, "https://example.com/missing.png")
            show("⏳ 404 资源开始…")
            handler.postDelayed({
                rum.stopResource(key, 404, 0, RumResourceKind.IMAGE)
                show("✓ stopResource(404)")
            }, 600)
        }
    }

    /** Adds a button that opens a GET resource and closes it as a success of the given [kind]. */
    private fun kindButton(label: String, url: String, kind: RumResourceKind, size: Long) {
        button(label) {
            val key = newKey()
            rum.startResource(key, RumResourceMethod.GET, url)
            show("⏳ ${kind.name} 资源开始…")
            handler.postDelayed({
                rum.stopResource(key, 200, size, kind, mapOf("url" to url))
                show("✓ stopResource(${kind.name}, 200, ${size}B)")
            }, 500)
        }
    }

    private fun newKey(): String = "res-${System.currentTimeMillis()}-${(0..9999).random()}"

    override fun onCleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}
