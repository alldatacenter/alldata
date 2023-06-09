if (ENABLE_TESTS)
    set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -w")
    add_subdirectory(tests)
endif ()
