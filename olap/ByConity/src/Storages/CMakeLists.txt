add_subdirectory(MergeTree)
add_subdirectory(System)

if(ENABLE_EXAMPLES)
    add_subdirectory(examples)
    add_subdirectory(HDFS/examples)
endif()

# tools for verifying hdfs.
add_subdirectory(HDFS/hdfs_tool)