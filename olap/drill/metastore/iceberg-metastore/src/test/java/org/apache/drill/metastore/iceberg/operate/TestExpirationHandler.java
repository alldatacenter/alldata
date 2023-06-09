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
package org.apache.drill.metastore.iceberg.operate;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.drill.metastore.iceberg.IcebergMetastore;
import org.apache.drill.metastore.iceberg.components.tables.IcebergTables;
import org.apache.drill.metastore.iceberg.config.IcebergConfigConstants;
import org.apache.iceberg.TableProperties;
import org.junit.Test;

import java.io.File;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class TestExpirationHandler extends IcebergBaseTest {

  @Test
  public void testNoExpiration() {
    IcebergTables tables = tables("no-expiration", false, 2);

    // check that there is no history
    assertEquals(0, tables.table().history().size());

    int operationsNumber = 5;
    execute(tables, operationsNumber);

    // check that the number of executed operations is same as number of history records
    assertEquals(operationsNumber, tables.table().history().size());
  }

  @Test
  public void testExpiration() {
    int retainNumber = 3;
    IcebergTables tables = tables("expiration", true, retainNumber);

    // check that there is no history
    assertEquals(0, tables.table().history().size());

    execute(tables, 5);

    // check that number of history records corresponds to the expected retain number
    assertEquals(retainNumber, tables.table().history().size());
  }

  @Test
  public void testSubsequentExpiration() {
    String name = "subsequent-expiration";
    int retainNumber = 2;
    int operationsNumber = 5;

    IcebergTables initialTables = tables(name, false, retainNumber);

    execute(initialTables, operationsNumber);

    // check that number of executed operations is the same as number of history records
    assertEquals(operationsNumber, initialTables.table().history().size());

    // update table configuration, allow expiration
    IcebergTables updatedTables = tables(name, true, retainNumber);

    // check that number of history operation did not change
    assertEquals(operationsNumber, updatedTables.table().history().size());

    execute(updatedTables, operationsNumber);

    // check that number of history records corresponds to the expected retain number
    assertEquals(retainNumber, updatedTables.table().history().size());
  }

  private IcebergTables tables(String name, boolean shouldExpire, int retainNumber) {
    Config config = baseIcebergConfig(new File(defaultFolder.getRoot(), name))
      .withValue(IcebergConfigConstants.COMPONENTS_COMMON_PROPERTIES + "." + TableProperties.METADATA_PREVIOUS_VERSIONS_MAX,
        ConfigValueFactory.fromAnyRef(retainNumber))
      .withValue(IcebergConfigConstants.COMPONENTS_COMMON_PROPERTIES + "." + TableProperties.METADATA_DELETE_AFTER_COMMIT_ENABLED,
        ConfigValueFactory.fromAnyRef(shouldExpire));
    DrillConfig drillConfig = new DrillConfig(config);
    return (IcebergTables) new IcebergMetastore(drillConfig).tables();
  }

  private void execute(Tables tables, int operationsNumber) {
    IntStream.range(0, operationsNumber)
      .mapToObj(i -> TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey("dir" + i)
        .build())
      .forEach(table -> tables.modify()
        .overwrite(table)
        .execute());
  }
}
