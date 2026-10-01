cmake_minimum_required(VERSION 3.15)
project(DecafCompiler VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(decaf_compiler src/main.cpp)
target_include_directories(decaf_compiler PRIVATE include)