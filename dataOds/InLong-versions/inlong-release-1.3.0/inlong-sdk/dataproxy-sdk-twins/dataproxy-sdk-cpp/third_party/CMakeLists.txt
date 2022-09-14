#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

cmake_minimum_required(VERSION 3.1)

project(dataproxysdk_third_party)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wno-error")

include(ExternalProject)

ExternalProject_Add(
    snappy_proj
    URL https://github.com/google/snappy/archive/1.1.8.tar.gz
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    CMAKE_ARGS
    -DCMAKE_INSTALL_PREFIX=<INSTALL_DIR>
    -DSNAPPY_BUILD_TESTS=OFF
    -DHAVE_LIBLZO2=OFF
    -DCMAKE_CXX_FLAGS=-fPIC
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 1
    )

ExternalProject_Add(
    curl_proj
    URL https://github.com/curl/curl/releases/download/curl-7_78_0/curl-7.78.0.tar.gz
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    CONFIGURE_COMMAND ./configure --prefix=<INSTALL_DIR> --without-zlib --without-ssl --disable-shared --disable-ldap && make install
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 1
    )

ExternalProject_Add(
    rapidjson
    PREFIX "rapidjson"
    URL "https://github.com/Tencent/rapidjson/archive/v1.1.0.tar.gz"
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
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
    asio_proj
    URL https://github.com/chriskohlhoff/asio/archive/asio-1-18-0.tar.gz 
    INSTALL_DIR ${CMAKE_BINARY_DIR}/third_party/
    CONFIGURE_COMMAND cd ../asio_proj/asio && ./autogen.sh && ./configure --prefix=<INSTALL_DIR> CFLAGS=-std=c++11 CPPFLAGS=-std=c++11 CXXFLAGS=-std=c++11 && make install
    TEST_BEFORE_INSTALL 0
    BUILD_IN_SOURCE 0
    BUILD_COMMAND "" 
    INSTALL_COMMAND ""
    ) 
