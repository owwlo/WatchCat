cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++14")
add_library(native-nv21Utils
        SHARED
        native-nv21Utils.cpp)
target_link_libraries(native-nv21Utils
        android)
