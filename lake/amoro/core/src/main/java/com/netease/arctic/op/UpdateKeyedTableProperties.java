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

import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.TableProperties;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;

public class UpdateKeyedTableProperties implements UpdateProperties {

  private final Map<String, String> propsSet = Maps.newHashMap();
  private final Set<String> propsDel = Sets.newHashSet();
  private final KeyedTable keyedTable;
  private final TableMeta meta;

  public UpdateKeyedTableProperties(KeyedTable keyedTable, TableMeta meta) {
    this.keyedTable = keyedTable;
    this.meta = meta;
  }

  @Override
  public UpdateProperties set(String key, String value) {
    propsSet.put(key, value);
    return this;
  }

  @Override
  public UpdateProperties remove(String key) {
    propsDel.add(key);
    return this;
  }

  @Override
  public UpdateProperties defaultFormat(FileFormat format) {
    set(TableProperties.DEFAULT_FILE_FORMAT, format.name());
    return this;
  }

  @Override
  public Map<String, String> apply() {
    Map<String, String> newProperties = Maps.newHashMap();
    for (Map.Entry<String, String> entry : keyedTable.properties().entrySet()) {
      if (!propsDel.contains(entry.getKey())) {
        newProperties.put(entry.getKey(), entry.getValue());
      }
    }
    newProperties.putAll(propsSet);
    return newProperties;
  }

  @Override
  public void commit() {
    Map<String, String> props = apply();
    commitIcebergTableProperties(keyedTable.baseTable());
    if (keyedTable.changeTable() != null) {
      commitIcebergTableProperties(keyedTable.changeTable());
    }
    this.meta.setProperties(props);
  }

  protected void commitIcebergTableProperties(UnkeyedTable unkeyedTable) {
    UpdateProperties updateProperties = unkeyedTable.updateProperties();
    propsSet.forEach(updateProperties::set);
    propsDel.forEach(updateProperties::remove);
    updateProperties.commit();
  }
}
