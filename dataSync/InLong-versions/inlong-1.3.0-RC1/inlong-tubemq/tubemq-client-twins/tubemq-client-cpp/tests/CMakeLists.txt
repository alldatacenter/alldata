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


# A function to set up a test, since it's the same for each one.  Note:
# unit_tests test is not set up using this function because it does not like
# the additional argument on commmand line and consequently does not run any
# test.
function(tubemq_add_tests _name)
  set(_srcs ${ARGN})
  message (STATUS "${_name} sources: ${_srcs}")
  add_executable (${_name} ${_srcs})
  TARGET_LINK_LIBRARIES (${_name} tubemq ssl crypto liblog4cplus.a libprotobuf.a libsnappy.a tubemq_proto pthread rt)
endfunction()

add_subdirectory (log)
add_subdirectory (executor_pool)
add_subdirectory (thread_pool)
