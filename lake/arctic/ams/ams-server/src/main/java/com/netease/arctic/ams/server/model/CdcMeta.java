/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.model;


import java.util.Objects;

public class CdcMeta {
  private boolean enable;
  private KafkaClusterSimpleInfo kafkaClusterInfo;
  private TopicInfo topicInfo;

  public CdcMeta() {
  }

  public boolean isEnable() {
    return enable;
  }

  public void setEnable(boolean enable) {
    this.enable = enable;
  }

  public KafkaClusterSimpleInfo getKafkaClusterInfo() {
    return kafkaClusterInfo;
  }

  public void setKafkaClusterInfo(KafkaClusterSimpleInfo kafkaClusterInfo) {
    this.kafkaClusterInfo = kafkaClusterInfo;
  }

  public TopicInfo getTopicInfo() {
    return topicInfo;
  }

  public void setTopicInfo(TopicInfo topicInfo) {
    this.topicInfo = topicInfo;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CdcMeta cdcMeta = (CdcMeta) o;

    return enable == cdcMeta.enable && Objects.equals(kafkaClusterInfo, cdcMeta.kafkaClusterInfo) &&
        Objects.equals(topicInfo, cdcMeta.topicInfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enable, kafkaClusterInfo, topicInfo);
  }

  @Override
  public String toString() {
    return "CdcMeta{" +
        "enable=" + enable +
        ", kafkaClusterInfo=" + kafkaClusterInfo +
        ", topicInfo=" + topicInfo +
        '}';
  }

  /**
   * validate.
   */
  public void validate() {
    if (enable) {
      topicInfo.validate();
      kafkaClusterInfo.validate();
    }
  }
}
