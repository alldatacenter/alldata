if (USE_KRB5)
    add_executable (kerberos_init Kerberos_init.cpp)
    target_link_libraries (kerberos_init PRIVATE dbms ${KRB5_LIBRARY} ${hdfs3})
endif()
