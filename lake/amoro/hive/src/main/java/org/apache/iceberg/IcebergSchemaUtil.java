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

package org.apache.iceberg;

public class IcebergSchemaUtil {

  /**
   * Copy an new partition spec depend on an new schema, new schema should contain the same fields partition spec need.
   *
   * @param partitionSpec partition spec to be copied
   * @param copySchema schema new partition spec depend on
   * @return the new partition spec
   */
  public static PartitionSpec copyPartitionSpec(PartitionSpec partitionSpec, Schema copySchema) {
    PartitionSpec.Builder builder = PartitionSpec.builderFor(copySchema);
    partitionSpec.fields().forEach(partitionField -> {
      builder.add(partitionField.sourceId(), partitionField.name(), partitionField.transform());
    });
    return builder.build();
  }

  /**
   * Copy an new sort order spec depend on an new schema, new schema should contain the same fields sort order spec
   * need.
   *
   * @param sortOrder sort order spec to be copied
   * @param copySchema schema new partition spec depend on
   * @return the new partition spec
   */
  public static SortOrder copySortOrderSpec(SortOrder sortOrder, Schema copySchema) {
    SortOrder.Builder builder = SortOrder.builderFor(copySchema);
    sortOrder.fields().forEach(sortField -> {
      builder.addSortField(
          sortField.transform(),
          sortField.sourceId(),
          sortField.direction(),
          sortField.nullOrder());
    });
    return builder.build();
  }

  public static PartitionSpec projectPartition(PartitionSpec partitionSpec, Schema schema) {
    PartitionSpec.Builder builder = PartitionSpec.builderFor(schema);

    partitionSpec.fields().forEach(p -> {
      if (schema.findField(p.sourceId()) == null) {
        return;
      }

      builder.add(p.sourceId(), p.name(), p.transform());
    });
    return builder.build();
  }
}
