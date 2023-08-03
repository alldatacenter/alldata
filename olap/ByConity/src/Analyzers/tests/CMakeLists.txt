if(USE_GTEST)
    add_executable(test_name_resolution test_name_resolution.cpp)
    target_link_libraries(test_name_resolution PRIVATE dbms ${GTEST_BOTH_LIBRARIES})
endif()
