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

project(DataProxySDK)

# compile level -O2
set(CMAKE_CXX_FLAGS "-std=c++11 -O2 -g -Wall  -Wsign-compare -Wfloat-equal -fno-strict-aliasing -fPIC -DASIO_STANDALONE")

include_directories(${CMAKE_BINARY_DIR}/third_party/include)
include_directories(${CMAKE_BINARY_DIR}/third_party/rapidjson/src/rapidjson/include)
include_directories(release/inc)
include_directories(src/base)
include_directories(src/net)

link_directories(${CMAKE_BINARY_DIR}/third_party/lib)
link_directories(${CMAKE_BINARY_DIR}/third_party/lib64)

add_subdirectory(third_party)
add_subdirectory(src/base)
add_subdirectory(src/net)
# add_subdirectory(test)
add_subdirectory(release)

set(CMAKE_ARCHIVE_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/release/lib)
# set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/release/so)
aux_source_directory(src/base BASE_SRCS)
aux_source_directory(src/net NET_SRCS)

# dynamic library
# # add_library(dataproxy_sdk SHARED ${BASE_SRCS} ${NET_SRCS})
# # set_target_properties(dataproxy_sdk PROPERTIES PREFIX "")
# # target_link_libraries(dataproxy_sdk -llog4cplus -lsnappy -lcurl)

# static library
add_library(dataproxy_sdk STATIC ${BASE_SRCS} ${NET_SRCS})
set_target_properties(dataproxy_sdk PROPERTIES OUTPUT_NAME "dataproxy_sdk" PREFIX "")
target_link_libraries(dataproxy_sdk liblog4cplus.a libsnappy.a libcurl.a)

