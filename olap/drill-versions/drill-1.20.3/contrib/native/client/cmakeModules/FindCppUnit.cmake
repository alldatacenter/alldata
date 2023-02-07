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

# A simple cmake module to find CppUnit (inspired by
# http://root.cern.ch/viewvc/trunk/cint/reflex/cmake/modules/FindCppUnit.cmake)

#
# - Find CppUnit
# This module finds an installed CppUnit package.
#
# It sets the following variables:
#  CPPUNIT_FOUND       - Set to false if CppUnit isn't found.
#  CPPUNIT_INCLUDE_DIR - The CppUnit include directory.
#  CPPUNIT_LIBRARY     - The CppUnit library to link against.

if (MSVC)
    if (${CMAKE_BUILD_TYPE} MATCHES "Debug")
        set(CPPUNIT_BuildOutputDir "Debug")
        set(CPPUNIT_LibName "cppunitd")
    else()
        set(CPPUNIT_BuildOutputDir "Release")
        set(CPPUNIT_LibName "cppunit")
    endif()
    if ("${CPPUNIT_HOME}_" MATCHES  "^_$")
        message(" ")
        message("- Please set the cache variable CPPUNIT_HOME to point to the directory with the cppunit source.")
        message("- CMAKE will look for cppunit include files in $CPPUNIT_HOME/include.")
        message("- CMAKE will look for cppunit library files in $CPPUNIT_HOME/src/Debug or $CPPUNIT_HOME/src/Release.")
    else()
        file(TO_CMAKE_PATH ${CPPUNIT_HOME} CPPUNIT_HomePath)
        set(CPPUNIT_LIB_PATHS ${CPPUNIT_HomePath}/src/cppunit/${CPPUNIT_BuildOutputDir})

        find_path(CPPUNIT_INCLUDE_DIR cppunit/Test.h ${CPPUNIT_HomePath}/include)
        find_library(CPPUNIT_LIBRARY NAMES ${CPPUNIT_LibName} PATHS ${CPPUNIT_LIB_PATHS})
    endif()
else()
    set(CPPUNIT_LIB_PATHS /usr/local/lib /opt/local/lib)
    find_path(CPPUNIT_INCLUDE_DIR cppunit/Test.h /usr/local/include /opt/local/include)
    find_library(CPPUNIT_LIBRARY NAMES cppunit PATHS ${CPPUNIT_LIB_PATHS})
endif()

if (CPPUNIT_INCLUDE_DIR AND CPPUNIT_LIBRARY)
    set(CPPUNIT_FOUND TRUE)
else (CPPUNIT_INCLUDE_DIR AND CPPUNIT_LIBRARY)
    set(CPPUNIT_FOUND FALSE)
endif (CPPUNIT_INCLUDE_DIR AND CPPUNIT_LIBRARY)

if (CPPUNIT_FOUND)
    message(STATUS "Found CppUnit: ${CPPUNIT_LIBRARY}")
else (CPPUNIT_FOUND)
    message(WARNING "Could not find CppUnit: tests won't compile")
endif (CPPUNIT_FOUND)
