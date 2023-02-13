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
public interface PDumpPhaseStatusOrBuilder extends // @@protoc_insertion_point(interface_extends:stream.PDumpPhaseStatus)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    int getTablesDumpCount();

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    boolean containsTablesDump(java.lang.String key);

    /**
     * Use {@link #getTablesDumpMap()} instead.
     */
    @java.lang.Deprecated
    java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDump();

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    java.util.Map<java.lang.String, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus> getTablesDumpMap();

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrDefault(java.lang.String key, com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus defaultValue);

    /**
     * <code>map&lt;string, .TableDumpStatus&gt; tablesDump = 1;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.common.TableDumpStatus getTablesDumpOrThrow(java.lang.String key);
}
