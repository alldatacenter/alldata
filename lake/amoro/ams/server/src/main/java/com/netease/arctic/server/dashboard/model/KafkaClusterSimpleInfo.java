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

package com.netease.arctic.server.dashboard.model;

import com.netease.arctic.server.dashboard.utils.CommonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.util.Objects;

public class KafkaClusterSimpleInfo {
  private String name;
  private String zkAddress;
  private String brokerList;

  public KafkaClusterSimpleInfo() {
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getZkAddress() {
    return zkAddress;
  }

  public void setZkAddress(String zkAddress) {
    this.zkAddress = zkAddress;
  }

  public String getBrokerList() {
    return brokerList;
  }

  public void setBrokerList(String brokerList) {
    this.brokerList = brokerList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KafkaClusterSimpleInfo that = (KafkaClusterSimpleInfo) o;
    return Objects.equals(name, that.name) && Objects.equals(zkAddress, that.zkAddress) &&
        Objects.equals(brokerList, that.brokerList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, zkAddress, brokerList);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("zkAddress", zkAddress)
        .add("brokerList", brokerList)
        .toString();
  }

  /**
   * validate.
   */
  public void validate() {
    Preconditions
        .checkArgument(StringUtils.isNotBlank(zkAddress) || StringUtils.isNotBlank(brokerList),
            "both zk and broker address is blank");
    if (StringUtils.isNotBlank(brokerList)) {
      Preconditions
          .checkArgument(CommonUtil.telnetOrPing(brokerList), "telnet broker address timeout! " + brokerList);
    }

  }
}
