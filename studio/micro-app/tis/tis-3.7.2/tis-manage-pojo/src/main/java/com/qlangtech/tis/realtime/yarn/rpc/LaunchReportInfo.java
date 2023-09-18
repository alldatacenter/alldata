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
package com.qlangtech.tis.realtime.yarn.rpc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 增量监听节点启动，将本地的状态发送到服务端去
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年9月28日
 */
public class LaunchReportInfo {

    // 本地节点的编号
    // private String localUUID;
    private Map<String, TopicInfo> /* collection */
    collectionFocusTopicInfo;

    public LaunchReportInfo() {
        super();
        this.collectionFocusTopicInfo = new HashMap<>();
    }

    public Map<String, /* collection */
    TopicInfo> getCollectionFocusTopicInfo() {
        return Collections.unmodifiableMap(this.collectionFocusTopicInfo);
    }

    public LaunchReportInfo(Map<String, /* collection */
    TopicInfo> collectionFocusTopicInfo) {
        super();
        // this.localUUID = localUUID;
        this.collectionFocusTopicInfo = collectionFocusTopicInfo;
        if (collectionFocusTopicInfo == null || collectionFocusTopicInfo.isEmpty()) {
            throw new IllegalArgumentException("param collectionFocusTopicInfo can not be null");
        }
    }
    // @Override
    // public void write(DataOutput out) throws IOException {
    // // 序列化
    // // WritableUtils.writeString(out, localUUID);
    // out.writeInt(collectionFocusTopicInfo.size());
    // for (Map.Entry<String, TopicInfo> /* collection */
    // entry : collectionFocusTopicInfo.entrySet()) {
    // WritableUtils.writeString(out, entry.getKey());
    // this.writeTopicInfo(out, entry.getValue());
    // }
    // }
    // private void writeTopicInfo(DataOutput out, TopicInfo info) throws IOException {
    // Map<String, Set<String>> /* tags */
    // topicWithTags = info.topicWithTags;
    // out.writeInt(topicWithTags.size());
    // for (Map.Entry<String, Set<String>> entry : topicWithTags.entrySet()) {
    // WritableUtils.writeString(out, entry.getKey());
    // out.writeInt(entry.getValue().size());
    // for (String tag : entry.getValue()) {
    // WritableUtils.writeString(out, tag);
    // }
    // }
    // }
    // @Override
    // public void readFields(DataInput in) throws IOException {
    // // 反序列化
    // // this.localUUID = WritableUtils.readString(in);
    // int collectionCount = in.readInt();
    // String collectionName = null;
    // int tagCount = 0;
    // int topicCount = 0;
    // TopicInfo topicInfo = null;
    // String topicName = null;
    // for (int i = 0; i < collectionCount; i++) {
    // collectionName = WritableUtils.readString(in);
    // topicInfo = new TopicInfo();
    // collectionFocusTopicInfo.put(collectionName, topicInfo);
    // topicCount = in.readInt();
    // for (int t = 0; t < topicCount; t++) {
    // topicName = WritableUtils.readString(in);
    // tagCount = in.readInt();
    // for (int tagIndex = 0; tagIndex < tagCount; tagIndex++) {
    // topicInfo.addTag(topicName, WritableUtils.readString(in));
    // }
    // }
    // }
    // }
}
