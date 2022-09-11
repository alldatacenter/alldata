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


cmake_minimum_required (VERSION 3.1)

project (TubeMQ)

find_package(Protobuf REQUIRED)

SET(CMAKE_CXX_FLAGS "-std=c++11 -O2 -g -Wall -Werror -Wno-error=class-memaccess -Wsign-compare -Wfloat-equal -fno-strict-aliasing -fPIC -DASIO_STANDALONE")

INCLUDE_DIRECTORIES(${CMAKE_BINARY_DIR}/proto)

INCLUDE_DIRECTORIES(include)
INCLUDE_DIRECTORIES(src)

INCLUDE_DIRECTORIES(${CMAKE_BINARY_DIR}/third_party/rapidjson/src/rapidjson/include)
INCLUDE_DIRECTORIES(${CMAKE_BINARY_DIR}/third_party/include)
LINK_DIRECTORIES(${CMAKE_BINARY_DIR}/third_party/lib)
LINK_DIRECTORIES(${CMAKE_BINARY_DIR}/third_party/lib64)

ADD_SUBDIRECTORY(third_party)
ADD_SUBDIRECTORY(proto)
ADD_SUBDIRECTORY(src)
ADD_SUBDIRECTORY(example)
ADD_SUBDIRECTORY(tests)


