package com.example.fc_sdk_test.flow

import com.datadog.android.rum.RumResourceAttributesProvider
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.UUID

/**
 * 「自定义串联」机制:给一段业务流程一个恒定的 flow_id,从流程开始起,期间产生的
 * 自定义事件 / action / resource 都打上同一个 flow_id,从而在控制台按它聚合查看。
 *
 *  - action / 自定义事件:flow_id 直接作为 addAction 的属性传入,创建时即冻结。
 *  - resource(网络请求):flow_id 由 [FlowTaggingInterceptor] 在请求发出时写进 request.tag,
 *    再由 [FlowResourceAttributesProvider] 读回挂到 resource 事件上。值跟着 request 走,
 *    即使请求重试 / 延迟完成也不会被后续流程串味(并发安全)。
 */
object FlowContext {

    // Demo 级别的持有者:单个「当前」flow_id。本 demo 一次只跑一段流程,够用。
    // 生产中若流程会并发重叠,应把它按流程作用域化(协程 context / 每流程持有者)。
    @Volatile
    private var currentFlowId: String? = null

    /** 开始一段流程并返回其 id;后续请求会继承它,直到 [clearFlow]。 */
    fun startFlow(): String {
        val id = UUID.randomUUID().toString()
        currentFlowId = id
        return id
    }

    /** 当前进行中的 flow_id,没有则为 null。 */
    fun currentFlowId(): String? = currentFlowId

    /** 结束当前流程,之后的请求不再被打标。 */
    fun clearFlow() {
        currentFlowId = null
    }

    const val ATTR_FLOW_ID: String = "flow_id"
}

/** 承载 flow_id 的 OkHttp 强类型 tag,请求发出时冻结在 [Request] 上。 */
internal data class FlowTag(val flowId: String)

/**
 * 把当前 [FlowContext] 的 flow_id 标记到每个出站请求上的拦截器。
 * 必须加在 DatadogInterceptor **之前**,这样 Datadog 记录 resource、向
 * [FlowResourceAttributesProvider] 取属性时 tag 已经在请求上了。
 */
class FlowTaggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val flowId = FlowContext.currentFlowId()
        val request = if (flowId != null) {
            chain.request().newBuilder()
                .tag(FlowTag::class.java, FlowTag(flowId))
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

/**
 * 从请求上读出 [FlowTag],作为 `flow_id` 属性挂到 RUM resource 事件。
 * 拦截器把真实的 OkHttp [Request](tag 完好)传给已废弃的重载,所以实现那个。
 */
class FlowResourceAttributesProvider : RumResourceAttributesProvider {

    @Deprecated("Use the variant with HttpRequestInfo/HttpResponseInfo instead")
    @Suppress("OVERRIDE_DEPRECATION")
    override fun onProvideAttributes(
        request: Request,
        response: Response?,
        throwable: Throwable?
    ): Map<String, Any?> {
        val flowId = request.tag(FlowTag::class.java)?.flowId ?: return emptyMap()
        return mapOf(FlowContext.ATTR_FLOW_ID to flowId)
    }
}
