/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.rpc.grpc.log.stream;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public interface PPhaseStatusCollectionOrBuilder extends // @@protoc_insertion_point(interface_extends:stream.PPhaseStatusCollection)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    boolean hasDumpPhase();

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatus getDumpPhase();

    /**
     * <code>.stream.PDumpPhaseStatus dumpPhase = 1;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PDumpPhaseStatusOrBuilder getDumpPhaseOrBuilder();

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    boolean hasJoinPhase();

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatus getJoinPhase();

    /**
     * <code>.stream.PJoinPhaseStatus joinPhase = 2;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PJoinPhaseStatusOrBuilder getJoinPhaseOrBuilder();

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    boolean hasBuildPhase();

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatus getBuildPhase();

    /**
     * <code>.stream.PBuildPhaseStatus buildPhase = 3;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PBuildPhaseStatusOrBuilder getBuildPhaseOrBuilder();

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    boolean hasIndexBackFlowPhaseStatus();

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatus getIndexBackFlowPhaseStatus();

    /**
     * <code>.stream.PIndexBackFlowPhaseStatus indexBackFlowPhaseStatus = 4;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PIndexBackFlowPhaseStatusOrBuilder getIndexBackFlowPhaseStatusOrBuilder();

    /**
     * <code>uint32 taskId = 5;</code>
     */
    int getTaskId();
}
