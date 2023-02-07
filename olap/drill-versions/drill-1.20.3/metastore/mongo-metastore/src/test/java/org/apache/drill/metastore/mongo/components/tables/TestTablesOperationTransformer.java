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
package org.apache.drill.metastore.mongo.components.tables;

import com.mongodb.client.model.Filters;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.mongo.MongoMetastore;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;
import org.apache.drill.metastore.mongo.operate.MongoDelete;
import org.apache.drill.metastore.mongo.operate.Overwrite;
import org.apache.drill.metastore.mongo.transform.InputDataTransformer;
import org.apache.drill.metastore.mongo.transform.OperationTransformer;
import org.apache.drill.metastore.operate.Delete;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestTablesOperationTransformer {

  private static OperationTransformer<TableMetadataUnit> transformer;
  private static MongoMetastore metastore;

  @BeforeClass
  public static void init() {
    DrillConfig drillConfig = new DrillConfig(DrillConfig.create().withValue(MongoConfigConstants.CONNECTION,
      ConfigValueFactory.fromAnyRef("mongodb://localhost:27017/?connectTimeoutMS=60000&maxPoolSize=1000&safe=true")));
    metastore = new MongoMetastore(drillConfig);
    transformer = new TablesOperationTransformer(((MongoTables) metastore.tables()).context());
  }

  @Test
  public void testToOverwriteOperation() {
    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin("dfs").workspace("tmp").tableName("nation")
      .metadataType(MetadataType.TABLE.name()).metadataIdentifier("s1").build();
    List<Overwrite> operations = transformer.toOverwrite(Collections.singletonList(unit));
    InputDataTransformer<TableMetadataUnit> inputDataTransformer =
      ((MongoTables) metastore.tables()).transformer().inputData();
    Document expected = new Document();
    expected.append("storagePlugin", "dfs");
    expected.append("workspace", "tmp");
    expected.append("tableName", "nation");
    expected.append("metadataType", MetadataType.TABLE.name());
    expected.append("metadataIdentifier", "s1");

    assertEquals(new Document()
        .append(MongoConfigConstants.ID, inputDataTransformer.createId(expected)),
      operations.get(0).filter());
    assertEquals(expected, operations.get(0).data().get(MongoConfigConstants.ID));
  }

  @Test
  public void testToOverwriteOperations() {
    List<TableMetadataUnit> units = Arrays.asList(
      TableMetadataUnit.builder().storagePlugin("dfs").workspace("tmp")
        .tableName("nation").metadataType(MetadataType.ROW_GROUP.name())
        .metadataIdentifier("s1/nation2.parquet/0").build(),
      TableMetadataUnit.builder().storagePlugin("dfs").workspace("tmp")
        .tableName("nation").metadataType(MetadataType.ROW_GROUP.name())
        .metadataIdentifier("s1/nation.parquet/0").build(),
      TableMetadataUnit.builder().storagePlugin("dfs").workspace("tmp")
        .tableName("nation").metadataType(MetadataType.FILE.name())
        .metadataIdentifier("s1/nation2.parquet").build(),
      TableMetadataUnit.builder().storagePlugin("dfs").workspace("tmp")
        .tableName("nation").metadataType(MetadataType.FILE.name())
        .metadataIdentifier("s1/nation.parquet").build(),
      TableMetadataUnit.builder().storagePlugin("dfs").workspace("tmp")
        .tableName("region").metadataType(MetadataType.SEGMENT.name())
        .metadataIdentifier("s1").build(),
      TableMetadataUnit.builder().storagePlugin("s3").workspace("tmp")
        .tableName("region").metadataType(MetadataType.TABLE.name())
        .metadataIdentifier("GENERAL_INFO").build());

    List<Overwrite> operations = transformer.toOverwrite(units);
    assertEquals(6, operations.size());
  }

  @Test
  public void testToDeleteOperation() {
    Bson expected = Filters.and(
      Filters.eq(MetastoreColumn.STORAGE_PLUGIN.columnName(), "dfs"),
      Filters.eq(MetastoreColumn.WORKSPACE.columnName(), "tmp"));
    FilterExpression filter = FilterExpression.and(
      FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
      FilterExpression.equal(MetastoreColumn.WORKSPACE, "tmp"));

    Delete delete = Delete.builder()
      .metadataType(MetadataType.ALL)
      .filter(filter)
      .build();

    MongoDelete operation = transformer.toDelete(delete);
    assertEquals(expected.toString(), operation.filter().toString());
  }
}
