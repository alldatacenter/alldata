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
public interface NodeBackflowStatusOrBuilder extends // @@protoc_insertion_point(interface_extends:NodeBackflowStatus)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string nodeName = 1;</code>
     */
    java.lang.String getNodeName();

    /**
     * <code>string nodeName = 1;</code>
     */
    com.google.protobuf.ByteString getNodeNameBytes();

    /**
     * <code>uint64 allSize = 2;</code>
     */
    long getAllSize();

    /**
     * <code>uint64 readed = 3;</code>
     */
    long getReaded();

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
