package com.example.fc_sdk_test.ui.vitals

import android.graphics.Bitmap
import com.example.fc_sdk_test.ui.RumDemoFragment

/**
 * Performance / vitals scenarios: long tasks, frozen frames, slow frames, a non-fatal ANR, a CPU
 * stress toggle and memory pressure. The main-thread blocks are picked up by the SDK's long-task /
 * ANR tracking (see [FcSdkTestApplication]); the toggles feed the CPU & memory vitals.
 */
class VitalsFragment : RumDemoFragment() {

    override val title: String = "Vitals / Perf"

    @Volatile
    private var cpuRunning = false
    private var cpuThread: Thread? = null
    private val bitmaps = mutableListOf<Bitmap>()

    override fun buildScenarios() {
        section("主线程阻塞")
        button("Long Task (~250ms)") {
            blockMainThread(250)
            show("✓ 主线程阻塞 ~250ms (long task)")
        }
        button("Frozen Frame (~900ms)") {
            blockMainThread(900)
            show("✓ 主线程阻塞 ~900ms (frozen frame)")
        }
        button("Slow Frames (连续 10×80ms)") {
            repeat(10) { blockMainThread(80) }
            show("✓ 连续 10 次 80ms 阻塞 (slow frames)")
        }
        button("ANR (~5s, 非致命)") {
            show("⏳ 主线程将阻塞 ~5s…")
            blockMainThread(5000)
            show("✓ 主线程阻塞 ~5s (non-fatal ANR)")
        }

        section("CPU 压力 (后台)")
        button("开始 / 停止 CPU 压力") {
            if (cpuRunning) stopCpu() else startCpu()
        }

        section("内存压力")
        button("分配 20 张大 Bitmap") {
            repeat(20) {
                // 1080x1920 ARGB_8888 ≈ 8MB each
                bitmaps.add(Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888))
            }
            show("✓ 已分配 ${bitmaps.size} 张大 Bitmap (~${bitmaps.size * 8}MB)")
        }
        button("释放 Bitmap") {
            val n = bitmaps.size
            bitmaps.forEach { it.recycle() }
            bitmaps.clear()
            show("✓ 已释放 $n 张 Bitmap")
        }
    }

    private fun blockMainThread(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (_: InterruptedException) {
        }
    }

    private fun startCpu() {
        cpuRunning = true
        cpuThread = Thread {
            var x = 0.0
            var i = 1.0
            while (cpuRunning) {
                // Leibniz series for Pi — pure busy work
                x += (if (i.toInt() % 2 == 0) -1.0 else 1.0) / (2 * i - 1)
                i++
                if (i > 1_000_000) i = 1.0
            }
        }.also { it.start() }
        show("⏳ CPU 压力运行中… 再点一次停止")
    }

    private fun stopCpu() {
        cpuRunning = false
        cpuThread?.interrupt()
        cpuThread = null
        show("✓ CPU 压力已停止")
    }

    override fun onCleanup() {
        stopCpu()
        bitmaps.forEach { it.recycle() }
        bitmaps.clear()
    }
}
