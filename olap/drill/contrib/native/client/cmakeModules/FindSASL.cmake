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

# - Try to find Cyrus SASL

if (MSVC)
    if("${SASL_HOME}_" MATCHES "^_$")
        message(" ")
        message("- Please set the cache variable SASL_HOME to point to the directory with the Cyrus SASL source.")
        message("- CMAKE will look for Cyrus SASL include files in $SASL_HOME/include or $SASL_HOME/win32/include.")
        message("- CMAKE will look for Cyrus SASL library files in $SASL_HOME/lib.")
    else()
        FILE(TO_CMAKE_PATH ${SASL_HOME} SASL_HomePath)
        set(SASL_LIB_PATHS ${SASL_HomePath}/lib)

        find_path(SASL_INCLUDE_DIR sasl.h ${SASL_HomePath}/include ${SASL_HomePath}/win32/include)
        find_library(SASL_LIBRARY NAMES "libsasl2${CMAKE_SHARED_LIBRARY_SUFFIX}" PATHS ${SASL_LIB_PATHS})
    endif()
else()
    set(SASL_LIB_PATHS /usr/local/lib /opt/local/lib)
    find_path(SASL_INCLUDE_DIR sasl/sasl.h /usr/local/include /opt/local/include)
    find_library(SASL_LIBRARY NAMES "libsasl2${CMAKE_SHARED_LIBRARY_SUFFIX}" PATHS ${SASL_LIB_PATHS})
endif()


set(SASL_LIBRARIES ${SASL_LIBRARY})
set(SASL_INCLUDE_DIRS ${SASL_INCLUDE_DIR})

include(FindPackageHandleStandardArgs)
# handle the QUIETLY and REQUIRED arguments and set SASL_FOUND to TRUE if all listed variables are valid
find_package_handle_standard_args(SASL  DEFAULT_MSG
    SASL_LIBRARY SASL_INCLUDE_DIR)

mark_as_advanced(SASL_INCLUDE_DIR SASL_LIBRARY)
