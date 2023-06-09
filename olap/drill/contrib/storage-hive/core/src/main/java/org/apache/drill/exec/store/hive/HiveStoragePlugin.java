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
package org.apache.drill.exec.store.hive;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.commons.lang3.StringEscapeUtils;

import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.sql.logical.ConvertHiveParquetScanToDrillParquetScan;
import org.apache.drill.exec.planner.sql.logical.HivePushPartitionFilterIntoScan;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.StoragePluginOptimizerRule;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.exec.store.hive.schema.HiveSchemaFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.metastore.api.MetaException;
import org.apache.thrift.transport.TTransportException;

public class HiveStoragePlugin extends AbstractStoragePlugin {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HiveStoragePlugin.class);

  public static final String HIVE_MAPRDB_FORMAT_PLUGIN_NAME = "hive-maprdb";

  private final HiveStoragePluginConfig config;
  private HiveSchemaFactory schemaFactory;
  private final HiveConf hiveConf;

  public HiveStoragePlugin(HiveStoragePluginConfig config, DrillbitContext context, String name) throws ExecutionSetupException {
    super(context, name);
    this.config = config;
    this.hiveConf = HiveUtilities.generateHiveConf(config.getConfigProps());
    this.schemaFactory = new HiveSchemaFactory(this, name, hiveConf);
  }

  public HiveConf getHiveConf() {
    return hiveConf;
  }

  @Override
  public HiveStoragePluginConfig getConfig() {
    return config;
  }

  @Override
  public HiveScan getPhysicalScan(String userName, JSONOptions selection, SessionOptionManager options) throws IOException {
    return getPhysicalScan(userName, selection, AbstractGroupScan.ALL_COLUMNS, options);
  }

  @Override
  public HiveScan getPhysicalScan(String userName, JSONOptions selection, List<SchemaPath> columns) throws IOException {
    return getPhysicalScan(userName, selection, columns, null);
  }

  @Override
  public HiveScan getPhysicalScan(String userName, JSONOptions selection, List<SchemaPath> columns, SessionOptionManager options) throws IOException {
    HiveReadEntry hiveReadEntry = selection.getListWith(new ObjectMapper(), new TypeReference<HiveReadEntry>(){});
    try {
      Map<String, String> confProperties = new HashMap<>();
      if (options != null) {
        String value = StringEscapeUtils.unescapeJava(options.getString(ExecConstants.HIVE_CONF_PROPERTIES));
        logger.trace("[{}] is set to {}.", ExecConstants.HIVE_CONF_PROPERTIES, value);
        try {
          Properties properties = new Properties();
          properties.load(new StringReader(value));
          confProperties =
            properties.stringPropertyNames().stream()
              .collect(
                Collectors.toMap(
                  Function.identity(),
                  properties::getProperty,
                  (o, n) -> n));
          } catch (IOException e) {
            logger.warn("Unable to parse Hive conf properties {}, ignoring them.", value);
        }
      }

      return new HiveScan(userName, hiveReadEntry, this, columns, null, confProperties);
    } catch (ExecutionSetupException e) {
      throw new IOException(e);
    }
  }

  // Forced to synchronize this method to allow error recovery
  // in the multi-threaded case. Can remove synchronized only
  // by restructuring connections and cache to allow better
  // recovery from failed secure connections.

  @Override
  public synchronized void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    try {
      schemaFactory.registerSchemas(schemaConfig, parent);
      return;

    // Hack. We may need to retry the connection. But, we can't because
    // the retry logic is implemented in the very connection we need to
    // discard and rebuild. To work around, we discard the entire schema
    // factory, and all its invalid connections. Very crude, but the
    // easiest short-term solution until we refactor the code to do the
    // job properly. See DRILL-5510.

    } catch (Throwable e) {
      // Unwrap exception
      Throwable ex = e;
      while (true) {
        // Case for failing on an invalid cached connection
        if (ex instanceof MetaException ||
            // Case for a timed-out impersonated connection, and
            // an invalid non-secure connection used to get security
            // tokens.
            ex instanceof TTransportException) {
          break;
        }

        // All other exceptions are not handled, just pass along up
        // the stack.

        if (ex.getCause() == null  ||  ex.getCause() == ex) {
          logger.error("Hive metastore register schemas failed", e);
          throw new DrillRuntimeException("Unknown Hive error", e);
        }
        ex = ex.getCause();
      }
    }

    // Build a new factory which will cause an all new set of
    // Hive metastore connections to be created.

    try {
      schemaFactory.close();
    } catch (Throwable t) {
      // Ignore, we're in a bad state.
      logger.warn("Schema factory forced close failed, error ignored", t);
    }
    try {
      schemaFactory = new HiveSchemaFactory(this, getName(), hiveConf);
    } catch (ExecutionSetupException e) {
      throw new DrillRuntimeException(e);
    }

    // Try the schemas again. If this fails, just give up.

    schemaFactory.registerSchemas(schemaConfig, parent);
    logger.debug("Successfully recovered from a Hive metastore connection failure.");
  }

  @Override
  public Set<StoragePluginOptimizerRule> getLogicalOptimizerRules(OptimizerRulesContext optimizerContext) {
    final String defaultPartitionValue = hiveConf.get(ConfVars.DEFAULTPARTITIONNAME.varname);

    ImmutableSet.Builder<StoragePluginOptimizerRule> ruleBuilder = ImmutableSet.builder();

    ruleBuilder.add(HivePushPartitionFilterIntoScan.getFilterOnProject(optimizerContext, defaultPartitionValue));
    ruleBuilder.add(HivePushPartitionFilterIntoScan.getFilterOnScan(optimizerContext, defaultPartitionValue));

    return ruleBuilder.build();
  }

  @Override
  public Set<StoragePluginOptimizerRule> getPhysicalOptimizerRules(OptimizerRulesContext optimizerRulesContext) {
    ImmutableSet.Builder<StoragePluginOptimizerRule> ruleBuilder = ImmutableSet.builder();
    OptionManager options = optimizerRulesContext.getPlannerSettings().getOptions();
    // TODO: Remove implicit using of convert_fromTIMESTAMP_IMPALA function
    // once "store.parquet.reader.int96_as_timestamp" will be true by default
    if (options.getBoolean(ExecConstants.HIVE_OPTIMIZE_SCAN_WITH_NATIVE_READERS) ||
        options.getBoolean(ExecConstants.HIVE_OPTIMIZE_PARQUET_SCAN_WITH_NATIVE_READER)) {
      ruleBuilder.add(ConvertHiveParquetScanToDrillParquetScan.INSTANCE);
    }
    if (options.getBoolean(ExecConstants.HIVE_OPTIMIZE_MAPRDB_JSON_SCAN_WITH_NATIVE_READER)) {
      try {
        Class<?> hiveToDrillMapRDBJsonRuleClass =
            Class.forName("org.apache.drill.exec.planner.sql.logical.ConvertHiveMapRDBJsonScanToDrillMapRDBJsonScan");
        ruleBuilder.add((StoragePluginOptimizerRule) hiveToDrillMapRDBJsonRuleClass.getField("INSTANCE").get(null));
      } catch (ReflectiveOperationException e) {
        logger.warn("Current Drill build is not designed for working with Hive MapR-DB tables. " +
            "Please disable {} option", ExecConstants.HIVE_OPTIMIZE_MAPRDB_JSON_SCAN_WITH_NATIVE_READER);
      }
    }
    return ruleBuilder.build();
  }

  @Override
  public FormatPlugin getFormatPlugin(FormatPluginConfig formatConfig) {
    //  TODO: implement formatCreator similar to FileSystemPlugin formatCreator. DRILL-6621
    try {
      Class<?> mapRDBFormatPluginConfigClass =
          Class.forName("org.apache.drill.exec.store.mapr.db.MapRDBFormatPluginConfig");
      Class<?> mapRDBFormatPluginClass =
          Class.forName("org.apache.drill.exec.store.mapr.db.MapRDBFormatPlugin");

      if (mapRDBFormatPluginConfigClass.isInstance(formatConfig)) {
        return (FormatPlugin) mapRDBFormatPluginClass.getConstructor(
              new Class[]{String.class, DrillbitContext.class, Configuration.class,
                  StoragePluginConfig.class, mapRDBFormatPluginConfigClass})
          .newInstance(
              new Object[]{HIVE_MAPRDB_FORMAT_PLUGIN_NAME, context, hiveConf, config, formatConfig});
      }
    } catch (ReflectiveOperationException e) {
      throw new DrillRuntimeException("The error is occurred while connecting to MapR-DB or instantiating mapRDBFormatPlugin", e);
    }
    throw new DrillRuntimeException(String.format("Hive storage plugin doesn't support usage of %s format plugin",
        formatConfig.getClass().getName()));
  }

}
