if (USE_KRB5)
    add_executable (hdfs_krb5 Hdfs_krb5.cpp)
    target_link_libraries (hdfs_krb5 PRIVATE dbms ${KRB5_LIBRARY} ${HDFS3_LIBRARY})
endif()
