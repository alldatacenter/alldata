CMAKE_MINIMUM_REQUIRED(VERSION 3.1)                                             

PROJECT(TubeMQThirdParty)

SET(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error")

include(ExternalProject)

ExternalProject_Add(
    rapidjson
    PREFIX "rapidjson"
    URL "https://github.com/Tencent/rapidjson/archive/v1.1.0.tar.gz"
    CMAKE_ARGS
    -DRAPIDJSON_BUILD_TESTS=OFF
    -DRAPIDJSON_BUILD_DOC=OFF
    -DRAPIDJSON_BUILD_EXAMPLES=OFF
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
    UPDATE_COMMAND ""
    )

ExternalProject_Add(
    snappy_proj
    URL https://github.com/google/snappy/archive/1.1.8.tar.gz
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    CMAKE_ARGS
    -DCMAKE_INSTALL_PREFIX=<INSTALL_DIR>
    -DCMAKE_CXX_FLAGS=-fPIC
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 1
    )

ExternalProject_Add(
    log4cplus_proj
    URL https://github.com/log4cplus/log4cplus/releases/download/REL_2_0_5/log4cplus-2.0.5.tar.gz
    CONFIGURE_COMMAND ./configure --prefix=<INSTALL_DIR> --disable-shared --with-pic CFLAGS=-O2\  CXXFLAGS=-O2\ -fPIC
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 1
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    )

ExternalProject_Add(
    asio_proj
    URL https://github.com/chriskohlhoff/asio/archive/asio-1-18-0.tar.gz 
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    CONFIGURE_COMMAND cd ../asio_proj/asio && ./autogen.sh && ./configure --prefix=<INSTALL_DIR> CFLAGS=-std=c++11 CPPFLAGS=-std=c++11 CXXFLAGS=-std=c++11 && make install
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 0
    BUILD_COMMAND "" 
    INSTALL_COMMAND ""
    )

