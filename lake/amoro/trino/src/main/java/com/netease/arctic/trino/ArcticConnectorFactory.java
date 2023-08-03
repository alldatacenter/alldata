
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

package com.netease.arctic.trino;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.bootstrap.LifeCycleManager;
import io.airlift.event.client.EventModule;
import io.airlift.json.JsonModule;
import io.trino.filesystem.TrinoFileSystemFactory;
import io.trino.filesystem.hdfs.HdfsFileSystemFactory;
import io.trino.plugin.base.CatalogName;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSinkProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorPageSourceProvider;
import io.trino.plugin.base.classloader.ClassLoaderSafeConnectorSplitManager;
import io.trino.plugin.base.classloader.ClassLoaderSafeNodePartitioningProvider;
import io.trino.plugin.base.jmx.ConnectorObjectNameGeneratorModule;
import io.trino.plugin.base.jmx.MBeanServerModule;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.hive.NodeVersion;
import io.trino.plugin.iceberg.IcebergSchemaProperties;
import io.trino.plugin.iceberg.IcebergSecurityModule;
import io.trino.plugin.iceberg.IcebergTableProperties;
import io.trino.plugin.iceberg.InternalIcebergConnectorFactory;
import io.trino.spi.NodeManager;
import io.trino.spi.PageIndexerFactory;
import io.trino.spi.classloader.ThreadContextClassLoader;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorAccessControl;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;
import io.trino.spi.connector.ConnectorNodePartitioningProvider;
import io.trino.spi.connector.ConnectorPageSinkProvider;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.TableProcedureMetadata;
import io.trino.spi.procedure.Procedure;
import io.trino.spi.type.TypeManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weakref.jmx.guice.MBeanModule;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.inject.Scopes.SINGLETON;

/**
 * Factory to generate {@link Connector}
 */
public class ArcticConnectorFactory implements ConnectorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArcticConnectorFactory.class);

  @Override
  public String getName() {
    return "arctic";
  }

  @Override
  public Connector create(String catalogName, Map<String, String> config, ConnectorContext context) {
    ClassLoader classLoader = InternalIcebergConnectorFactory.class.getClassLoader();
    try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(classLoader)) {
      Bootstrap app = new Bootstrap(
          new EventModule(),
          new MBeanModule(),
          new ConnectorObjectNameGeneratorModule("io.trino.plugin.iceberg", "trino.plugin.iceberg"),
          new JsonModule(),
          new ArcticModule(context.getTypeManager()),
          new IcebergSecurityModule(),
          new MBeanServerModule(),
          binder -> {
            binder.bind(NodeVersion.class)
                .toInstance(new NodeVersion(context.getNodeManager().getCurrentNode().getVersion()));
            binder.bind(NodeManager.class).toInstance(context.getNodeManager());
            binder.bind(TypeManager.class).toInstance(context.getTypeManager());
            binder.bind(PageIndexerFactory.class).toInstance(context.getPageIndexerFactory());
            binder.bind(CatalogName.class).toInstance(new CatalogName(catalogName));
            binder.bind(TrinoFileSystemFactory.class).to(HdfsFileSystemFactory.class).in(SINGLETON);
          });

      Injector injector = app
          .doNotInitializeLogging()
          .setRequiredConfigurationProperties(config)
          .initialize();

      LifeCycleManager lifeCycleManager = injector.getInstance(LifeCycleManager.class);
      ArcticTransactionManager transactionManager = injector.getInstance(ArcticTransactionManager.class);
      ConnectorSplitManager splitManager = injector.getInstance(ConnectorSplitManager.class);
      ConnectorPageSourceProvider connectorPageSource = injector.getInstance(ConnectorPageSourceProvider.class);
      ConnectorPageSinkProvider pageSinkProvider = injector.getInstance(ConnectorPageSinkProvider.class);
      ConnectorNodePartitioningProvider connectorDistributionProvider =
          injector.getInstance(ConnectorNodePartitioningProvider.class);
      Set<SessionPropertiesProvider> sessionPropertiesProviders =
          injector.getInstance(Key.get(new TypeLiteral<Set<SessionPropertiesProvider>>() {}));
      IcebergTableProperties icebergTableProperties = injector.getInstance(IcebergTableProperties.class);
      Set<Procedure> procedures = injector.getInstance(Key.get(new TypeLiteral<Set<Procedure>>() {}));
      Set<TableProcedureMetadata> tableProcedures =
          injector.getInstance(Key.get(new TypeLiteral<Set<TableProcedureMetadata>>() {}));
      Optional<ConnectorAccessControl> accessControl =
          injector.getInstance(Key.get(new TypeLiteral<Optional<ConnectorAccessControl>>() {}));

      return new ArcticConnector(
          lifeCycleManager,
          transactionManager,
          new ClassLoaderSafeConnectorSplitManager(splitManager, classLoader),
          new ClassLoaderSafeConnectorPageSourceProvider(connectorPageSource, classLoader),
          new ClassLoaderSafeConnectorPageSinkProvider(pageSinkProvider, classLoader),
          new ClassLoaderSafeNodePartitioningProvider(connectorDistributionProvider, classLoader),
          sessionPropertiesProviders,
          IcebergSchemaProperties.SCHEMA_PROPERTIES,
          icebergTableProperties.getTableProperties(),
          accessControl,
          procedures,
          tableProcedures);
    }
  }
}
