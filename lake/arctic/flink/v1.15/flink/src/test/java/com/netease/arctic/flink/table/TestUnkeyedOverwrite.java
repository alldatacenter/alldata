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

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.util.DataUtil;
import com.netease.arctic.hive.HiveTableTestBase;
import org.apache.flink.table.api.ApiExpression;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.Table;
import org.apache.flink.test.util.MiniClusterWithClientResource;
import org.apache.iceberg.flink.MiniClusterResource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static com.netease.arctic.ams.api.MockArcticMetastoreServer.TEST_CATALOG_NAME;

@RunWith(Parameterized.class)
public class TestUnkeyedOverwrite extends FlinkTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestUnkeyedOverwrite.class);

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @ClassRule
  public static final MiniClusterWithClientResource MINI_CLUSTER_RESOURCE =
      MiniClusterResource.createWithClassloaderCheckDisabled();
  
  private static final String TABLE = "test_unkeyed";
  private static final String DB = TABLE_ID.getDatabase();

  private String catalog;
  private String db;
  private HiveTableTestBase hiveTableTestBase = new HiveTableTestBase();

  @Parameterized.Parameter
  public boolean isHive;

  @Parameterized.Parameters(name = "isHive = {0}")
  public static Collection<Boolean> parameters() {
    return Arrays.asList(false);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    HiveTableTestBase.startMetastore();
    FlinkTestBase.prepare();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    FlinkTestBase.shutdown();
  }

  public void before() throws Exception {
    if (isHive) {
      catalog = HiveTableTestBase.HIVE_CATALOG_NAME;
      db = HiveTableTestBase.HIVE_DB_NAME;
      hiveTableTestBase.setupTables();
    } else {
      catalog = TEST_CATALOG_NAME;
      db = DB;
      super.before();
    }
    super.config(catalog);
  }

  @After
  public void after() {
    sql("DROP TABLE IF EXISTS arcticCatalog." + db + "." + TABLE);
    if (isHive) {
      hiveTableTestBase.clearTable();
    }
  }

  @Test(timeout = 30000)
  public void testInsertOverwrite() throws IOException {
    List<Object[]> data = new LinkedList<>();
    data.add(new Object[]{1000004, "a"});
    data.add(new Object[]{1000015, "b"});
    data.add(new Object[]{1000011, "c"});
    data.add(new Object[]{1000014, "d"});
    data.add(new Object[]{1000021, "d"});
    data.add(new Object[]{1000007, "e"});

    List<ApiExpression> rows = DataUtil.toRows(data);

    Table input = getTableEnv().fromValues(DataTypes.ROW(
            DataTypes.FIELD("id", DataTypes.INT()),
            DataTypes.FIELD("name", DataTypes.STRING())
        ),
        rows
    );
    getTableEnv().createTemporaryView("input", input);

    sql("CREATE CATALOG arcticCatalog WITH %s", toWithClause(props));
    sql("CREATE TABLE IF NOT EXISTS arcticCatalog." + db + "." + TABLE + "(" +
        " id INT, name STRING)");

    sql("insert overwrite arcticCatalog." + db + "." + TABLE + " select * from input");

    Assert.assertEquals(
        DataUtil.toRowSet(data), sqlSet("select * from arcticCatalog." + db + "." + TABLE + " /*+ OPTIONS(" +
            "'streaming'='false'" +
            ") */"));
  }

  @Test
  public void testPartitionInsertOverwrite() throws IOException {
    List<Object[]> data = new LinkedList<>();
    data.add(new Object[]{1000004, "a", "2022-05-17"});
    data.add(new Object[]{1000015, "b", "2022-05-17"});
    data.add(new Object[]{1000011, "c", "2022-05-17"});
    data.add(new Object[]{1000014, "d", "2022-05-18"});
    data.add(new Object[]{1000021, "d", "2022-05-18"});
    data.add(new Object[]{1000007, "e", "2022-05-18"});

    List<Object[]> expected = new LinkedList<>();
    expected.add(new Object[]{11, "d", "2022-05-19"});
    expected.add(new Object[]{21, "d", "2022-05-19"});
    expected.add(new Object[]{35, "e", "2022-05-19"});

    data.addAll(expected);
    List<ApiExpression> rows = DataUtil.toRows(data);

    Table input = getTableEnv().fromValues(DataTypes.ROW(
            DataTypes.FIELD("id", DataTypes.INT()),
            DataTypes.FIELD("name", DataTypes.STRING()),
            DataTypes.FIELD("dt", DataTypes.STRING())
        ),
        rows
    );
    getTableEnv().createTemporaryView("input", input);

    sql("CREATE CATALOG arcticCatalog WITH %s", toWithClause(props));

    sql("CREATE TABLE IF NOT EXISTS arcticCatalog." + db + "." + TABLE + "(" +
        " id INT, name STRING, dt STRING) PARTITIONED BY (dt)");

    sql("insert into arcticCatalog." + db + "." + TABLE +
        " select * from input");
    sql("insert overwrite arcticCatalog." + db + "." + TABLE +
        " PARTITION (dt='2022-05-18') select id, name from input where dt = '2022-05-19'");

    Assert.assertEquals(DataUtil.toRowSet(expected),
        sqlSet("select id, name, '2022-05-19' from arcticCatalog." + db + "." + TABLE + " /*+ OPTIONS(" +
            "'streaming'='false'" +
            ") */" +
            " where dt='2022-05-18'"));
  }

}
