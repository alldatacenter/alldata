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

# - Try to find Zookeeper
# Defines
#  Zookeeper_FOUND - System has Zookeeper
#  Zookeeper_INCLUDE_DIRS - The Zookeeper include directories
#  Zookeeper_LIBRARIES - The libraries needed to use Zookeeper
#  Zookeeper_DEFINITIONS - Compiler switches required for using LibZookeeper

#find_package(PkgConfig)
#pkg_check_modules(PC_LIBXML QUIET libxml-2.0)
#set(Zookeeper_DEFINITIONS ${PC_LIBXML_CFLAGS_OTHER})

if (MSVC)
    if(${CMAKE_BUILD_TYPE} MATCHES "Debug")
        set(ZK_BuildOutputDir "Debug")
        set(ZK_LibName "zookeeper_d")
    else()
        set(ZK_BuildOutputDir "Release")
        set(ZK_LibName "zookeeper")
    endif()
    if("${ZOOKEEPER_HOME}_" MATCHES  "^_$")
        message(" ")
        message("- Please set the cache variable ZOOKEEPER_HOME to point to the directory with the zookeeper source.")
        message("- CMAKE will look for zookeeper include files in $ZOOKEEPER_HOME/src/c/include.")
        message("- CMAKE will look for zookeeper library files in $ZOOKEEPER_HOME/src/c/Debug or $ZOOKEEPER_HOME/src/c/Release.")
    else()
        FILE(TO_CMAKE_PATH ${ZOOKEEPER_HOME} Zookeeper_HomePath)
        set(Zookeeper_LIB_PATHS ${Zookeeper_HomePath}/src/c/${ZK_BuildOutputDir} ${Zookeeper_HomePath}/src/c/x64/${ZK_BuildOutputDir} )

        find_path(ZK_INCLUDE_DIR zookeeper.h ${Zookeeper_HomePath}/src/c/include)
        find_path(ZK_INCLUDE_DIR_GEN zookeeper.jute.h ${Zookeeper_HomePath}/src/c/generated)
        set(Zookeeper_INCLUDE_DIR zookeeper.h ${ZK_INCLUDE_DIR} ${ZK_INCLUDE_DIR_GEN} )
        find_library(Zookeeper_LIBRARY NAMES ${ZK_LibName} PATHS ${Zookeeper_LIB_PATHS})
    endif()
else()
    set(Zookeeper_LIB_PATHS /usr/local/lib /opt/local/lib)
    find_path(Zookeeper_INCLUDE_DIR zookeeper/zookeeper.h /usr/local/include)
    find_library(Zookeeper_LIBRARY NAMES zookeeper_mt PATHS ${Zookeeper_LIB_PATHS})
endif()


set(Zookeeper_LIBRARIES ${Zookeeper_LIBRARY} )
set(Zookeeper_INCLUDE_DIRS ${Zookeeper_INCLUDE_DIR} )

include(FindPackageHandleStandardArgs)
# handle the QUIETLY and REQUIRED arguments and set Zookeeper_FOUND to TRUE
# if all listed variables are TRUE
find_package_handle_standard_args(Zookeeper  DEFAULT_MSG
    Zookeeper_LIBRARY Zookeeper_INCLUDE_DIR)

mark_as_advanced(Zookeeper_INCLUDE_DIR Zookeeper_LIBRARY )
