cmake_minimum_required(VERSION 3.22.1)

project("fcsdktestnative")

add_library(fcsdktest-native SHARED native-crash.cpp)

find_library(log-lib log)

target_link_libraries(fcsdktest-native ${log-lib})
