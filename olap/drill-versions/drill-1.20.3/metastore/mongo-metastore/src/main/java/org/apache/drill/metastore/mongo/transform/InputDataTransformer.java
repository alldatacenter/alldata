/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.mongo.transform;

import com.google.gson.Gson;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;
import org.apache.drill.metastore.mongo.exception.MongoMetastoreException;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.bson.Document;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Converts list of Metastore component units into {@link Document}.
 *
 * @param <T> Metastore component unit type
 */
public class InputDataTransformer<T> {
  private static final Gson GSON = new Gson();
  private final List<T> units = new ArrayList<>();
  private final Map<String, MethodHandle> unitGetters;

  public InputDataTransformer(Map<String, MethodHandle> unitGetters) {
    this.unitGetters = unitGetters;
  }

  public InputDataTransformer<T> units(List<T> units) {
    this.units.addAll(units);
    return this;
  }

  public Document createId(Document document) {
    return Document.parse(GSON.toJson(ImmutableMap.of(
      MetastoreColumn.STORAGE_PLUGIN.columnName(), document.get(MetastoreColumn.STORAGE_PLUGIN.columnName()),
      MetastoreColumn.WORKSPACE.columnName(), document.get(MetastoreColumn.WORKSPACE.columnName()),
      MetastoreColumn.TABLE_NAME.columnName(), document.get(MetastoreColumn.TABLE_NAME.columnName()),
      MetastoreColumn.METADATA_TYPE.columnName(), document.get(MetastoreColumn.METADATA_TYPE.columnName()),
      MetastoreColumn.METADATA_IDENTIFIER.columnName(), document.get(MetastoreColumn.METADATA_IDENTIFIER.columnName()))));
  }

  public List<Document> execute() {
    return units.stream().map(unit -> {
      Document document = new Document();
      for (Map.Entry<String, MethodHandle> entry : unitGetters.entrySet()) {
        try {
          document.put(entry.getKey(), entry.getValue().invoke(unit));
        } catch (Throwable e) {
          throw new MongoMetastoreException(
            String.format("Unable to invoke getter for column [%s] using [%s]", entry.getKey(), entry.getValue()), e);
        }
      }
      return document.append(MongoConfigConstants.ID, createId(document));
    }).collect(Collectors.toList());
  }
}
