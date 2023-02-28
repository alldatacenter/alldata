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
public interface TableDumpStatusOrBuilder extends // @@protoc_insertion_point(interface_extends:TableDumpStatus)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>string tableName = 1;</code>
     */
    java.lang.String getTableName();

    /**
     * <code>string tableName = 1;</code>
     */
    com.google.protobuf.ByteString getTableNameBytes();

    /**
     * <code>uint32 taskid = 2;</code>
     */
    int getTaskid();

    /**
     * <pre>
     * 全部的记录数
     * </pre>
     *
     * <code>uint32 allRows = 3;</code>
     */
    int getAllRows();

    /**
     * <pre>
     * 已经读取的记录数
     * </pre>
     *
     * <code>uint32 readRows = 4;</code>
     */
    int getReadRows();

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
