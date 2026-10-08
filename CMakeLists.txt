cmake_minimum_required(VERSION 3.15)
project(DecafCompiler VERSION 1.0 LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_library(decaf_core
    src/lexer/lexer.cpp
    src/parser/parser.cpp
)
target_include_directories(decaf_core PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include/decaf_compiler/lexer
    ${CMAKE_CURRENT_SOURCE_DIR}/include/decaf_compiler/parser
)

add_executable(decaf_compiler src/main.cpp)
target_link_libraries(decaf_compiler PRIVATE decaf_core)

include(CTest)

if(BUILD_TESTING)
    function(decaf_add_test NAME PHASE FILE)
        cmake_parse_arguments(ARG "EXPECT_FAIL" "EXPECTED" "" ${ARGN})
        set(args
            -DEXE=$<TARGET_FILE:decaf_compiler>
            -DPHASE=${PHASE}
            -DFILE=${FILE}
            -P ${CMAKE_CURRENT_SOURCE_DIR}/tests/run_test.cmake)
        if(ARG_EXPECTED)
            list(PREPEND args -DEXPECTED=${ARG_EXPECTED})
        endif()
        if(ARG_EXPECT_FAIL)
            list(PREPEND args -DEXPECT_FAIL=TRUE)
        endif()
        add_test(NAME ${NAME} COMMAND ${CMAKE_COMMAND} ${args})
    endfunction()

    file(GLOB SCAN_TEST_FILES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/tests/scan_*.decaf)
    foreach(test_file IN LISTS SCAN_TEST_FILES)
        get_filename_component(test_name ${test_file} NAME_WE)
        string(REGEX REPLACE "\\.decaf$" ".expected" expected_file "${test_file}")
        decaf_add_test(${test_name} scan ${test_file} EXPECTED ${expected_file})
    endforeach()

    file(GLOB PARSE_OK_TEST_FILES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/tests/parse_ok_*.decaf)
    foreach(test_file IN LISTS PARSE_OK_TEST_FILES)
        get_filename_component(test_name ${test_file} NAME_WE)
        decaf_add_test(${test_name} parse ${test_file})
    endforeach()

    file(GLOB PARSE_BAD_TEST_FILES CONFIGURE_DEPENDS ${CMAKE_CURRENT_SOURCE_DIR}/tests/parse_bad_*.decaf)
    foreach(test_file IN LISTS PARSE_BAD_TEST_FILES)
        get_filename_component(test_name ${test_file} NAME_WE)
        decaf_add_test(${test_name} parse ${test_file} EXPECT_FAIL)
    endforeach()
endif()
