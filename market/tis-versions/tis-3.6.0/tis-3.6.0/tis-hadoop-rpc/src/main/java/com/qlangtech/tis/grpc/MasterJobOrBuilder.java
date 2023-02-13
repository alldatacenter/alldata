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
package com.qlangtech.tis.grpc;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/09/25
 */
public interface MasterJobOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.MasterJob)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>.rpc.MasterJob.JobType jobType = 1;</code>
     */
    int getJobTypeValue();

    /**
     * <code>.rpc.MasterJob.JobType jobType = 1;</code>
     */
    com.qlangtech.tis.grpc.MasterJob.JobType getJobType();

    /**
     * <code>bool stop = 2;</code>
     */
    boolean getStop();

    /**
     * <code>string indexName = 3;</code>
     */
    java.lang.String getIndexName();

    /**
     * <code>string indexName = 3;</code>
     */
    com.google.protobuf.ByteString getIndexNameBytes();

    /**
     * <code>string uuid = 4;</code>
     */
    java.lang.String getUuid();

    /**
     * <code>string uuid = 4;</code>
     */
    com.google.protobuf.ByteString getUuidBytes();

    /**
     * <code>uint64 createTime = 5;</code>
     */
    long getCreateTime();
}
