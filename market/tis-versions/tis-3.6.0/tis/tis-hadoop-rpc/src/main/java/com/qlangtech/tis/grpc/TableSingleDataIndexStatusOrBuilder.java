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
public interface TableSingleDataIndexStatusOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.TableSingleDataIndexStatus)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    int getTableConsumeDataCount();

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    boolean containsTableConsumeData(java.lang.String key);

    /**
     * Use {@link #getTableConsumeDataMap()} instead.
     */
    @java.lang.Deprecated
    java.util.Map<java.lang.String, java.lang.Long> getTableConsumeData();

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    java.util.Map<java.lang.String, java.lang.Long> getTableConsumeDataMap();

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    long getTableConsumeDataOrDefault(java.lang.String key, long defaultValue);

    /**
     * <code>map&lt;string, uint64&gt; tableConsumeData = 1;</code>
     */
    long getTableConsumeDataOrThrow(java.lang.String key);

    /**
     * <code>uint32 bufferQueueRemainingCapacity = 2;</code>
     */
    int getBufferQueueRemainingCapacity();

    /**
     * <code>uint32 bufferQueueUsedSize = 3;</code>
     */
    int getBufferQueueUsedSize();

    /**
     * <code>uint32 consumeErrorCount = 4;</code>
     */
    int getConsumeErrorCount();

    /**
     * <code>uint32 ignoreRowsCount = 5;</code>
     */
    int getIgnoreRowsCount();

    /**
     * <code>string uuid = 6;</code>
     */
    java.lang.String getUuid();

    /**
     * <code>string uuid = 6;</code>
     */
    com.google.protobuf.ByteString getUuidBytes();

    /**
     * <code>uint64 tis30sAvgRT = 7;</code>
     */
    long getTis30SAvgRT();

    /**
     * <pre>
     * 增量任务执行是否暂停
     * </pre>
     *
     * <code>bool incrProcessPaused = 8;</code>
     */
    boolean getIncrProcessPaused();
}
