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
package org.apache.drill.metastore.rdbms.components.tables;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.components.tables.AbstractTablesMetastoreTest;
import org.apache.drill.metastore.rdbms.RdbmsMetastore;
import org.apache.drill.metastore.rdbms.config.RdbmsConfigConstants;
import org.junit.BeforeClass;

public class TestRdbmsTablesMetastore extends AbstractTablesMetastoreTest {

  @BeforeClass
  public static void init() {
    Config config = DrillConfig.create()
      .withValue(RdbmsConfigConstants.DATA_SOURCE_DRIVER, ConfigValueFactory.fromAnyRef("org.sqlite.JDBC"))
      .withValue(RdbmsConfigConstants.DATA_SOURCE_URL, ConfigValueFactory.fromAnyRef("jdbc:sqlite::memory:"));

    innerInit(config, RdbmsMetastore.class);
  }
}
