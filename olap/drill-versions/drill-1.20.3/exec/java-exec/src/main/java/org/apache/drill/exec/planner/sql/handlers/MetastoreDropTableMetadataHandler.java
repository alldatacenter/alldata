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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlDropTableMetadata;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.components.tables.Tables;
import org.apache.drill.metastore.exceptions.MetastoreException;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.operate.Delete;
import org.apache.parquet.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MetastoreDropTableMetadataHandler extends DefaultSqlHandler {
  private static final Logger logger = LoggerFactory.getLogger(MetastoreDropTableMetadataHandler.class);

  public MetastoreDropTableMetadataHandler(SqlHandlerConfig config, Pointer<String> textPlan) {
    super(config, textPlan);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    if (!context.getOptions().getOption(ExecConstants.METASTORE_ENABLED_VALIDATOR)) {
      throw UserException.validationError()
          .message("Running ANALYZE TABLE DROP command when Metastore is disabled (`metastore.enabled` is set to false)")
          .build(logger);
    }

    SqlDropTableMetadata dropTableMetadata = unwrap(sqlNode, SqlDropTableMetadata.class);

    AbstractSchema drillSchema = SchemaUtilites.resolveToDrillSchema(
        config.getConverter().getDefaultSchema(), dropTableMetadata.getSchemaPath());

    List<String> schemaPath = drillSchema.getSchemaPath();
    String pluginName = schemaPath.get(0);
    String workspaceName = Strings.join(schemaPath.subList(1, schemaPath.size()), AbstractSchema.SCHEMA_SEPARATOR);

    TableInfo tableInfo = TableInfo.builder()
        .name(dropTableMetadata.getName())
        .storagePlugin(pluginName)
        .workspace(workspaceName)
        .build();

    try {
      Tables tables = context.getMetastoreRegistry().get().tables();

      MetastoreTableInfo metastoreTableInfo = tables.basicRequests()
          .metastoreTableInfo(tableInfo);

      if (!metastoreTableInfo.isExists()) {
        if (dropTableMetadata.checkMetadataExistence()) {
          throw UserException.validationError()
              .message("Metadata for table [%s] not found.", dropTableMetadata.getName())
              .build(logger);
        }
        return DirectPlan.createDirectPlan(context, false,
            String.format("Metadata for table [%s] does not exist.", dropTableMetadata.getName()));
      }

      tables.modify()
        .delete(Delete.builder()
          .metadataType(MetadataType.ALL)
          .filter(tableInfo.toFilter())
          .build())
        .execute();
    } catch (MetastoreException e) {
      logger.error("Error when dropping metadata for table {}", dropTableMetadata.getName(), e);
      return DirectPlan.createDirectPlan(context, false, e.getMessage());
    }

    return DirectPlan.createDirectPlan(context, true,
        String.format("Metadata for table [%s] dropped.", dropTableMetadata.getName()));
  }
}
