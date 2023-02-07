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

# Drill Client library tests
include_directories("${OPENSSL_INCLUDE_DIR}")
include_directories(${CMAKE_SOURCE_DIR}/src)
add_executable(testSSL ssl/testSSL.cpp )
target_link_libraries(testSSL ${Boost_LIBRARIES} ${PROTOBUF_LIBRARY} drillClient protomsgs y2038 ${OPENSSL_LIBRARIES} )
add_executable(testServer ssl/testServer.cpp )
target_link_libraries(testServer ${Boost_LIBRARIES} ${OPENSSL_LIBRARIES} )
add_executable(testClient ssl/testClient.cpp )
target_link_libraries(testClient ${Boost_LIBRARIES} ${OPENSSL_LIBRARIES} )

