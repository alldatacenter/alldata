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
package org.apache.drill.exec.hive;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.hive.HiveStoragePlugin;
import org.apache.drill.exec.store.hive.HiveStoragePluginConfig;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.session.SessionState;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static org.apache.drill.exec.hive.HiveTestUtilities.createDirWithPosixPermissions;


/**
 * Test fixture for configuration of Hive tests, which
 * allows granular control over initialization of test data
 * and hive storage plugin.
 * <p>
 * Below is example of usage HiveTestFixture along with ClusterFixture:
 * <p>
 * <pre><code>
 *   // Note that HiveTestFixture doesn't require extension of ClusterTest,
 *   // this is just the simplest way for configuring test drillbit
 *   public class HiveTestExample extends ClusterTest {
 *
 *       private static HiveTestFixture hiveTestFixture;
 *
 *       {@literal @}BeforeClass
 *       public static void setUp() throws Exception {
 *            startCluster(ClusterFixture.builder(dirTestWatcher));
 *
 *            // Below is minimal config which uses defaults from HiveTestFixture.Builder
 *            // constructor, but any option for driver or storage plugin may be
 *            // overridden using builder's methods
 *            hiveTestFixture = HiveTestFixture.builder(dirTestWatcher).build();
 *
 *            // Use driver manager to configure test data in Hive metastore
 *            hiveTestFixture.getDriverManager().runWithinSession(HiveTestExample::generateData);
 *
 *            // Use plugin manager to add, remove, update hive storage plugin of one or many test drillbits
 *            hiveTestFixture.getPluginManager().addHivePluginTo(cluster.drillbits());
 *       }
 *
 *       private static void generateData(Driver driver) {
 *            // Set up data using HiveTestUtilities.executeQuery(driver, sql)
 *       }
 *
 *       {@literal @}AfterClass
 *       public static void tearDown() throws Exception {
 *            if (nonNull(hiveTestFixture)) {
 *                hiveTestFixture.getPluginManager().removeHivePluginFrom(cluster.drillbits());
 *            }
 *       }
 * }
 * </code></pre>
 */
public class HiveTestFixture {

  private final Map<String, String> pluginConf;

  private final Map<String, String> driverConf;

  private final String pluginName;

  private final HivePluginManager pluginManager;

  private final HiveDriverManager driverManager;

  private HiveTestFixture(Builder builder) {
    this.pluginConf = new HashMap<>(builder.pluginConf);
    this.driverConf = new HashMap<>(builder.driverConf);
    this.pluginName = builder.pluginName;
    this.pluginManager = new HivePluginManager();
    this.driverManager = new HiveDriverManager();
  }

  public static Builder builder(BaseDirTestWatcher dirWatcher) {
    return builder(requireNonNull(dirWatcher, "Parameter 'dirWatcher' can't be null!").getRootDir());
  }

  public static Builder builder(File baseDir) {
    return new Builder(requireNonNull(baseDir, "Parameter 'baseDir' can't be null!"));
  }

  public HivePluginManager getPluginManager() {
    return pluginManager;
  }

  public HiveDriverManager getDriverManager() {
    return driverManager;
  }

  /**
   * Returns current value of 'hive.metastore.warehouse.dir' option
   * which expected to represent location of metastore warehouse directory.
   * Builder's user can override any option either of pluginConf or driverConf.
   * Since setting of the option is not enforced, this method just tries to
   * find it in any of the conf maps.
   *
   * @return current value of 'hive.metastore.warehouse.dir' option
   *         from pluginConf or driverConf
   */
  public String getWarehouseDir() {
    String warehouseDir = pluginConf.get(ConfVars.METASTOREWAREHOUSE.varname);
    return nonNull(warehouseDir) ? warehouseDir : driverConf.get(ConfVars.METASTOREWAREHOUSE.varname);
  }

  public static class Builder {

    private final Map<String, String> pluginConf;

    private final Map<String, String> driverConf;

    private String pluginName;

    private Builder(File baseDir) {
      this.pluginConf = new HashMap<>();
      this.driverConf = new HashMap<>();
      String jdbcUrl = String.format("jdbc:derby:;databaseName=%s;create=true",
          new File(baseDir, "metastore_db").getAbsolutePath());
      String warehouseDir = new File(baseDir, "warehouse").getAbsolutePath();
      // Drill Hive Storage plugin defaults
      pluginName("hive");
      pluginOption(ConfVars.METASTOREURIS, "");
      pluginOption(ConfVars.METASTORECONNECTURLKEY, jdbcUrl);
      pluginOption(ConfVars.METASTOREWAREHOUSE, warehouseDir);
      pluginOption(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
      // Hive Driver defaults
      driverOption(ConfVars.METASTORECONNECTURLKEY, jdbcUrl);
      driverOption(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
      driverOption(ConfVars.METASTOREWAREHOUSE, warehouseDir);
      driverOption("mapred.job.tracker", "local");
      driverOption(ConfVars.SCRATCHDIR, createDirWithPosixPermissions(baseDir, "scratch_dir").getAbsolutePath());
      driverOption(ConfVars.LOCALSCRATCHDIR, createDirWithPosixPermissions(baseDir, "local_scratch_dir").getAbsolutePath());
      driverOption(ConfVars.DYNAMICPARTITIONINGMODE, "nonstrict");
      driverOption(ConfVars.METASTORE_AUTO_CREATE_ALL, Boolean.toString(true));
      driverOption(ConfVars.METASTORE_SCHEMA_VERIFICATION, Boolean.toString(false));
      driverOption(ConfVars.HIVE_MATERIALIZED_VIEW_ENABLE_AUTO_REWRITING, Boolean.toString(false));
      driverOption(HiveConf.ConfVars.HIVESESSIONSILENT, Boolean.toString(true));
      driverOption(ConfVars.HIVE_CBO_ENABLED, Boolean.toString(false));
    }

    public Builder pluginOption(ConfVars option, String value) {
      return pluginOption(option.varname, value);
    }

    public Builder pluginOption(String option, String value) {
      return put(pluginConf, option, value);
    }

    public Builder driverOption(ConfVars option, String value) {
      return driverOption(option.varname, value);
    }

    public Builder driverOption(String option, String value) {
      return put(driverConf, option, value);
    }

    public Builder pluginName(String name) {
      this.pluginName = Objects.requireNonNull(name, "Hive plugin name can't be null!");
      return this;
    }

    private Builder put(Map<String, String> map, String key, String value) {
      map.put(key, value);
      return this;
    }

    public HiveTestFixture build() {
      return new HiveTestFixture(this);
    }

  }

  /**
   * Implements addition, update and deletion of
   * Hive storage plugin for drillbits passed from outside.
   * The class was made inner because it uses pluginName and pluginConf
   * of enclosing fixture instance.
   */
  public class HivePluginManager {

    /**
     *  {@link HiveTestFixture}'s constructor will create instance,
     *  and API users will get it via {@link HiveTestFixture#getPluginManager()}.
     */
    private HivePluginManager() {
    }

    public void addHivePluginTo(Drillbit... drillbits) {
      addHivePluginTo(Arrays.asList(drillbits));
    }

    public void addHivePluginTo(Iterable<Drillbit> drillbits) {
      try {
        for (Drillbit drillbit : drillbits) {
          HiveStoragePluginConfig pluginConfig = new HiveStoragePluginConfig(new HashMap<>(pluginConf));
          pluginConfig.setEnabled(true);
          drillbit.getContext().getStorage().put(pluginName, pluginConfig);
        }
      } catch (PluginException e) {
        throw new RuntimeException("Failed to add Hive storage plugin to drillbits", e);
      }
    }

    public void removeHivePluginFrom(Drillbit... drillbits) {
      removeHivePluginFrom(Arrays.asList(drillbits));
    }

    public void removeHivePluginFrom(Iterable<Drillbit> drillbits) {
      try {
        for (Drillbit drillbit : drillbits) {
          drillbit.getContext().getStorage().remove(pluginName);
        }
      } catch (PluginException e) {
        throw new RuntimeException("Failed to remove Hive storage plugin for drillbits", e);
      }
    }

    public void updateHivePlugin(Iterable<Drillbit> drillbits,
                                 Map<String, String> configOverride) {
      try {
        for (Drillbit drillbit : drillbits) {
          StoragePluginRegistry pluginRegistry = drillbit.getContext().getStorage();
          HiveStoragePlugin storagePlugin = Objects.requireNonNull(
              (HiveStoragePlugin) pluginRegistry.getPlugin(pluginName),
              String.format("Hive storage plugin with name '%s' doesn't exist.", pluginName));

          HiveStoragePluginConfig newPluginConfig = storagePlugin.getConfig();
          newPluginConfig.getConfigProps().putAll(configOverride);
          pluginRegistry.put(pluginName, newPluginConfig);
        }
      } catch (PluginException e) {
        throw new RuntimeException("Failed to update Hive storage plugin for drillbits", e);
      }
    }
  }

  /**
   * Implements method for initialization and passing
   * of Hive to consumer instances in order to be used
   * for test data generation within session.
   * The class was made inner because it uses driverConf
   * of enclosing fixture instance.
   */
  public class HiveDriverManager {

    /**
     *  {@link HiveTestFixture}'s constructor will create instance,
     *  and API users will get it via {@link HiveTestFixture#getDriverManager()}.
     */
    private HiveDriverManager() { }

    public void runWithinSession(Consumer<Driver> driverConsumer) {
      final HiveConf hiveConf = new HiveConf(SessionState.class);
      driverConf.forEach(hiveConf::set);
      SessionState ss = new SessionState(hiveConf);
      try (Closeable ssClose = ss::close) {
        SessionState.start(ss);
        driverConsumer.accept(new Driver(hiveConf));
      } catch (IOException e) {
        throw new RuntimeException("Exception was thrown while closing SessionState", e);
      }
    }
  }
}
