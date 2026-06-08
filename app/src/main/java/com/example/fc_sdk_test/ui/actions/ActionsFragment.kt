package com.example.fc_sdk_test.ui.actions

import android.os.Handler
import android.os.Looper
import com.datadog.android.rum.RumActionType
import com.datadog.android.rum.featureoperations.FailureReason
import com.example.fc_sdk_test.ui.RumDemoFragment

/**
 * RUM actions & custom-event scenarios: discrete actions of every [RumActionType], a timed
 * start/stop action, business custom events, custom timings, feature-flag evaluations and
 * feature operations.
 */
class ActionsFragment : RumDemoFragment() {

    override val title: String = "Actions & Events"
    private val handler = Handler(Looper.getMainLooper())

    override fun buildScenarios() {
        section("addAction — 各动作类型")
        button("TAP action") {
            rum.addAction(RumActionType.TAP, "manual_tap_button", mapOf("from" to "ActionsFragment"))
            show("✓ addAction(TAP, manual_tap_button)")
        }
        button("CLICK action") {
            rum.addAction(RumActionType.CLICK, "manual_click_button")
            show("✓ addAction(CLICK, manual_click_button)")
        }
        button("SCROLL action") {
            rum.addAction(RumActionType.SCROLL, "manual_scroll")
            show("✓ addAction(SCROLL, manual_scroll)")
        }
        button("SWIPE action") {
            rum.addAction(RumActionType.SWIPE, "manual_swipe")
            show("✓ addAction(SWIPE, manual_swipe)")
        }
        button("BACK action") {
            rum.addAction(RumActionType.BACK, "manual_back")
            show("✓ addAction(BACK, manual_back)")
        }
        button("CUSTOM action") {
            rum.addAction(RumActionType.CUSTOM, "execute_business_logic", mapOf("step" to 1))
            show("✓ addAction(CUSTOM, execute_business_logic)")
        }

        section("start/stop Action — 长动作计时")
        button("计时动作 (1.5s 后 stop)") {
            rum.startAction(RumActionType.CUSTOM, "checkout_flow")
            show("⏳ startAction(checkout_flow)… 1.5s 后 stop")
            handler.postDelayed({
                rum.stopAction(RumActionType.CUSTOM, "checkout_flow", mapOf("result" to "success"))
                show("✓ stopAction(checkout_flow)")
            }, 1500)
        }

        section("自定义业务事件 (addAction CUSTOM + 属性)")
        button("商品购买事件") {
            rum.addAction(
                RumActionType.CUSTOM,
                "商品购买",
                mapOf(
                    "productId" to "SKU-10086",
                    "productName" to "FlashCat Pro",
                    "price" to 199.0,
                    "currency" to "CNY",
                    "quantity" to 2
                )
            )
            show("✓ 自定义事件: 商品购买")
        }
        button("社交分享事件") {
            rum.addAction(
                RumActionType.CUSTOM,
                "社交分享",
                mapOf("platform" to "微信朋友圈", "targetId" to "post_42")
            )
            show("✓ 自定义事件: 社交分享")
        }

        section("addTiming — 自定义性能指标")
        button("上报数据加载耗时") {
            val start = System.currentTimeMillis()
            handler.postDelayed({
                rum.addTiming("data_loaded")
                show("✓ addTiming(data_loaded) @ ${System.currentTimeMillis() - start}ms")
            }, 800)
            show("⏳ 模拟数据加载中…")
        }

        section("Feature Flags")
        button("单个 feature flag 评估") {
            rum.addFeatureFlagEvaluation("new_checkout", true)
            show("✓ addFeatureFlagEvaluation(new_checkout = true)")
        }
        button("批量 feature flag 评估") {
            rum.addFeatureFlagEvaluations(
                mapOf(
                    "dark_mode" to true,
                    "experiment_bucket" to "B",
                    "max_items" to 50
                )
            )
            show("✓ addFeatureFlagEvaluations(3 flags)")
        }

        section("Feature Operations")
        button("操作成功 (start → succeed)") {
            rum.startFeatureOperation("sync_contacts", "op-${System.currentTimeMillis()}")
            handler.postDelayed({
                rum.succeedFeatureOperation("sync_contacts")
                show("✓ succeedFeatureOperation(sync_contacts)")
            }, 1000)
            show("⏳ startFeatureOperation(sync_contacts)…")
        }
        button("操作失败 (start → fail)") {
            val key = "op-${System.currentTimeMillis()}"
            rum.startFeatureOperation("upload_file", key)
            handler.postDelayed({
                rum.failFeatureOperation("upload_file", key, FailureReason.ERROR)
                show("✓ failFeatureOperation(upload_file, ERROR)")
            }, 1000)
            show("⏳ startFeatureOperation(upload_file)…")
        }
    }

    override fun onCleanup() {
        handler.removeCallbacksAndMessages(null)
    }
}
