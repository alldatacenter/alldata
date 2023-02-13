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
public interface LaunchReportInfoEntryOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.LaunchReportInfoEntry)
com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * topic
     * </pre>
     *
     * <code>string topicName = 1;</code>
     */
    java.lang.String getTopicName();

    /**
     * <pre>
     * topic
     * </pre>
     *
     * <code>string topicName = 1;</code>
     */
    com.google.protobuf.ByteString getTopicNameBytes();

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    java.util.List<java.lang.String> getTagNameList();

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    int getTagNameCount();

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    java.lang.String getTagName(int index);

    /**
     * <pre>
     * tags
     * </pre>
     *
     * <code>repeated string tagName = 2;</code>
     */
    com.google.protobuf.ByteString getTagNameBytes(int index);
}
