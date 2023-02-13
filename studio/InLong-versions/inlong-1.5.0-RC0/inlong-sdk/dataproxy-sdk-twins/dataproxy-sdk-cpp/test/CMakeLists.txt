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

function(sdk_add_tests _name)
    set(_srcs ${ARGN})
    message(STATUS "${_name} sources: ${_srcs}")
    add_executable(${_name} ${_srcs})
    include_directories(release/inc)
    target_link_libraries(${_name} sdk_base sdk_net pthread gtest gmock)
endfunction(sdk_add_tests _name)


add_subdirectory(base)
add_subdirectory(net)
add_subdirectory(main)
