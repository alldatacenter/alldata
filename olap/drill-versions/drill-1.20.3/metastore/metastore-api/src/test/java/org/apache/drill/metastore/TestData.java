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
package org.apache.drill.metastore;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;

import java.util.Arrays;
import java.util.Collections;

public class TestData {

  /**
   * Returns table metadata unit where all fields are filled in.
   * Note: data in the fields may be not exactly true to reality.
   *
   * @return basic table metadata unit
   */
  public static TableMetadataUnit basicTableMetadataUnit() {
    return TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("test")
      .owner("user")
      .tableType("parquet")
      .metadataType(MetadataType.NONE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .location("/tmp/nation")
      .interestingColumns(Arrays.asList("`id`", "`name`"))
      .schema("{\"type\":\"tuple_schema\"," +
        "\"columns\":[{\"name\":\"id\",\"type\":\"INT\",\"mode\":\"REQUIRED\"}," +
        "{\"name\":\"name\",\"type\":\"VARCHAR\",\"mode\":\"REQUIRED\"}]," +
        "\"properties\":{\"drill.strict\":\"true\"}}\n")
      .columnsStatistics(Collections.singletonMap("`name`", "{\"statistics\":[{\"statisticsValue\":\"aaa\"," +
        "\"statisticsKind\":{\"exact\":true,\"name\":\"minValue\"}},{\"statisticsValue\":\"zzz\"," +
        "\"statisticsKind\":{\"exact\":true,\"name\":\"maxValue\"}}],\"type\":\"VARCHAR\"}"))
      .metadataStatistics(Collections.singletonList("{\"statisticsValue\":2.1," +
        "\"statisticsKind\":{\"name\":\"approx_count_distinct\"}}"))
      .lastModifiedTime(System.currentTimeMillis())
      .partitionKeys(Collections.singletonMap("dir0", "2018"))
      .additionalMetadata("additional test metadata")
      .metadataIdentifier(MetadataInfo.GENERAL_INFO_KEY)
      .column("`id`")
      .locations(Arrays.asList("/tmp/nation/1", "/tmp/nation/2"))
      .partitionValues(Arrays.asList("1", "2"))
      .path("/tmp/nation/1/0_0_0.parquet")
      .rowGroupIndex(0)
      .hostAffinity(Collections.singletonMap("host1", 0.1F))
      .build();
  }
}
