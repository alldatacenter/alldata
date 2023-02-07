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
package org.apache.drill.exec.store.mapr.db;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.physical.impl.join.RowKeyJoin;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.StoragePluginRegistry;

/**
 * A RestrictedMapRDBSubScan is intended for skip-scan (as opposed to sequential scan) operations
 * where the set of rowkeys is obtained from a corresponding RowKeyJoin instance
*/
@JsonTypeName("maprdb-restricted-subscan")
public class RestrictedMapRDBSubScan extends MapRDBSubScan {

  @JsonCreator
  public RestrictedMapRDBSubScan(@JacksonInject StoragePluginRegistry engineRegistry,
                                 @JsonProperty("userName") String userName,
                                 @JsonProperty("formatPluginConfig") MapRDBFormatPluginConfig formatPluginConfig,
                                 @JsonProperty("storageConfig") StoragePluginConfig storageConfig,
                                 @JsonProperty("regionScanSpecList") List<RestrictedMapRDBSubScanSpec> regionScanSpecList,
                                 @JsonProperty("columns") List<SchemaPath> columns,
                                 @JsonProperty("maxRecordsToRead") int maxRecordsToRead,
                                 @JsonProperty("tableType") String tableType,
                                 @JsonProperty("schema") TupleMetadata schema) throws ExecutionSetupException {
    this(userName,
        engineRegistry.resolveFormat(storageConfig, formatPluginConfig, MapRDBFormatPlugin.class),
        regionScanSpecList, columns, maxRecordsToRead, tableType, schema);
  }

  public RestrictedMapRDBSubScan(String userName, MapRDBFormatPlugin formatPlugin,
      List<RestrictedMapRDBSubScanSpec> maprDbSubScanSpecs,
      List<SchemaPath> columns, int maxRecordsToRead, String tableType, TupleMetadata schema) {
    super(userName, formatPlugin, new ArrayList<>(), columns, maxRecordsToRead, tableType, schema);

    for(RestrictedMapRDBSubScanSpec restrictedSpec : maprDbSubScanSpecs) {
      getRegionScanSpecList().add(restrictedSpec);
    }

  }

  @Override
  public void addJoinForRestrictedSubScan(RowKeyJoin rjbatch) {
    // currently, all subscan specs are sharing the same join batch instance
    for (MapRDBSubScanSpec s : getRegionScanSpecList()) {
      assert (s instanceof RestrictedMapRDBSubScanSpec);
      ((RestrictedMapRDBSubScanSpec)s).setJoinForSubScan(rjbatch);
    }
  }

  @Override
  public boolean isRestrictedSubScan() {
    return true;
  }

}
