package com.example.fc_sdk_test.ui.context

import com.datadog.android.Datadog
import com.example.fc_sdk_test.ui.RumDemoFragment

/**
 * User identity and context scenarios: [Datadog] user info, global RUM attributes, per-view
 * attributes, session control and time-to-fully-displayed reporting.
 */
class ContextFragment : RumDemoFragment() {

    override val title: String = "User & Context"

    override fun buildScenarios() {
        section("用户信息 (Datadog)")
        button("setUserInfo (id/name/email)") {
            Datadog.setUserInfo(
                id = "user-10086",
                name = "Bai Yang",
                email = "baiyang@example.com",
                extraInfo = mapOf("plan" to "pro")
            )
            show("✓ setUserInfo(user-10086)")
        }
        button("addUserProperties") {
            Datadog.addUserProperties(mapOf("vip_level" to 3, "region" to "CN"))
            show("✓ addUserProperties(vip_level, region)")
        }
        button("clearUserInfo") {
            Datadog.clearUserInfo()
            show("✓ clearUserInfo()")
        }

        section("全局属性 (RUM)")
        button("addAttribute(tenant)") {
            rum.addAttribute("tenant", "flashcat-cn")
            show("✓ addAttribute(tenant)\n当前: ${rum.getAttributes()}")
        }
        button("addAttribute(feature_branch)") {
            rum.addAttribute("feature_branch", "rum-demo")
            show("✓ addAttribute(feature_branch)\n当前: ${rum.getAttributes()}")
        }
        button("removeAttribute(tenant)") {
            rum.removeAttribute("tenant")
            show("✓ removeAttribute(tenant)\n当前: ${rum.getAttributes()}")
        }
        button("clearAttributes") {
            rum.clearAttributes()
            show("✓ clearAttributes()\n当前: ${rum.getAttributes()}")
        }
        button("查看当前全局属性") {
            show("当前全局属性: ${rum.getAttributes()}")
        }

        section("视图属性 / 会话")
        button("addViewAttributes") {
            rum.addViewAttributes(mapOf("screen_variant" to "A", "logged_in" to true))
            show("✓ addViewAttributes(当前视图)")
        }
        button("reportAppFullyDisplayed") {
            rum.reportAppFullyDisplayed()
            show("✓ reportAppFullyDisplayed()")
        }
        button("stopSession (结束当前会话)") {
            rum.stopSession()
            show("✓ stopSession() — 下个交互会开启新会话")
        }
    }
}
