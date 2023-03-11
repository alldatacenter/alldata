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

package com.netease.arctic.op;

import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.StructLikeMap;

import java.util.Map;
import java.util.Set;

public class PartitionPropertiesUpdate implements UpdatePartitionProperties {

  private final UnkeyedTable table;
  private final Table transactionTable;
  private final StructLikeMap<Map<String, String>> setProperties;
  private final StructLikeMap<Set<String>> removeProperties;
  private final Transaction transaction;

  public PartitionPropertiesUpdate(UnkeyedTable table, Transaction transaction) {
    this.table = table;
    this.setProperties = StructLikeMap.create(table.spec().partitionType());
    this.removeProperties = StructLikeMap.create(table.spec().partitionType());
    this.transaction = transaction;
    if (transaction != null) {
      transactionTable = transaction.table();
    } else {
      transactionTable = null;
    }
  }

  @Override
  public PartitionPropertiesUpdate set(
      StructLike partitionData, String key, String value) {
    Preconditions.checkNotNull(partitionData, "partition cannot be null");
    Preconditions.checkNotNull(key, "Key cannot be null");
    Preconditions.checkNotNull(key, "Value cannot be null");
    Set<String> removePropertiesForPartition = removeProperties.get(partitionData);
    Preconditions.checkArgument(removePropertiesForPartition == null ||
            !removePropertiesForPartition.contains(key),
        "Cannot remove and update the same key: %s", key);
    Map<String, String> properties = setProperties.computeIfAbsent(partitionData, k -> Maps.newHashMap());
    properties.put(key, value);
    return this;
  }

  @Override
  public PartitionPropertiesUpdate remove(StructLike partitionData, String key) {
    Preconditions.checkNotNull(partitionData, "partition cannot be null");
    Preconditions.checkNotNull(key, "Key cannot be null");
    Map<String, String> setPropertiesForPartition = setProperties.get(partitionData);
    Preconditions.checkArgument(setPropertiesForPartition == null ||
            !setPropertiesForPartition.containsKey(key),
        "Cannot remove and update the same key: %s", key);
    Set<String> properties = removeProperties.computeIfAbsent(partitionData, k -> Sets.newHashSet());
    properties.add(key);
    return this;
  }

  @Override
  public StructLikeMap<Map<String, String>> apply() {
    StructLikeMap<Map<String, String>> partitionProperties;
    if (transactionTable != null) {
      String s = transactionTable.properties().get(TableProperties.TABLE_PARTITION_PROPERTIES);
      if (s != null) {
        partitionProperties = TablePropertyUtil.decodePartitionProperties(table.spec(), s);
      } else {
        partitionProperties = StructLikeMap.create(table.spec().partitionType());
      }
    } else {
      partitionProperties = table.partitionProperty();
    }
    setProperties.forEach((partitionData, properties) -> {
      Map<String, String> oldProperties = partitionProperties.computeIfAbsent(partitionData, k -> Maps.newHashMap());
      oldProperties.putAll(properties);
    });
    removeProperties.forEach((partitionData, keys) -> {
      Map<String, String> oldProperties = partitionProperties.get(partitionData);
      if (oldProperties != null) {
        keys.forEach(oldProperties::remove);
      }
    });
    return partitionProperties;
  }

  @Override
  public void commit() {
    StructLikeMap<Map<String, String>> result = apply();
    UpdateProperties updateProperties;
    if (transaction == null) {
      updateProperties = table.updateProperties();
    } else {
      updateProperties = transaction.updateProperties();
    }
    updateProperties.set(
        TableProperties.TABLE_PARTITION_PROPERTIES,
        TablePropertyUtil.encodePartitionProperties(table.spec(), result));
    updateProperties.commit();
  }
}
