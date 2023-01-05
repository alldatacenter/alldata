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
public interface LaunchReportInfoOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.LaunchReportInfo)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    int getCollectionFocusTopicInfoCount();

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    boolean containsCollectionFocusTopicInfo(java.lang.String key);

    /**
     * Use {@link #getCollectionFocusTopicInfoMap()} instead.
     */
    @java.lang.Deprecated
    java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfo();

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    java.util.Map<java.lang.String, com.qlangtech.tis.grpc.TopicInfo> getCollectionFocusTopicInfoMap();

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrDefault(java.lang.String key, com.qlangtech.tis.grpc.TopicInfo defaultValue);

    /**
     * <code>map&lt;string, .rpc.TopicInfo&gt; collectionFocusTopicInfo = 1;</code>
     */
    com.qlangtech.tis.grpc.TopicInfo getCollectionFocusTopicInfoOrThrow(java.lang.String key);
}
