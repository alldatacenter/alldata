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

# Find Protobufs
find_package(Protobuf ${PROTOBUF_MINIMUM_VERSION} REQUIRED)
include_directories(${PROTOBUF_INCLUDE_DIR})

#Generate Protobuf code

set (PROTO_SRC_FILES
    ${CMAKE_BINARY_DIR}/protobuf/BitControl.proto
    ${CMAKE_BINARY_DIR}/protobuf/Coordination.proto
    ${CMAKE_BINARY_DIR}/protobuf/GeneralRPC.proto
    ${CMAKE_BINARY_DIR}/protobuf/Types.proto
    ${CMAKE_BINARY_DIR}/protobuf/UserBitShared.proto
    ${CMAKE_BINARY_DIR}/protobuf/BitData.proto
    ${CMAKE_BINARY_DIR}/protobuf/ExecutionProtos.proto
    ${CMAKE_BINARY_DIR}/protobuf/SchemaDef.proto
    ${CMAKE_BINARY_DIR}/protobuf/User.proto
    )

PROTOBUF_GENERATE_CPP(GenProtoSources GenProtoHeaders ${PROTO_SRC_FILES})

set (PROTO_CPPSRC_FILES
    ${CMAKE_CURRENT_SOURCE_DIR}/BitControl.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/Coordination.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/GeneralRPC.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/Types.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/UserBitShared.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/BitData.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/ExecutionProtos.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/SchemaDef.pb.cc
    ${CMAKE_CURRENT_SOURCE_DIR}/User.pb.cc
    )

set (PROTO_HDR_DIR ${CMAKE_CURRENT_SOURCE_DIR})

set (PROTO_CPPHDR_FILES
    ${PROTO_HDR_DIR}/BitControl.pb.h
    ${PROTO_HDR_DIR}/Coordination.pb.h
    ${PROTO_HDR_DIR}/GeneralRPC.pb.h
    ${PROTO_HDR_DIR}/UserBitShared.pb.h
    ${PROTO_HDR_DIR}/BitData.pb.h
    ${PROTO_HDR_DIR}/ExecutionProtos.pb.h
    ${PROTO_HDR_DIR}/SchemaDef.pb.h
    ${PROTO_HDR_DIR}/User.pb.h
    )

set (PROTO_INC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../include/drill/protobuf)

set (PROTO_CPPINC_FILES
    ${PROTO_INC_DIR}/Types.pb.h
    )

set(ProtoIncludes ${PROTO_CPPINC_FILES} )
set(ProtoSources ${PROTO_CPPSRC_FILES} )
set(ProtoHeaders ${PROTO_CPPHDR_FILES})
include_directories(${PROTO_INC_DIR})
include_directories(${PROTO_HDR_DIR})

set(ProtoHeadersDir ${PROTO_HDR_DIR} PARENT_SCOPE)
set(ProtoIncludesDir ${PROTO_INC_DIR} PARENT_SCOPE)

#
#   TARGETS
#

#Preprocess to fix protobuf .proto definitions
SET (SRCDIR ${PROJECT_SOURCE_DIR}/../../../protocol/src/main/protobuf )
SET (TARGDIR ${CMAKE_BINARY_DIR}/protobuf)
SET (FNAME Types.proto)
SET (CPPSRCDIR ${CMAKE_BINARY_DIR}/src/protobuf)
SET (CPPDESTDIR ${PROJECT_SOURCE_DIR}/src/protobuf)
SET (CPPINCDIR ${PROJECT_SOURCE_DIR}/src/include/drill/protobuf)

add_custom_target(fixProtobufs
    COMMAND ${PROJECT_SOURCE_DIR}/scripts/fixProtodefs.sh ${SRCDIR} ${TARGDIR} ${FNAME}
    )

add_custom_target(genProtobufs DEPENDS ${GenProtoSources} fixProtobufs)

# copy protobuf CPP files to destinations in the source tree
add_custom_target(cpProtobufs
    COMMAND ${PROJECT_SOURCE_DIR}/scripts/cpProtofiles.sh ${CPPSRCDIR} ${CPPDESTDIR} ${CPPINCDIR}
    )

#message("ProtoSources =  ${ProtoSources}" )
#message("ProtoHeaders =  ${ProtoHeaders}" )
#message("ProtoIncludes =  ${ProtoIncludes}" )

add_library(protomsgs STATIC ${ProtoSources} ${ProtoHeaders} ${ProtoIncludes} )
#set linker properties. The first time around, the protobufs generated files may not exist
# and CMAKE will not be able to determine the linker type.
SET_TARGET_PROPERTIES(protomsgs PROPERTIES LINKER_LANGUAGE C)

add_dependencies(cpProtobufs genProtobufs)

