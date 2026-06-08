#include <jni.h>
#include <signal.h>
#include <stdlib.h>

// Each scenario crashes through a distinctly-named function so the captured
// native stack has a unique top frame. This exercises NDK issue grouping:
// different functions (even with the same signal) should produce different
// issues, while repeating the same scenario should collapse into one.

// region SIGSEGV scenarios

// Scenario 0: classic null pointer dereference (read from address 0x0).
int crash_null_dereference() {
    volatile int *pointer = nullptr;
#pragma clang diagnostic push
#pragma ide diagnostic ignored "NullDereferences"
    return *pointer;
#pragma clang diagnostic pop
}

// Scenario 1: write to a wild / invalid pointer. Same signal as the null
// dereference (SIGSEGV) but a different top frame -> a distinct issue.
void crash_wild_pointer_write() {
    volatile int *pointer = reinterpret_cast<int *>(0xDEAD);
    *pointer = 42;
}

// Scenario 2: unbounded recursion exhausts the stack -> SIGSEGV.
int crash_stack_overflow(int depth) {
    volatile char buffer[1024];
    buffer[0] = static_cast<char>(depth & 0xFF);
    return buffer[0] + crash_stack_overflow(depth + 1);
}

// Scenario 3: crash reached through a small call chain, producing a
// multi-frame meaningful stack (level_one -> level_two -> level_three -> deref).
// The leaf dereferences directly so its top frame is unique.
int crash_nested_level_three() {
    volatile int *pointer = nullptr;
#pragma clang diagnostic push
#pragma ide diagnostic ignored "NullDereferences"
    return *pointer;
#pragma clang diagnostic pop
}
int crash_nested_level_two() {
    return crash_nested_level_three();
}
int crash_nested_level_one() {
    return crash_nested_level_two();
}

// endregion

// region SIGABRT scenarios

// Scenario 4: explicit abort().
void crash_via_abort() {
    abort();
}

// Scenario 5: uncaught C++ exception -> std::terminate -> SIGABRT.
#pragma clang diagnostic push
#pragma ide diagnostic ignored "hicpp-exception-baseclass"
void crash_uncaught_cpp_exception() {
    throw "Unhandled Exception";
}
#pragma clang diagnostic pop

// endregion

// region SIGILL scenario

// Scenario 6: execute an illegal instruction (reliable trap on all ABIs).
void crash_illegal_instruction() {
    __builtin_trap();
}

// endregion

extern "C" JNIEXPORT void JNICALL
Java_com_example_fc_1sdk_1test_NativeCrash_simulateNdkCrash(
        JNIEnv *env,
        jobject thiz,
        jint scenario) {

    switch (scenario) {
        case 0:
            crash_null_dereference();
            break;
        case 1:
            crash_wild_pointer_write();
            break;
        case 2:
            crash_stack_overflow(0);
            break;
        case 3:
            crash_nested_level_one();
            break;
        case 4:
            crash_via_abort();
            break;
        case 5:
            crash_uncaught_cpp_exception();
            break;
        case 6:
            crash_illegal_instruction();
            break;
        default:
            crash_null_dereference();
            break;
    }
}
