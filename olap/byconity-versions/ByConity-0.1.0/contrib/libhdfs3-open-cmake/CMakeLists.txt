if (NOT USE_INTERNAL_PROTOBUF_LIBRARY)
    # compatiable with protobuf which was compiled old C++ ABI
    set(CMAKE_CXX_FLAGS "-D_GLIBCXX_USE_CXX11_ABI=0")
    set(CMAKE_C_FLAGS "")
    if (NOT (CMAKE_VERSION VERSION_LESS "3.8.0"))
        unset(CMAKE_CXX_STANDARD)
    endif ()
endif()

SET(WITH_KERBEROS false)
SET(WITH_CURL false)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DUSE_POCO_LOGGER")

# project and source dir
set(HDFS3_ROOT_DIR ${CMAKE_SOURCE_DIR}/contrib/libhdfs3-open)
set(HDFS3_SOURCE_DIR ${HDFS3_ROOT_DIR}/src)
set(HDFS3_COMMON_DIR ${HDFS3_SOURCE_DIR}/common)

message(STATUS "Use libhdfs3 of open version at ${HDFS3_ROOT_DIR}")


# module
set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/CMake" ${CMAKE_MODULE_PATH})
include(Platform)
include(Options)

# prefer shared libraries
if (WITH_KERBEROS)
    find_package(KERBEROS REQUIRED)
endif()

if (WITH_CURL)
    find_package(CURL REQUIRED)
endif()

# source
set(PROTO_FILES
    ${HDFS3_SOURCE_DIR}/proto/encryption.proto
    ${HDFS3_SOURCE_DIR}/proto/ClientDatanodeProtocol.proto
    ${HDFS3_SOURCE_DIR}/proto/hdfs.proto
    ${HDFS3_SOURCE_DIR}/proto/Security.proto
    ${HDFS3_SOURCE_DIR}/proto/ProtobufRpcEngine.proto
    ${HDFS3_SOURCE_DIR}/proto/ClientNamenodeProtocol.proto
    ${HDFS3_SOURCE_DIR}/proto/IpcConnectionContext.proto
    ${HDFS3_SOURCE_DIR}/proto/RpcHeader.proto
    ${HDFS3_SOURCE_DIR}/proto/datatransfer.proto
    )

if(USE_PROTOBUF)
    PROTOBUF_GENERATE_CPP(PROTO_SOURCES PROTO_HEADERS ${PROTO_FILES})
endif()

configure_file(${HDFS3_SOURCE_DIR}/platform.h.in ${CMAKE_CURRENT_BINARY_DIR}/platform.h)


file(GLOB_RECURSE SRCS
        "${HDFS3_SOURCE_DIR}/*.h"
        "${HDFS3_SOURCE_DIR}/*.cpp"
        )
# target
add_library(hdfs3 STATIC ${SRCS} ${PROTO_SOURCES} ${PROTO_HEADERS})

if (USE_INTERNAL_PROTOBUF_LIBRARY)
    add_dependencies(hdfs3 protoc)
endif()
target_include_directories(hdfs3 PRIVATE ${HDFS3_SOURCE_DIR})
target_include_directories(hdfs3 PRIVATE ${HDFS3_COMMON_DIR})
target_include_directories(hdfs3 PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
target_include_directories(hdfs3 PRIVATE ${HDFS3_ROOT_DIR})
target_include_directories(hdfs3 PRIVATE ${HDFS3_ROOT_DIR}/contrib)

target_include_directories(hdfs3 PRIVATE ${LIBGSASL_INCLUDE_DIR})
if (WITH_KERBEROS)
    target_include_directories(hdfs3 PRIVATE ${KERBEROS_INCLUDE_DIRS})
endif()

if (WITH_CURL)
    target_include_directories(hdfs3 PRIVATE ${CURL_INCLUDE_DIRS})
endif()

target_include_directories(hdfs3 PRIVATE ${LIBXML2_INCLUDE_DIR})


target_link_libraries(hdfs3 ${LIBGSASL_LIBRARY})
if (WITH_KERBEROS)
    target_link_libraries(hdfs3 ${KERBEROS_LIBRARIES})
endif()

if (WITH_CURL)
    target_link_libraries(hdfs3 ${CURL_LIBRARIES})
endif()

target_link_libraries(hdfs3 ${LIBXML2_LIBRARIES})


# inherit from parent cmake
target_include_directories(hdfs3 PRIVATE ${Protobuf_INCLUDE_DIR})

#poco logger
#target_include_directories(hdfs3 PRIVATE ${Poco_INCLUDE_DIRS})
target_link_libraries(hdfs3 Poco::Foundation)

# libgsasl
#target_compile_definitions (hdfs3 PRIVATE HAVE_CONFIG_H=1)

target_link_libraries(hdfs3 ${Protobuf_LIBRARY})
target_link_libraries(hdfs3  ${Protobuf_LIBRARY} boost::headers_only)
if(OPENSSL_INCLUDE_DIR AND OPENSSL_LIBRARIES)
    target_include_directories(hdfs3 PRIVATE ${OPENSSL_INCLUDE_DIR})
    target_link_libraries(hdfs3 ${OPENSSL_LIBRARIES})
endif()
