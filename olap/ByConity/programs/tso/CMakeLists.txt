if(ENABLE_TESTS)

add_executable(tso_client_test client_test.cpp)
target_link_libraries(tso_client_test PRIVATE tso_lib dbms)

add_custom_command(TARGET tso_client_test POST_BUILD
        COMMAND ${CMAKE_COMMAND} -E copy $<TARGET_FILE:tso_client_test> ${PROJECT_BINARY_DIR}/programs/tso_client_test)
endif()
