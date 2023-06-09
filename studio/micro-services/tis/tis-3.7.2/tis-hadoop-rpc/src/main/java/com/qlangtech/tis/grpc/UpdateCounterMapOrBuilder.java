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
public interface UpdateCounterMapOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.UpdateCounterMap)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    int getDataCount();

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    boolean containsData(java.lang.String key);

    /**
     * Use {@link #getDataMap()} instead.
     */
    @java.lang.Deprecated
    java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getData();

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TableSingleDataIndexStatus> getDataMap();

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TableSingleDataIndexStatus defaultValue);

    /**
     * <code>map&lt;string, .rpc.TableSingleDataIndexStatus&gt; data = 1;</code>
     */
    com.qlangtech.tis.grpc.TableSingleDataIndexStatus getDataOrThrow(java.lang.String key);

    /**
     * <code>uint64 gcCounter = 2;</code>
     */
    long getGcCounter();

    /**
     * <pre>
     * 从哪个地址发送过来的
     * </pre>
     *
     * <code>string from = 3;</code>
     */
    java.lang.String getFrom();

    /**
     * <pre>
     * 从哪个地址发送过来的
     * </pre>
     *
     * <code>string from = 3;</code>
     */
    com.google.protobuf.ByteString getFromBytes();

    /**
     * <code>uint64 updateTime = 4;</code>
     */
    long getUpdateTime();
}
