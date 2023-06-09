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

package com.netease.arctic.table;

import org.apache.commons.lang.StringUtils;

public enum DistributionHashMode {
  NONE("none", false, false),
  PRIMARY_KEY("primary-key", true, false),
  PARTITION_KEY("partition-key", false, true),
  PRIMARY_PARTITION_KEY("primary-partition-key", true, true),
  AUTO("auto", true, true);
  private final String desc;
  private final boolean supportPrimaryKey;
  private final boolean supportPartition;

  DistributionHashMode(String desc, boolean supportPrimaryKey, boolean supportPartition) {
    this.desc = desc;
    this.supportPrimaryKey = supportPrimaryKey;
    this.supportPartition = supportPartition;
  }

  public String getDesc() {
    return desc;
  }

  /**
   * If shuffled by primary key supported.
   *
   * @return true/false
   */
  public boolean isSupportPrimaryKey() {
    return supportPrimaryKey;
  }

  /**
   * If shuffled by partition supported.
   *
   * @return true/false
   */
  public boolean isSupportPartition() {
    return supportPartition;
  }

  /**
   * If primary key needed.
   * @return true/false
   */
  public boolean mustByPrimaryKey() {
    return supportPrimaryKey && strict();
  }

  /**
   * If partition needed.
   * @return true/false
   */
  public boolean mustByPartition() {
    return supportPartition && strict();
  }
  
  private boolean strict() {
    return this != AUTO;
  }

  /**
   * Get ShufflePolicyType from desc ignore case.
   *
   * @param desc - desc of ShufflePolicyType
   * @return ShufflePolicyType
   */
  public static DistributionHashMode valueOfDesc(String desc) {
    for (DistributionHashMode value : DistributionHashMode.values()) {
      if (StringUtils.equalsIgnoreCase(value.getDesc(), desc)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown ShufflePolicyType " + desc);
  }

  /**
   * auto select ShufflePolicyType.
   * @param primaryKeyExist -
   * @param partitionExist -
   * @return ShufflePolicyType
   */
  public static DistributionHashMode autoSelect(boolean primaryKeyExist, boolean partitionExist) {
    if (primaryKeyExist) {
      if (partitionExist) {
        return PRIMARY_PARTITION_KEY;
      } else {
        return PRIMARY_KEY;
      }
    } else {
      if (partitionExist) {
        return PARTITION_KEY;
      } else {
        return NONE;
      }
    }
  }
}
