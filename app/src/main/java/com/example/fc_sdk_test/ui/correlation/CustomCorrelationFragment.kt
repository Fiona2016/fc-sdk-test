package com.example.fc_sdk_test.ui.correlation

import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.datadog.android.rum.RumActionType
import com.example.fc_sdk_test.flow.FlowContext
import com.example.fc_sdk_test.network.ApiService
import com.example.fc_sdk_test.ui.RumDemoFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 「自定义串联」演示页。点一次按钮,按顺序触发一段业务流程,并用同一个 flow_id 把这段流程里的
 * 自定义事件 / action / resource 串联起来,便于在控制台按 flow_id 聚合查看:
 *
 *   1. 先发送 2 个自定义事件(RUM CUSTOM action)
 *   2. 再发送 1 个 action 事件(RUM CLICK action)
 *   3. 再发送 1 个请求(网络 resource,经拦截器自动带上同一 flow_id)
 *   4. 再发送 2 个自定义事件(RUM CUSTOM action)
 *
 * flow_id 在 action 上作为属性传入(创建时冻结);在 resource 上由 FlowTaggingInterceptor 在
 * 请求发出时写进 request.tag、再由 FlowResourceAttributesProvider 读回,因此并发/重试都不串味。
 */
class CustomCorrelationFragment : RumDemoFragment() {

    override val title: String = "自定义串联 (Custom Correlation)"
    override val logTag: String = "FlowSeq"

    private val apiService = ApiService()

    // 防止序列未跑完又被点击,导致两段 flow 重叠覆盖 FlowContext。
    @Volatile
    private var running = false

    override fun buildScenarios() {
        section("一键串联序列")
        button("▶ 开始串联序列 (2 自定义 → 1 action → 2 请求(成功+失败) → 2 自定义)") {
            startSequence()
        }
        section("说明")
        button("ℹ️ 序列里的事件如何串联?") {
            show(
                "本页按钮会生成一个 flow_id,让序列里的 5 个 RUM 事件 + 1 个网络 resource 都带上它。\n" +
                    "控制台用 flow_id 过滤即可把它们聚合查看。"
            )
        }
    }

    private fun startSequence() {
        if (running) {
            show("⏳ 上一段序列还在执行,请稍候…")
            return
        }
        running = true

        // 开启一段流程:本次序列里所有事件 + 请求共享这个 flow_id。
        val flowId = FlowContext.startFlow()
        Log.d(logTag, "=== flow 开始, flow_id=$flowId ===")
        show("▶ 串联序列开始\nflow_id = $flowId")

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // 1. 先发送 2 个自定义事件
                customEvent("custom_event_1", flowId, seq = 1)
                delay(STEP_GAP_MS)
                customEvent("custom_event_2", flowId, seq = 2)
                delay(STEP_GAP_MS)

                // 2. 再发送 1 个 action 事件
                rum.addAction(
                    RumActionType.CLICK,
                    "correlation_action",
                    mapOf(FlowContext.ATTR_FLOW_ID to flowId, "seq" to 3)
                )
                show("③ action 事件: correlation_action")
                Log.d(logTag, "ACTION correlation_action -> flow_id=$flowId")
                delay(STEP_GAP_MS)

                // 3a. 成功请求 → 拿到 200 响应 → 记成 Resource 事件(带同一 flow_id)
                show("④a 成功请求中…(200 → Resource)")
                apiService.get(URL_SUCCESS)
                    .onSuccess { Log.d(logTag, "RESOURCE(成功) GET $URL_SUCCESS -> flow_id=$flowId") }
                    .onFailure { Log.w(logTag, "成功请求意外失败 -> flow_id=$flowId", it) }
                show("④a 成功请求完成(Resource 带 flow_id)")
                delay(STEP_GAP_MS)

                // 3b. 失败请求 → 不可达,3s 内抛异常(无响应)→ 记成 Error 事件(同样带 flow_id)
                show("④b 失败请求中…(不可达 → Error)")
                apiService.getWithTimeout(URL_FAILURE, FAIL_TIMEOUT_MS)
                    .onSuccess { Log.w(logTag, "失败请求意外成功 -> flow_id=$flowId") }
                    .onFailure { Log.d(logTag, "ERROR(失败) GET $URL_FAILURE -> flow_id=$flowId (${it.javaClass.simpleName})") }
                show("④b 失败请求完成(Error 带 flow_id)")
                delay(STEP_GAP_MS)

                // 4. 再发送 2 个自定义事件
                customEvent("custom_event_3", flowId, seq = 5)
                delay(STEP_GAP_MS)
                customEvent("custom_event_4", flowId, seq = 6)

                Log.d(logTag, "=== flow 结束, flow_id=$flowId ===")
                show(
                    "✓ 串联序列结束\nflow_id = $flowId\n" +
                        "(5 个 action + 1 个成功 Resource + 1 个失败 Error 均带同一 flow_id)"
                )
            } finally {
                FlowContext.clearFlow()
                running = false
            }
        }
    }

    private fun customEvent(name: String, flowId: String, seq: Int) {
        rum.addAction(
            RumActionType.CUSTOM,
            name,
            mapOf(FlowContext.ATTR_FLOW_ID to flowId, "seq" to seq)
        )
        show("${circled(seq)} 自定义事件: $name")
        Log.d(logTag, "CUSTOM $name (seq=$seq) -> flow_id=$flowId")
    }

    private fun circled(seq: Int): String = when (seq) {
        1 -> "①"
        2 -> "②"
        5 -> "⑤"
        6 -> "⑥"
        else -> "•"
    }

    companion object {
        private const val STEP_GAP_MS = 500L

        // 成功:稳定返回 200 → 记成 Resource
        private const val URL_SUCCESS = "https://jsonplaceholder.typicode.com/posts/1"

        // 失败:不存在/不可达的域名;配合 3s call 超时 → 秒级抛异常 → 记成 Error 事件(带 flow_id)
        private const val URL_FAILURE = "https://nonexistent-host.flashcat-demo.invalid/"

        // 失败请求的 call 超时:避免模拟器 DNS/连接挂到默认 30s
        private const val FAIL_TIMEOUT_MS = 3000L
    }
}
