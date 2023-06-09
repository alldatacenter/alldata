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
public interface PExecuteStateOrBuilder extends // @@protoc_insertion_point(interface_extends:stream.PExecuteState)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
     */
    int getInfoTypeValue();

    /**
     * <code>.stream.PExecuteState.InfoType infoType = 1;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.InfoType getInfoType();

    /**
     * <code>.stream.PExecuteState.LogType logType = 2;</code>
     */
    int getLogTypeValue();

    /**
     * <code>.stream.PExecuteState.LogType logType = 2;</code>
     */
    com.qlangtech.tis.rpc.grpc.log.stream.PExecuteState.LogType getLogType();

    /**
     * <code>string msg = 3;</code>
     */
    java.lang.String getMsg();

    /**
     * <code>string msg = 3;</code>
     */
    com.google.protobuf.ByteString getMsgBytes();

    /**
     * <code>string from = 4;</code>
     */
    java.lang.String getFrom();

    /**
     * <code>string from = 4;</code>
     */
    com.google.protobuf.ByteString getFromBytes();

    /**
     * <code>uint64 jobId = 5;</code>
     */
    long getJobId();

    /**
     * <code>uint64 taskId = 6;</code>
     */
    long getTaskId();

    /**
     * <code>string serviceName = 7;</code>
     */
    java.lang.String getServiceName();

    /**
     * <code>string serviceName = 7;</code>
     */
    com.google.protobuf.ByteString getServiceNameBytes();

    /**
     * <code>string execState = 8;</code>
     */
    java.lang.String getExecState();

    /**
     * <code>string execState = 8;</code>
     */
    com.google.protobuf.ByteString getExecStateBytes();

    /**
     * <code>uint64 time = 9;</code>
     */
    long getTime();

    /**
     * <code>string component = 10;</code>
     */
    java.lang.String getComponent();

    /**
     * <code>string component = 10;</code>
     */
    com.google.protobuf.ByteString getComponentBytes();
}
