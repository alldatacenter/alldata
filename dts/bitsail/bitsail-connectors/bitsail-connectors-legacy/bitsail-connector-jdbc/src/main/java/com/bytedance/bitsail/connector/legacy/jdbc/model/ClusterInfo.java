/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.model;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

/**
 * Created 2020/11/19.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(of = {"master", "slaves", "tableNames", "shardNumber"})
public class ClusterInfo {

  private ConnectionInfo master;

  @Singular("slave")
  private List<ConnectionInfo> slaves;

  @JSONField(name = "table_names")
  private String tableNames;

  @JSONField(name = "shard_num")
  private int shardNumber;

  public void setConnectionParameters(String connectionParameters) {
    if (master != null) {
      master.setConnectionParameters(connectionParameters);
    }
    if (slaves != null) {
      for (ConnectionInfo slave : slaves) {
        slave.setConnectionParameters(connectionParameters);
      }
    }
  }

  public void addNewMaster(ConnectionInfo newMaster) {
    master = newMaster;
  }

  public void addNewSlaves(List<ConnectionInfo> newSlaves) {
    if (CollectionUtils.isEmpty(slaves)) {
      slaves = newSlaves;
    } else {
      slaves.addAll(newSlaves);
    }
  }
}
