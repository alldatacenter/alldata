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

package org.apache.drill.exec.store.esri;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;

import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetComparison;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

@Category(RowSetTests.class)
public class TestShapefileFormatPlugin extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    dirTestWatcher.copyResourceToRoot(Paths.get("shapefiles/"));
  }

  @Test
  public void testRowCount() throws Exception {
    testBuilder()
      .sqlQuery("SELECT count(*) FROM dfs.`shapefiles/CA-cities.shp`")
      .ordered()
      .baselineColumns("EXPR$0")
      .baselineValues(5727L)
      .go();
  }

  @Test
  public void testShpQuery() throws Exception {
    String sql = "SELECT gid, srid, shapeType, name, st_astext(geom) as wkt FROM dfs.`shapefiles/CA-cities.shp` where gid = 100";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("gid", TypeProtos.MinorType.INT)
      .addNullable("srid", TypeProtos.MinorType.INT)
      .addNullable("shapeType", TypeProtos.MinorType.VARCHAR)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .addNullable("wkt", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(100, 4326, "Point", "Jenny Lind", "POINT (-120.8699371 38.0949216)")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testRegularQuery() throws Exception {
    String sql = "SELECT gid, srid, shapeType, name FROM dfs.`shapefiles/CA-cities.shp` where gid = 100";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("gid", TypeProtos.MinorType.INT)
      .addNullable("srid", TypeProtos.MinorType.INT)
      .addNullable("shapeType", TypeProtos.MinorType.VARCHAR)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(100, 4326, "Point", "Jenny Lind")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT count(*) FROM dfs.`shapefiles/CA-cities.shp`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match",5727L, cnt);
  }
}
