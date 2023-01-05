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
public interface TopicInfoOrBuilder extends // @@protoc_insertion_point(interface_extends:rpc.TopicInfo)
com.google.protobuf.MessageOrBuilder {

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    java.util.List<com.qlangtech.tis.grpc.LaunchReportInfoEntry> getTopicWithTagsList();

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    com.qlangtech.tis.grpc.LaunchReportInfoEntry getTopicWithTags(int index);

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    int getTopicWithTagsCount();

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    java.util.List<? extends com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder> getTopicWithTagsOrBuilderList();

    /**
     * <code>repeated .rpc.LaunchReportInfoEntry topicWithTags = 1;</code>
     */
    com.qlangtech.tis.grpc.LaunchReportInfoEntryOrBuilder getTopicWithTagsOrBuilder(int index);
}
