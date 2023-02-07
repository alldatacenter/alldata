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
cmake_minimum_required(VERSION 3.1.3)

set (CMAKE_CXX_STANDARD 11)
set (CMAKE_CXX_STANDARD_REQUIRED ON)

project(drillclient)
cmake_policy(SET CMP0043 NEW)
cmake_policy(SET CMP0048 NEW)
enable_testing()

#
# required version for dependencies
#
set (BOOST_MINIMUM_VERSION 1.54.0)
set (PROTOBUF_MINIMUM_VERSION 3.6.1)


set (DRILL_ROOT ${CMAKE_SOURCE_DIR}/../../..)
if (NOT DEFINED DRILL_VERSION)
    message("Detecting Drill version:")
    if (WIN32)
        find_program(POWERSHELL Powershell)
        file(TO_NATIVE_PATH "${DRILL_ROOT}/pom.xml" DRILL_POM_FILE)    
        execute_process(
            COMMAND ${POWERSHELL} "Select-Xml -Namespace @{'m'='http://maven.apache.org/POM/4.0.0'} -XPath '//m:project/m:version/text()' -Path ${DRILL_POM_FILE} | foreach {$_.Node.Value}"
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            OUTPUT_VARIABLE DRILL_VERSION
            OUTPUT_STRIP_TRAILING_WHITESPACE
        )
    else ()
        find_program(MVN mvn)
        execute_process(
            COMMAND ${MVN} -q -f ${DRILL_ROOT}/pom.xml -Dexec.executable=echo -Dexec.args=\${project.version} --non-recursive org.codehaus.mojo:exec-maven-plugin:1.3.1:exec
            WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
            OUTPUT_VARIABLE DRILL_VERSION
            OUTPUT_STRIP_TRAILING_WHITESPACE
            )
    endif ()
    if ("${DRILL_VERSION}" STREQUAL "")
        message(FATAL_ERROR "Cannot detect Drill version")
    endif()
endif()

string(REGEX REPLACE "^([0-9]+\\.[0-9]+\\.[0-9]+).*" "\\1"
       DRILL_VERSION
       ${DRILL_VERSION}
       )

project(drillclient
        VERSION ${DRILL_VERSION}
        )

message("Project Dir = ${PROJECT_SOURCE_DIR}")
message("Project Version = ${PROJECT_VERSION} ")
message("Source Dir = ${CMAKE_SOURCE_DIR} ")


set(CMAKE_MODULE_PATH ${CMAKE_MODULE_PATH} "${CMAKE_SOURCE_DIR}/cmakeModules/")

# Get the latest git commit properties of the working branch
execute_process(
    COMMAND git log -1 --format="\\nCommit: %H \\nDescription: %s \\nAuthor: %aN Date: %ai"
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    OUTPUT_VARIABLE GIT_COMMIT_PROP
    OUTPUT_STRIP_TRAILING_WHITESPACE
    )
execute_process(
    COMMAND git log -1 --format="%H"
    WORKING_DIRECTORY ${CMAKE_SOURCE_DIR}
    OUTPUT_VARIABLE GIT_SHA_PROP
    OUTPUT_STRIP_TRAILING_WHITESPACE
    )
STRING(REPLACE . " " GIT_COMMIT_PROP "${GIT_COMMIT_PROP}")
STRING(REPLACE \" "" GIT_COMMIT_PROP "${GIT_COMMIT_PROP}")
set(GIT_COMMIT_PROP "\"${GIT_COMMIT_PROP}\"")
add_definitions("-DGIT_COMMIT_PROP=${GIT_COMMIT_PROP}")



# Find Boost
set(Boost_USE_STATIC_LIBS ON)
set(Boost_USE_MULTITHREADED ON)
if(MSVC)
    set(Boost_USE_STATIC_RUNTIME OFF)
else()
    #    To build a production version, the linux/macos build must use a shaded version
    #    of boost. Arbirtarily, we choose the new namspace to be drill_boost.
    #    See the instructions in the readme for linux/macos and rebuild boost. Then
    #    uncomment the line below to build
    #    set(Boost_NAMESPACE drill_boost)
endif()

find_package(Boost ${BOOST_MINIMUM_VERSION} REQUIRED COMPONENTS regex system date_time chrono thread random)
include_directories(${Boost_INCLUDE_DIRS})


if(CMAKE_COMPILER_IS_GNUCXX OR CMAKE_COMPILER_IS_GNUCC)
    set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -lrt -lpthread")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")
endif()

if(MSVC)
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} /EHsc")
endif()

if(MSVC)
    # ask boost to not to auto link any libraries
    add_definitions(-DBOOST_ALL_NO_LIB)
    # use static libs with zookeeper
    add_definitions(-DUSE_STATIC_LIB)
else()
    add_definitions(-DBOOST_ALL_DYN_LINK)
    #suppress boost warnings (clang, gcc)
    add_definitions ("-Wno-unused-local-typedefs")
endif()


# Find Protobufs
find_package(Protobuf ${PROTOBUF_MINIMUM_VERSION} REQUIRED)
include_directories(${PROTOBUF_INCLUDE_DIR})

if (MSVC)
    set(OPENSSL_USE_STATIC_LIBS TRUE)
endif()
#Find SSL
find_package(OpenSSL REQUIRED )
if(OPENSSL_FOUND)
    add_definitions("-DIS_SSL_ENABLED=1")
endif()

#Find Zookeeper
find_package(Zookeeper REQUIRED )

# Find Cyrus SASL
find_package(SASL REQUIRED)

# Generated sources
configure_file(
    ${CMAKE_SOURCE_DIR}/src/clientlib/env.h.in
    ${CMAKE_BINARY_DIR}/generated/env.h
    )

include_directories(${CMAKE_BINARY_DIR}/generated)


#
#   TARGETS
#

# Preprocess to fix protobuf .proto definitions
add_subdirectory("${CMAKE_SOURCE_DIR}/src/protobuf")
# protobuf includes are required by clientlib
include_directories(${ProtoHeadersDir})
include_directories(${ProtoIncludesDir})

# Y2038 library
add_subdirectory("${CMAKE_SOURCE_DIR}/src/clientlib/y2038")

# Build the Client Library as a shared library
add_subdirectory("${CMAKE_SOURCE_DIR}/src/clientlib")
include_directories(${CMAKE_SOURCE_DIR}/src/include ${Zookeeper_INCLUDE_DIRS})

include_directories(${SASL_INCLUDE_DIRS})

add_subdirectory("${CMAKE_SOURCE_DIR}/src/test")

# add a DEBUG preprocessor macro
set_property(
    DIRECTORY
    PROPERTY COMPILE_DEFINITIONS_DEBUG DEBUG DEBUG=1 THREADED
    )

# Link directory
link_directories(/usr/local/lib)

#test programs
add_subdirectory("${CMAKE_SOURCE_DIR}/test")
message("Open SSL Include = ${OPENSSL_INCLUDE_DIR}")
message("Open SSL Libraries = ${OPENSSL_LIBRARIES}")
message("Open SSL = ${OPENSSL_ROOT_DIR}")

add_executable(querySubmitter example/querySubmitter.cpp )

target_link_libraries(querySubmitter ${Boost_LIBRARIES} ${PROTOBUF_LIBRARY} drillClient protomsgs y2038)
