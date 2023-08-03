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

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.netease.arctic.trino.keyed.KeyedConnectorSplitManager;
import com.netease.arctic.trino.keyed.KeyedPageSourceProvider;
import com.netease.arctic.trino.unkeyed.ArcticTrinoCatalogFactory;
import com.netease.arctic.trino.unkeyed.IcebergPageSourceProvider;
import com.netease.arctic.trino.unkeyed.IcebergSplitManager;
import io.airlift.configuration.ConfigBinder;
import io.trino.hdfs.HdfsConfig;
import io.trino.hdfs.HdfsConfiguration;
import io.trino.hdfs.HdfsEnvironment;
import io.trino.hdfs.authentication.HdfsAuthentication;
import io.trino.plugin.base.session.SessionPropertiesProvider;
import io.trino.plugin.hive.FileFormatDataSourceStats;
import io.trino.plugin.hive.NamenodeStats;
import io.trino.plugin.hive.metastore.HiveMetastoreConfig;
import io.trino.plugin.hive.orc.OrcReaderConfig;
import io.trino.plugin.hive.orc.OrcWriterConfig;
import io.trino.plugin.hive.parquet.ParquetReaderConfig;
import io.trino.plugin.hive.parquet.ParquetWriterConfig;
import io.trino.plugin.iceberg.CommitTaskData;
import io.trino.plugin.iceberg.IcebergConfig;
import io.trino.plugin.iceberg.IcebergFileWriterFactory;
import io.trino.plugin.iceberg.IcebergNodePartitioningProvider;
import io.trino.plugin.iceberg.IcebergPageSinkProvider;
import io.trino.plugin.iceberg.IcebergSessionProperties;
import io.trino.plugin.iceberg.IcebergTableProperties;
import io.trino.plugin.iceberg.RollbackToSnapshotProcedure;
import io.trino.plugin.iceberg.TableStatisticsWriter;
import io.trino.plugin.iceberg.catalog.TrinoCatalogFactory;
import io.trino.plugin.iceberg.procedure.ExpireSnapshotsTableProcedure;
import io.trino.plugin.iceberg.procedure.OptimizeTableProcedure;
import io.trino.plugin.iceberg.procedure.RemoveOrphanFilesTableProcedure;
import io.trino.spi.connector.ConnectorNodePartitioningProvider;
import io.trino.spi.connector.ConnectorPageSinkProvider;
import io.trino.spi.connector.ConnectorPageSourceProvider;
import io.trino.spi.connector.ConnectorSplitManager;
import io.trino.spi.connector.TableProcedureMetadata;
import io.trino.spi.procedure.Procedure;
import io.trino.spi.type.TypeManager;
import org.weakref.jmx.guice.ExportBinder;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static io.airlift.configuration.ConfigBinder.configBinder;
import static io.airlift.json.JsonCodecBinder.jsonCodecBinder;
import static org.weakref.jmx.guice.ExportBinder.newExporter;

/**
 * Arctic module of Trino
 */
public class ArcticModule implements Module {

  private TypeManager typeManager;

  public ArcticModule(TypeManager typeManager) {
    this.typeManager = typeManager;
  }

  @Override
  public void configure(Binder binder) {
    binder.bind(TypeManager.class).toInstance(typeManager);

    configBinder(binder).bindConfig(ArcticConfig.class);
    binder.bind(IcebergSessionProperties.class).in(Scopes.SINGLETON);
    binder.bind(KeyedConnectorSplitManager.class).in(Scopes.SINGLETON);
    binder.bind(KeyedPageSourceProvider.class).in(Scopes.SINGLETON);
    binder.bind(ArcticCatalogFactory.class).to(DefaultArcticCatalogFactory.class).in(Scopes.SINGLETON);
    binder.bind(TrinoCatalogFactory.class).to(ArcticTrinoCatalogFactory.class).in(Scopes.SINGLETON);
    binder.bind(ArcticTransactionManager.class).in(Scopes.SINGLETON);
    binder.bind(ArcticMetadataFactory.class).in(Scopes.SINGLETON);
    binder.bind(TableStatisticsWriter.class).in(Scopes.SINGLETON);
    binder.bind(ConnectorSplitManager.class).to(ArcticConnectorSplitManager.class).in(Scopes.SINGLETON);
    binder.bind(ConnectorPageSourceProvider.class).to(ArcticPageSourceProvider.class).in(Scopes.SINGLETON);

    configBinder(binder).bindConfig(HiveMetastoreConfig.class);
    configBinder(binder).bindConfig(IcebergConfig.class);

    newSetBinder(binder, SessionPropertiesProvider.class).addBinding()
        .to(ArcticSessionProperties.class)
        .in(Scopes.SINGLETON);
    binder.bind(IcebergTableProperties.class).in(Scopes.SINGLETON);

    binder.bind(IcebergSplitManager.class).in(Scopes.SINGLETON);

    binder.bind(IcebergPageSourceProvider.class).in(Scopes.SINGLETON);

    binder.bind(ConnectorPageSinkProvider.class)
        .to(IcebergPageSinkProvider.class).in(Scopes.SINGLETON);

    binder.bind(ConnectorNodePartitioningProvider.class)
        .to(IcebergNodePartitioningProvider.class).in(Scopes.SINGLETON);

    configBinder(binder).bindConfig(OrcReaderConfig.class);
    configBinder(binder).bindConfig(OrcWriterConfig.class);

    configBinder(binder).bindConfig(ParquetReaderConfig.class);
    configBinder(binder).bindConfig(ParquetWriterConfig.class);

    jsonCodecBinder(binder).bindJsonCodec(CommitTaskData.class);

    binder.bind(FileFormatDataSourceStats.class).in(Scopes.SINGLETON);
    newExporter(binder).export(FileFormatDataSourceStats.class).withGeneratedName();

    binder.bind(IcebergFileWriterFactory.class).in(Scopes.SINGLETON);
    newExporter(binder).export(IcebergFileWriterFactory.class).withGeneratedName();

    Multibinder<Procedure> procedures = newSetBinder(binder, Procedure.class);
    procedures.addBinding().toProvider(RollbackToSnapshotProcedure.class).in(Scopes.SINGLETON);

    Multibinder<TableProcedureMetadata> tableProcedures = newSetBinder(binder, TableProcedureMetadata.class);
    tableProcedures.addBinding().toProvider(OptimizeTableProcedure.class).in(Scopes.SINGLETON);
    tableProcedures.addBinding().toProvider(ExpireSnapshotsTableProcedure.class).in(Scopes.SINGLETON);
    tableProcedures.addBinding().toProvider(RemoveOrphanFilesTableProcedure.class).in(Scopes.SINGLETON);

    //hdfs
    ConfigBinder.configBinder(binder).bindConfig(HdfsConfig.class);
    binder.bind(HdfsConfiguration.class).to(ArcticHdfsConfiguration.class).in(Scopes.SINGLETON);
    binder.bind(HdfsAuthentication.class).to(ArcticHdfsAuthentication.class).in(Scopes.SINGLETON);
    binder.bind(HdfsEnvironment.class).in(Scopes.SINGLETON);
    binder.bind(NamenodeStats.class).in(Scopes.SINGLETON);
    ExportBinder.newExporter(binder).export(NamenodeStats.class).withGeneratedName();
  }
}
