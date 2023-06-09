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

import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlShowFiles;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory.WorkspaceSchema;
import org.apache.drill.exec.store.ischema.Records;
import org.apache.drill.exec.util.FileSystemUtil;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class ShowFilesHandler extends DefaultSqlHandler {

  private static final Logger logger = getLogger(ShowFilesHandler.class);

  public ShowFilesHandler(SqlHandlerConfig config) {
    super(config);
  }

  @Override
  public PhysicalPlan getPlan(SqlNode sqlNode) throws ForemanSetupException {
    SchemaPlus defaultSchema = config.getConverter().getDefaultSchema();
    SchemaPlus drillSchema = defaultSchema;
    SqlShowFiles showFiles = unwrap(sqlNode, SqlShowFiles.class);
    SqlIdentifier from = showFiles.getDb();
    String fromDir = null;

    // Show files can be used without from clause, in which case we display the files in the default schema
    if (from != null) {
      // We are not sure if the full from clause is just the schema or includes table name,
      // first try to see if the full path specified is a schema
      drillSchema = SchemaUtilites.findSchema(defaultSchema, from.names);
      if (drillSchema == null) {
        // Entire from clause is not a schema, try to obtain the schema without the last part of the specified clause.
        drillSchema = SchemaUtilites.findSchema(defaultSchema, from.names.subList(0, from.names.size() - 1));
        // Listing for specific directory: show files in dfs.tmp.specific_directory
        fromDir = from.names.get((from.names.size() - 1));
      }

      if (drillSchema == null) {
        throw UserException.validationError()
            .message("Invalid FROM/IN clause [%s]", from.toString())
            .build(logger);
      }
    }

    WorkspaceSchema wsSchema;
    try {
      wsSchema = (WorkspaceSchema) drillSchema.unwrap(AbstractSchema.class).getDefaultSchema();
    } catch (ClassCastException e) {
      throw UserException.validationError()
          .message("SHOW FILES is supported in workspace type schema only. Schema [%s] is not a workspace schema.",
              SchemaUtilites.getSchemaPath(drillSchema))
          .build(logger);
    }

    Path endPath = fromDir == null ? new Path(wsSchema.getDefaultLocation()) : new Path(wsSchema.getDefaultLocation(), fromDir);
    // add URI to the path to ensure that directory objects are skipped (see S3AFileSystem.listStatus method)
    Path path = new Path(wsSchema.getFS().getUri().toString(), endPath);
    List<ShowFilesCommandResult> records = FileSystemUtil.listAllSafe(wsSchema.getFS(), path, false).stream()
        // use ShowFilesCommandResult for backward compatibility
        .map(fileStatus -> new ShowFilesCommandResult(new Records.File(wsSchema.getFullSchemaName(), wsSchema, fileStatus)))
        .collect(Collectors.toList());

    return DirectPlan.createDirectPlan(context.getCurrentEndpoint(), records, ShowFilesCommandResult.class);
  }

  /**
   * Original show files command result holder is used as wrapper over new {@link Records.File} holder
   * to maintain backward compatibility with ODBC driver etc. in column names and types.
   */
  public static class ShowFilesCommandResult {

    public final String name;
    public final boolean isDirectory;
    public final boolean isFile;
    public final long length;
    public final String owner;
    public final String group;
    public final String permissions;
    public final Timestamp accessTime;
    public final Timestamp modificationTime;

    public ShowFilesCommandResult(Records.File fileRecord) {
      this.name = fileRecord.FILE_NAME;
      this.isDirectory = fileRecord.IS_DIRECTORY;
      this.isFile = fileRecord.IS_FILE;
      this.length = fileRecord.LENGTH;
      this.owner = fileRecord.OWNER;
      this.group = fileRecord.GROUP;
      this.permissions = fileRecord.PERMISSION;
      this.accessTime = fileRecord.ACCESS_TIME;
      this.modificationTime = fileRecord.MODIFICATION_TIME;
    }

  }

}
