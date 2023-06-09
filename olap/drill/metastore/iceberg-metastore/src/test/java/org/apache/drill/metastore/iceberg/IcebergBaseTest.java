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
package org.apache.drill.metastore.iceberg;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.iceberg.config.IcebergConfigConstants;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;

@Category(MetastoreTest.class)
public abstract class IcebergBaseTest extends BaseTest {

  @ClassRule
  public static TemporaryFolder defaultFolder = new TemporaryFolder();

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  /**
   * Creates Hadoop configuration and sets local file system as default.
   *
   * @return {@link Configuration} instance
   */
  protected static Configuration baseHadoopConfig() {
    Configuration config = new Configuration();
    config.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    return config;
  }

  /**
   * Creates default configuration for Iceberg Metastore based on given base path.
   * Sets local file system as default.
   *
   * @param base Iceberg Metastore base path
   * @return {@link Config} instance
   */
  public static Config baseIcebergConfig(File base) {
    return DrillConfig.create()
      .withValue(IcebergConfigConstants.BASE_PATH,
        ConfigValueFactory.fromAnyRef(new Path(base.toURI().getPath()).toUri().getPath()))
      .withValue(IcebergConfigConstants.RELATIVE_PATH,
        ConfigValueFactory.fromAnyRef("drill/metastore/iceberg"))
      .withValue(String.format("%s.%s", IcebergConfigConstants.CONFIG_PROPERTIES, FileSystem.FS_DEFAULT_NAME_KEY),
        ConfigValueFactory.fromAnyRef(FileSystem.DEFAULT_FS))
      .withValue(IcebergConfigConstants.COMPONENTS_TABLES_LOCATION,
        ConfigValueFactory.fromAnyRef("tables"))
      .withValue(IcebergConfigConstants.COMPONENTS_VIEWS_LOCATION,
        ConfigValueFactory.fromAnyRef("views"));
  }
}
