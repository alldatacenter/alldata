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
package org.apache.drill.metastore.iceberg.components.tables;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.metastore.iceberg.transform.OutputDataTransformer;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Metastore Tables component output data transformer that transforms
 * {@link org.apache.iceberg.data.Record} into {@link TableMetadataUnit}.
 */
public class TablesOutputDataTransformer extends OutputDataTransformer<TableMetadataUnit> {

  public TablesOutputDataTransformer(Map<String, MethodHandle> unitSetters) {
    super(unitSetters);
  }

  @Override
  public List<TableMetadataUnit> execute() {
    List<TableMetadataUnit> results = new ArrayList<>();
    for (Map<MethodHandle, Object> valueToSet : valuesToSet()) {
      TableMetadataUnit.Builder builder = TableMetadataUnit.builder();
      for (Map.Entry<MethodHandle, Object> entry : valueToSet.entrySet()) {
        try {
          entry.getKey().invokeWithArguments(builder, entry.getValue());
        } catch (Throwable e) {
          throw new IcebergMetastoreException(
            String.format("Unable to invoke setter for [%s] using [%s]",
              TableMetadataUnit.Builder.class.getSimpleName(), entry.getKey()), e);
        }
      }
      results.add(builder.build());
    }
    return results;
  }
}
