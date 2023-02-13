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
package com.qlangtech.tis.rpc.grpc.log.common;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public interface JoinTaskStatusOrBuilder extends // @@protoc_insertion_point(interface_extends:JoinTaskStatus)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string joinTaskName = 1;</code>
     */
    java.lang.String getJoinTaskName();

    /**
     * <code>string joinTaskName = 1;</code>
     */
    com.google.protobuf.ByteString getJoinTaskNameBytes();

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    int getJobStatusCount();

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    boolean containsJobStatus(int key);

    /**
     * Use {@link #getJobStatusMap()} instead.
     */
    @java.lang.Deprecated
    java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatus();

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    java.util.Map<java.lang.Integer, com.qlangtech.tis.rpc.grpc.log.common.JobLog> getJobStatusMap();

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrDefault(int key, com.qlangtech.tis.rpc.grpc.log.common.JobLog defaultValue);

    /**
     * <code>map&lt;uint32, .JobLog&gt; jobStatus = 2;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.common.JobLog getJobStatusOrThrow(int key);

    /**
     * <code>bool faild = 5;</code>
     */
    boolean getFaild();

    /**
     * <code>bool complete = 6;</code>
     */
    boolean getComplete();

    /**
     * <code>bool waiting = 7;</code>
     */
    boolean getWaiting();
}
