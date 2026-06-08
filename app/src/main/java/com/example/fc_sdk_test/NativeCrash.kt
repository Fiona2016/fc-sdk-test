package com.example.fc_sdk_test

/**
 * Bridge to the native crash library. Each scenario crashes through a distinctly
 * named C++ function so the captured native stack has a unique top frame, which
 * lets us exercise NDK issue grouping:
 *  - same scenario twice  -> one issue (dedup)
 *  - same signal, different function (NULL_DEREF vs WILD_POINTER) -> two issues
 *  - different signal (SIGSEGV vs SIGABRT) -> two issues
 */
object NativeCrash {

    init {
        System.loadLibrary("fcsdktest-native")
    }

    // Scenario codes must match the switch in src/main/cpp/native-crash.cpp.
    const val SCENARIO_NULL_DEREF = 0 // SIGSEGV - null pointer dereference
    const val SCENARIO_WILD_POINTER = 1 // SIGSEGV - write to invalid pointer
    const val SCENARIO_STACK_OVERFLOW = 2 // SIGSEGV - unbounded recursion
    const val SCENARIO_NESTED = 3 // SIGSEGV - reached via a call chain
    const val SCENARIO_ABORT = 4 // SIGABRT - abort()
    const val SCENARIO_CPP_EXCEPTION = 5 // SIGABRT - uncaught C++ exception
    const val SCENARIO_ILLEGAL_INSTRUCTION = 6 // SIGILL - illegal instruction

    external fun simulateNdkCrash(scenario: Int)
}
