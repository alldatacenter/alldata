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
package org.apache.drill.exec.store.dfs;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.calcite.plan.RelOptRule;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.logical.security.CredentialsProvider;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.planner.PlannerPhase;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.store.AbstractStoragePlugin;
import org.apache.drill.exec.store.ClassPathFileSystem;
import org.apache.drill.exec.store.LocalSyncableFileSystem;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Storage engine associated with a Hadoop FileSystem Implementation. Examples
 * include HDFS, MapRFS, QuantacastFileSystem, LocalFileSystem, as well Apache
 * Drill specific CachedFileSystem, ClassPathFileSystem and
 * LocalSyncableFileSystem. Tables are file names, directories and path
 * patterns. This storage engine delegates to FSFormatEngines but shares
 * references to the FileSystem configuration and path management.
 */
public class FileSystemPlugin extends AbstractStoragePlugin {
  private static final Logger logger = LoggerFactory.getLogger(FileSystemPlugin.class);

  /**
   * The {@code org.apache.hadoop.io.compress} library supports such codecs as
   * Gzip and Bzip2 out of box. This list stores only codecs that are missing in
   * Hadoop library.
   */
  private static final List<String> ADDITIONAL_CODECS = Collections.singletonList(
    ZipCodec.class.getCanonicalName());

  private final FileSystemSchemaFactory schemaFactory;
  private final FormatCreator formatCreator;
  private final Map<FormatPluginConfig, FormatPlugin> formatPluginsByConfig;
  private final FileSystemConfig config;
  private final Configuration fsConf;

  public FileSystemPlugin(FileSystemConfig config, DrillbitContext context, String name) throws ExecutionSetupException {
    super(context, name);
    this.config = config;

    try {
      fsConf = new Configuration();
      Optional.ofNullable(config.getConfig())
          .ifPresent(c -> c.forEach(fsConf::set));

      fsConf.set(FileSystem.FS_DEFAULT_NAME_KEY, config.getConnection());
      fsConf.set("fs.classpath.impl", ClassPathFileSystem.class.getName());
      fsConf.set("fs.dropbox.impl", DropboxFileSystem.class.getName());
      fsConf.set("fs.drill-local.impl", LocalSyncableFileSystem.class.getName());
      CredentialsProvider credentialsProvider = config.getCredentialsProvider();
      if (credentialsProvider != null) {
        credentialsProvider.getCredentials().forEach(fsConf::set);
      }

      addCodecs(fsConf);

      if (isS3Connection(fsConf)) {
        handleS3Credentials(fsConf);
      }

      formatCreator = newFormatCreator(config, context, fsConf);
      List<FormatMatcher> matchers = new ArrayList<>();
      formatPluginsByConfig = new HashMap<>();
      for (FormatPlugin p : formatCreator.getConfiguredFormatPlugins()) {
        matchers.add(p.getMatcher());
        formatPluginsByConfig.put(p.getConfig(), p);
      }
      // sort plugins in order according to their priority
      matchers.sort(Comparator.comparing(FormatMatcher::priority).reversed());

      boolean noWorkspace = config.getWorkspaces() == null || config.getWorkspaces().isEmpty();
      List<WorkspaceSchemaFactory> factories = new ArrayList<>();
      if (!noWorkspace) {
        for (Map.Entry<String, WorkspaceConfig> space : config.getWorkspaces().entrySet()) {
          factories.add(new WorkspaceSchemaFactory(
              this, space.getKey(), name, space.getValue(), matchers,
              context.getLpPersistence().getMapper(), context.getClasspathScan()));
        }
      }

      // if the "default" workspace is not given add one.
      if (noWorkspace || !config.getWorkspaces().containsKey(DEFAULT_WS_NAME)) {
        factories.add(new WorkspaceSchemaFactory(this, DEFAULT_WS_NAME, name,
            WorkspaceConfig.DEFAULT, matchers,
            context.getLpPersistence().getMapper(), context.getClasspathScan()));
      }

      this.schemaFactory = new FileSystemSchemaFactory(name, factories);
    } catch (IOException e) {
      throw new ExecutionSetupException("Failure setting up file system plugin.", e);
    }
  }

  /**
   * Merges codecs from configuration with the {@link #ADDITIONAL_CODECS}
   * and updates configuration property.
   * Drill built-in codecs are added at the beginning of the codecs string
   * so config codecs can override Drill ones.
   *
   * @param conf Hadoop configuration
   */
  private void addCodecs(Configuration conf) {
    String confCodecs = conf.get(CommonConfigurationKeys.IO_COMPRESSION_CODECS_KEY);
    String builtInCodecs = String.join(",", ADDITIONAL_CODECS);
    String newCodecs = Strings.isNullOrEmpty(confCodecs)
      ? builtInCodecs
      : builtInCodecs + ", " + confCodecs;
    logger.trace("Codecs: {}", newCodecs);
    conf.set(CommonConfigurationKeys.IO_COMPRESSION_CODECS_KEY, newCodecs);
  }

  private boolean isS3Connection(Configuration conf) {
    URI uri = FileSystem.getDefaultUri(conf);
    return uri.getScheme().equals("s3a");
  }

  /**
   * Retrieve secret and access keys from configured (with
   * {@link org.apache.hadoop.security.alias.CredentialProviderFactory#CREDENTIAL_PROVIDER_PATH} property)
   * credential providers and set it into {@code conf}. If provider path is not configured or credential
   * is absent in providers, it will conditionally fallback to configuration setting. The fallback will occur unless
   * {@link org.apache.hadoop.security.alias.CredentialProvider#CLEAR_TEXT_FALLBACK} is set to {@code false}.
   *
   * @param conf {@code Configuration} which will be updated with credentials from provider
   * @throws IOException thrown if a credential cannot be retrieved from provider
   */
  private void handleS3Credentials(Configuration conf) throws IOException {
    String[] credentialKeys = {"fs.s3a.secret.key", "fs.s3a.access.key"};
    for (String key : credentialKeys) {
      char[] credentialChars = conf.getPassword(key);
      if (credentialChars == null) {
        logger.warn("Property '{}' is absent.", key);
      } else {
        conf.set(key, String.valueOf(credentialChars));
      }
    }
  }

  /**
   * Creates a new FormatCreator instance.
   *
   * To be used by subclasses to return custom formats if required.
   * Note that this method is called by the constructor, which fields may not be initialized yet.
   *
   * @param config the plugin configuration
   * @param context the drillbit context
   * @return a new FormatCreator instance
   */
  protected FormatCreator newFormatCreator(FileSystemConfig config, DrillbitContext context, Configuration fsConf) {
    return new FormatCreator(context, fsConf, config);
  }

  @Override
  public boolean supportsRead() {
    return true;
  }

  @Override
  public StoragePluginConfig getConfig() {
    return config;
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      SessionOptionManager options) throws IOException {
    return getPhysicalScan(userName, selection, AbstractGroupScan.ALL_COLUMNS,
        options, null);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      SessionOptionManager options, MetadataProviderManager metadataProviderManager) throws IOException {
    return getPhysicalScan(userName, selection, AbstractGroupScan.ALL_COLUMNS,
        options, metadataProviderManager);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      List<SchemaPath> columns) throws IOException {
    return getPhysicalScan(userName, selection, columns, null, null);
  }

  @Override
  public AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      List<SchemaPath> columns, SessionOptionManager options,
      MetadataProviderManager metadataProviderManager) throws IOException {
    FormatSelection formatSelection = selection.getWith(
        context.getLpPersistence().getMapper(), FormatSelection.class);
    FormatPlugin plugin = getFormatPlugin(formatSelection.getFormat());
    return plugin.getGroupScan(userName, formatSelection.getSelection(), columns,
        options, metadataProviderManager);
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    schemaFactory.registerSchemas(schemaConfig, parent);
  }

  public FormatPlugin getFormatPlugin(String name) {
    return formatCreator.getFormatPluginByName(name);
  }

  /**
   * If format plugin configuration is for named format plugin, will return
   * format plugin from pre-loaded list by name. For other cases will try to
   * find format plugin by its configuration, if not present will attempt to
   * create one.
   *
   * @param config format plugin configuration
   * @return format plugin for given configuration if found, null otherwise
   */
  @Override
  public FormatPlugin getFormatPlugin(FormatPluginConfig config) {
    if (config instanceof NamedFormatPluginConfig) {
      return formatCreator.getFormatPluginByName(((NamedFormatPluginConfig) config).getName());
    }

    FormatPlugin plugin = formatPluginsByConfig.get(config);
    if (plugin == null) {
      plugin = formatCreator.newFormatPlugin(config);
    }
    return plugin;
  }

  @Override
  public Set<? extends RelOptRule> getOptimizerRules(OptimizerRulesContext optimizerContext, PlannerPhase phase) {
    return formatCreator.getConfiguredFormatPlugins()
      .stream()
      .map(plugin -> plugin.getOptimizerRules(phase))
      .filter(Objects::nonNull)
      .flatMap(Collection::stream)
      .collect(Collectors.toSet());
  }

  public Configuration getFsConf() {
    return new Configuration(fsConf);
  }
}
