#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Drill Client unit tests
set (TESTS_SRC_FILES
    ${CMAKE_CURRENT_SOURCE_DIR}/CollectionsTest.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/UtilsTest.cpp
    ${CMAKE_CURRENT_SOURCE_DIR}/DrillClientTest.cpp
    )

find_package(CppUnit REQUIRED)
include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/../include )
include_directories(${CMAKE_CURRENT_SOURCE_DIR} ${CMAKE_CURRENT_SOURCE_DIR}/../clientlib )
include_directories(${CPPUNIT_INCLUDE_DIR})
include_directories(${Boost_INCLUDE_DIRS})
include_directories(${PROTOBUF_INCLUDE_DIR})
include_directories(${Zookeeper_INCLUDE_DIRS})
include_directories(${OPENSSL_INCLUDE_DIR})

link_directories(/usr/local/lib)
add_executable(unit-tests ${CMAKE_CURRENT_SOURCE_DIR}/main.cpp ${TESTS_SRC_FILES} )
target_link_libraries(unit-tests drillClient protomsgs y2038 ${CPPUNIT_LIBRARY} ${Boost_LIBRARIES} ${PROTOBUF_LIBRARY} ${Zookeeper_LIBRARIES})

foreach(testfile ${TESTS_SRC_FILES})
get_filename_component(testname ${testfile} NAME_WE)
add_test(NAME ${testname}
         COMMAND unit-tests ${testname})
endforeach(testfile)
