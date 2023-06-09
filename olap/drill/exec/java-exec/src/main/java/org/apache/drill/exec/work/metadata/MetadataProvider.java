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
package org.apache.drill.exec.work.metadata;

import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_TABLE_TYPE;
import static org.apache.drill.exec.store.ischema.InfoSchemaTableType.CATALOGS;
import static org.apache.drill.exec.store.ischema.InfoSchemaTableType.COLUMNS;
import static org.apache.drill.exec.store.ischema.InfoSchemaTableType.SCHEMATA;
import static org.apache.drill.exec.store.ischema.InfoSchemaTableType.TABLES;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.ErrorHelper;
import org.apache.drill.exec.ops.ViewExpansionContext;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.exec.proto.UserProtos.CatalogMetadata;
import org.apache.drill.exec.proto.UserProtos.ColumnMetadata;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsReq;
import org.apache.drill.exec.proto.UserProtos.GetCatalogsResp;
import org.apache.drill.exec.proto.UserProtos.GetColumnsReq;
import org.apache.drill.exec.proto.UserProtos.GetColumnsResp;
import org.apache.drill.exec.proto.UserProtos.GetSchemasReq;
import org.apache.drill.exec.proto.UserProtos.GetSchemasResp;
import org.apache.drill.exec.proto.UserProtos.GetTablesReq;
import org.apache.drill.exec.proto.UserProtos.GetTablesResp;
import org.apache.drill.exec.proto.UserProtos.LikeFilter;
import org.apache.drill.exec.proto.UserProtos.RequestStatus;
import org.apache.drill.exec.proto.UserProtos.RpcType;
import org.apache.drill.exec.proto.UserProtos.SchemaMetadata;
import org.apache.drill.exec.proto.UserProtos.TableMetadata;
import org.apache.drill.exec.rpc.Response;
import org.apache.drill.exec.rpc.ResponseSender;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.exec.store.SchemaConfig.SchemaConfigInfoProvider;
import org.apache.drill.exec.store.SchemaTreeProvider;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter.ConstantExprNode;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter.ExprNode;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter.FieldExprNode;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter.FunctionExprNode;
import org.apache.drill.exec.store.ischema.InfoSchemaTableType;
import org.apache.drill.exec.store.ischema.Records.Catalog;
import org.apache.drill.exec.store.ischema.Records.Column;
import org.apache.drill.exec.store.ischema.Records.Schema;
import org.apache.drill.exec.store.ischema.Records.Table;
import org.apache.drill.exec.store.pojo.PojoRecordReader;

import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ComparisonChain;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Ordering;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains worker {@link Runnable} classes for providing the metadata and related helper methods.
 */
public class MetadataProvider {
  private static final Logger logger = LoggerFactory.getLogger(MetadataProvider.class);

  private static final String IN_FUNCTION = "in";
  // Is this intended? Function name here is lower case of actual name?
  private static final String AND_FUNCTION = FunctionNames.AND.toLowerCase();
  private static final String OR_FUNCTION = FunctionNames.OR.toLowerCase();

  /**
   * @return Runnable that fetches the catalog metadata for given {@link GetCatalogsReq} and sends response at the end.
   */
  public static Runnable catalogs(final UserSession session, final DrillbitContext dContext,
      final GetCatalogsReq req, final ResponseSender responseSender) {
    return new CatalogsProvider(session, dContext, req, responseSender);
  }

  /**
   * @return Runnable that fetches the schema metadata for given {@link GetSchemasReq} and sends response at the end.
   */
  public static Runnable schemas(final UserSession session, final DrillbitContext dContext,
      final GetSchemasReq req, final ResponseSender responseSender) {
    return new SchemasProvider(session, dContext, req, responseSender);
  }

  /**
   * @return Runnable that fetches the table metadata for given {@link GetTablesReq} and sends response at the end.
   */
  public static Runnable tables(final UserSession session, final DrillbitContext dContext,
      final GetTablesReq req, final ResponseSender responseSender) {
    return new TablesProvider(session, dContext, req, responseSender);
  }

  /**
   * @return Runnable that fetches the column metadata for given {@link GetColumnsReq} and sends response at the end.
   */
  public static Runnable columns(final UserSession session, final DrillbitContext dContext,
      final GetColumnsReq req, final ResponseSender responseSender) {
    return new ColumnsProvider(session, dContext, req, responseSender);
  }

  /**
   * Super class for all metadata provider runnable classes.
   */
  private abstract static class MetadataRunnable implements Runnable {
    protected final UserSession session;
    private final ResponseSender responseSender;
    private final DrillbitContext dContext;

    private MetadataRunnable(final UserSession session, final DrillbitContext dContext,
        final ResponseSender responseSender) {
      this.session = Preconditions.checkNotNull(session);
      this.dContext = Preconditions.checkNotNull(dContext);
      this.responseSender = Preconditions.checkNotNull(responseSender);
    }

    @Override
    public void run() {
      try(SchemaTreeProvider schemaTreeProvider = new SchemaTreeProvider(dContext)) {
        responseSender.send(runInternal(session, schemaTreeProvider));
      } catch (final Throwable error) {
        logger.error("Unhandled metadata provider error", error);
      }
    }

    /**
     * @return A {@link Response} message. Response must be returned in any case.
     */
    protected abstract Response runInternal(UserSession session, SchemaTreeProvider schemaProvider);

    public DrillConfig getConfig() {
      return dContext.getConfig();
    }

    public MetastoreRegistry getMetastoreRegistry() {
      return dContext.getMetastoreRegistry();
    }
  }

  /**
   * Runnable that fetches the catalog metadata for given {@link GetCatalogsReq} and sends response at the end.
   */
  private static class CatalogsProvider extends MetadataRunnable {
    private static final Ordering<CatalogMetadata> CATALOGS_ORDERING = new Ordering<CatalogMetadata>() {
      @Override
      public int compare(CatalogMetadata left, CatalogMetadata right) {
        return Ordering.natural().compare(left.getCatalogName(), right.getCatalogName());
      }
    };

    private final GetCatalogsReq req;

    public CatalogsProvider(final UserSession session, final DrillbitContext dContext,
        final GetCatalogsReq req, final ResponseSender responseSender) {
      super(session, dContext, responseSender);
      this.req = Preconditions.checkNotNull(req);
    }

    @Override
    protected Response runInternal(final UserSession session, final SchemaTreeProvider schemaProvider) {
      final GetCatalogsResp.Builder respBuilder = GetCatalogsResp.newBuilder();
      final InfoSchemaFilter filter = createInfoSchemaFilter(
          req.hasCatalogNameFilter() ? req.getCatalogNameFilter() : null, null, null, null, null);

      try {
        final PojoRecordReader<Catalog> records =
            getPojoRecordReader(CATALOGS, filter, getConfig(), schemaProvider, session, getMetastoreRegistry());

        List<CatalogMetadata> metadata = new ArrayList<>();
        for(Catalog c : records) {
          final CatalogMetadata.Builder catBuilder = CatalogMetadata.newBuilder();
          catBuilder.setCatalogName(c.CATALOG_NAME);
          catBuilder.setDescription(c.CATALOG_DESCRIPTION);
          catBuilder.setConnect(c.CATALOG_CONNECT);

          metadata.add(catBuilder.build());
        }

        // Reorder results according to JDBC spec
        Collections.sort(metadata, CATALOGS_ORDERING);

        respBuilder.addAllCatalogs(metadata);
        respBuilder.setStatus(RequestStatus.OK);
      } catch (Throwable e) {
        respBuilder.setStatus(RequestStatus.FAILED);
        respBuilder.setError(createPBError("get catalogs", e));
      } finally {
        return new Response(RpcType.CATALOGS, respBuilder.build());
      }
    }
  }

  private static class SchemasProvider extends MetadataRunnable {
    private static final Ordering<SchemaMetadata> SCHEMAS_ORDERING = new Ordering<SchemaMetadata>() {
      @Override
      public int compare(SchemaMetadata left, SchemaMetadata right) {
        return ComparisonChain.start()
            .compare(left.getCatalogName(), right.getCatalogName())
            .compare(left.getSchemaName(), right.getSchemaName())
            .result();
      };
    };

    private final GetSchemasReq req;

    private SchemasProvider(final UserSession session, final DrillbitContext dContext,
        final GetSchemasReq req, final ResponseSender responseSender) {
      super(session, dContext, responseSender);
      this.req = Preconditions.checkNotNull(req);
    }

    @Override
    protected Response runInternal(final UserSession session, final SchemaTreeProvider schemaProvider) {
      final GetSchemasResp.Builder respBuilder = GetSchemasResp.newBuilder();

      final InfoSchemaFilter filter = createInfoSchemaFilter(
          req.hasCatalogNameFilter() ? req.getCatalogNameFilter() : null,
          req.hasSchemaNameFilter() ? req.getSchemaNameFilter() : null,
          null, null, null);

      try {
        final PojoRecordReader<Schema> records =
            getPojoRecordReader(SCHEMATA, filter, getConfig(), schemaProvider, session, getMetastoreRegistry());

        List<SchemaMetadata> metadata = new ArrayList<>();
        for(Schema s : records) {
          final SchemaMetadata.Builder schemaBuilder = SchemaMetadata.newBuilder();
          schemaBuilder.setCatalogName(s.CATALOG_NAME);
          schemaBuilder.setSchemaName(s.SCHEMA_NAME);
          schemaBuilder.setOwner(s.SCHEMA_OWNER);
          schemaBuilder.setType(s.TYPE);
          schemaBuilder.setMutable(s.IS_MUTABLE);

          metadata.add(schemaBuilder.build());
        }
        // Reorder results according to JDBC spec
        Collections.sort(metadata, SCHEMAS_ORDERING);

        respBuilder.addAllSchemas(metadata);
        respBuilder.setStatus(RequestStatus.OK);
      } catch (Throwable e) {
        respBuilder.setStatus(RequestStatus.FAILED);
        respBuilder.setError(createPBError("get schemas", e));
      } finally {
        return new Response(RpcType.SCHEMAS, respBuilder.build());
      }
    }
  }

  private static class TablesProvider extends MetadataRunnable {
    private static final Ordering<TableMetadata> TABLES_ORDERING = new Ordering<TableMetadata>() {
      @Override
      public int compare(TableMetadata left, TableMetadata right) {
        return ComparisonChain.start()
            .compare(left.getType(), right.getType())
            .compare(left.getCatalogName(), right.getCatalogName())
            .compare(left.getSchemaName(), right.getSchemaName())
            .compare(left.getTableName(), right.getTableName())
            .result();
      }
    };
    private final GetTablesReq req;

    private TablesProvider(final UserSession session, final DrillbitContext dContext,
        final GetTablesReq req, final ResponseSender responseSender) {
      super(session, dContext, responseSender);
      this.req = Preconditions.checkNotNull(req);
    }

    @Override
    protected Response runInternal(final UserSession session, final SchemaTreeProvider schemaProvider) {
      final GetTablesResp.Builder respBuilder = GetTablesResp.newBuilder();

      final InfoSchemaFilter filter = createInfoSchemaFilter(
          req.hasCatalogNameFilter() ? req.getCatalogNameFilter() : null,
          req.hasSchemaNameFilter() ? req.getSchemaNameFilter() : null,
          req.hasTableNameFilter() ? req.getTableNameFilter() : null,
          req.getTableTypeFilterCount() != 0 ? req.getTableTypeFilterList() : null,
          null);

      try {
        final PojoRecordReader<Table> records =
            getPojoRecordReader(TABLES, filter, getConfig(), schemaProvider, session, getMetastoreRegistry());

        List<TableMetadata> metadata = new ArrayList<>();
        for(Table t : records) {
          final TableMetadata.Builder tableBuilder = TableMetadata.newBuilder();
          tableBuilder.setCatalogName(t.TABLE_CATALOG);
          tableBuilder.setSchemaName(t.TABLE_SCHEMA);
          tableBuilder.setTableName(t.TABLE_NAME);
          tableBuilder.setType(t.TABLE_TYPE);

          metadata.add(tableBuilder.build());
        }

        // Reorder results according to JDBC/ODBC spec
        Collections.sort(metadata, TABLES_ORDERING);

        respBuilder.addAllTables(metadata);
        respBuilder.setStatus(RequestStatus.OK);
      } catch (Throwable e) {
        respBuilder.setStatus(RequestStatus.FAILED);
        respBuilder.setError(createPBError("get tables", e));
      } finally {
        return new Response(RpcType.TABLES, respBuilder.build());
      }
    }
  }

  private static class ColumnsProvider extends MetadataRunnable {
    private static final Ordering<ColumnMetadata> COLUMNS_ORDERING = new Ordering<ColumnMetadata>() {
      @Override
      public int compare(ColumnMetadata left, ColumnMetadata right) {
        return ComparisonChain.start()
            .compare(left.getCatalogName(), right.getCatalogName())
            .compare(left.getSchemaName(), right.getSchemaName())
            .compare(left.getTableName(), right.getTableName())
            .compare(left.getOrdinalPosition(), right.getOrdinalPosition())
            .result();
      }
    };

    private final GetColumnsReq req;

    private ColumnsProvider(final UserSession session, final DrillbitContext dContext,
        final GetColumnsReq req, final ResponseSender responseSender) {
      super(session, dContext, responseSender);
      this.req = Preconditions.checkNotNull(req);
    }

    @Override
    protected Response runInternal(final UserSession session, final SchemaTreeProvider schemaProvider) {
      final GetColumnsResp.Builder respBuilder = GetColumnsResp.newBuilder();

      final InfoSchemaFilter filter = createInfoSchemaFilter(
          req.hasCatalogNameFilter() ? req.getCatalogNameFilter() : null,
          req.hasSchemaNameFilter() ? req.getSchemaNameFilter() : null,
          req.hasTableNameFilter() ? req.getTableNameFilter() : null,
          null, req.hasColumnNameFilter() ? req.getColumnNameFilter() : null
      );

      try {
        final PojoRecordReader<Column> records =
            getPojoRecordReader(COLUMNS, filter, getConfig(), schemaProvider, session, getMetastoreRegistry());

        List<ColumnMetadata> metadata = new ArrayList<>();
        for(Column c : records) {
          final ColumnMetadata.Builder columnBuilder = ColumnMetadata.newBuilder();
          columnBuilder.setCatalogName(c.TABLE_CATALOG);
          columnBuilder.setSchemaName(c.TABLE_SCHEMA);
          columnBuilder.setTableName(c.TABLE_NAME);
          columnBuilder.setColumnName(c.COLUMN_NAME);
          columnBuilder.setOrdinalPosition(c.ORDINAL_POSITION);
          if (c.COLUMN_DEFAULT != null) {
            columnBuilder.setDefaultValue(c.COLUMN_DEFAULT);
          }

          if ("YES".equalsIgnoreCase(c.IS_NULLABLE)) {
            columnBuilder.setIsNullable(true);
          } else {
            columnBuilder.setIsNullable(false);
          }
          columnBuilder.setDataType(c.DATA_TYPE);
          if (c.CHARACTER_MAXIMUM_LENGTH != null) {
            columnBuilder.setCharMaxLength(c.CHARACTER_MAXIMUM_LENGTH);
          }

          if (c.CHARACTER_OCTET_LENGTH != null) {
            columnBuilder.setCharOctetLength(c.CHARACTER_OCTET_LENGTH);
          }

          if (c.NUMERIC_SCALE != null) {
            columnBuilder.setNumericScale(c.NUMERIC_SCALE);
          }

          if (c.NUMERIC_PRECISION != null) {
            columnBuilder.setNumericPrecision(c.NUMERIC_PRECISION);
          }

          if (c.NUMERIC_PRECISION_RADIX != null) {
            columnBuilder.setNumericPrecisionRadix(c.NUMERIC_PRECISION_RADIX);
          }

          if (c.DATETIME_PRECISION != null) {
            columnBuilder.setDateTimePrecision(c.DATETIME_PRECISION);
          }

          if (c.INTERVAL_TYPE != null) {
            columnBuilder.setIntervalType(c.INTERVAL_TYPE);
          }

          if (c.INTERVAL_PRECISION != null) {
            columnBuilder.setIntervalPrecision(c.INTERVAL_PRECISION);
          }

          if (c.COLUMN_SIZE != null) {
            columnBuilder.setColumnSize(c.COLUMN_SIZE);
          }

          metadata.add(columnBuilder.build());
        }

        // Reorder results according to JDBC/ODBC spec
        Collections.sort(metadata, COLUMNS_ORDERING);

        respBuilder.addAllColumns(metadata);
        respBuilder.setStatus(RequestStatus.OK);
      } catch (Throwable e) {
        respBuilder.setStatus(RequestStatus.FAILED);
        respBuilder.setError(createPBError("get columns", e));
      } finally {
        return new Response(RpcType.COLUMNS, respBuilder.build());
      }
    }
  }

  /**
   * Helper method to create a {@link InfoSchemaFilter} that combines the given filters with an AND.
   *
   * @param catalogNameFilter Optional filter on <code>catalog name</code>
   * @param schemaNameFilter Optional filter on <code>schema name</code>
   * @param tableNameFilter Optional filter on <code>table name</code>
   * @param tableTypeFilter Optional filter on <code>table type</code>
   * @param columnNameFilter Optional filter on <code>column name</code>
   * @return information schema filter
   */
  private static InfoSchemaFilter createInfoSchemaFilter(LikeFilter catalogNameFilter,
                                                         LikeFilter schemaNameFilter,
                                                         LikeFilter tableNameFilter,
                                                         List<String> tableTypeFilter,
                                                         LikeFilter columnNameFilter) {

    FunctionExprNode exprNode = createLikeFunctionExprNode(CATS_COL_CATALOG_NAME,  catalogNameFilter);

    // schema names are case insensitive in Drill and stored in lower case
    // convert like filter condition elements to lower case
    if (schemaNameFilter != null) {
      LikeFilter.Builder builder = LikeFilter.newBuilder();
      if (schemaNameFilter.hasPattern()) {
        builder.setPattern(schemaNameFilter.getPattern().toLowerCase());
      }

      if (schemaNameFilter.hasEscape()) {
        builder.setEscape(schemaNameFilter.getEscape().toLowerCase());
      }
      schemaNameFilter = builder.build();
    }

    exprNode = combineFunctions(AND_FUNCTION,
        exprNode,
        combineFunctions(OR_FUNCTION,
            createLikeFunctionExprNode(SHRD_COL_TABLE_SCHEMA, schemaNameFilter),
            createLikeFunctionExprNode(SCHS_COL_SCHEMA_NAME, schemaNameFilter)
        )
    );

    exprNode = combineFunctions(AND_FUNCTION,
        exprNode,
        createLikeFunctionExprNode(SHRD_COL_TABLE_NAME, tableNameFilter)
    );

    exprNode = combineFunctions(AND_FUNCTION,
        exprNode,
        createInFunctionExprNode(TBLS_COL_TABLE_TYPE, tableTypeFilter)
        );

    exprNode = combineFunctions(AND_FUNCTION,
        exprNode,
        createLikeFunctionExprNode(COLS_COL_COLUMN_NAME, columnNameFilter)
    );

    return exprNode != null ? new InfoSchemaFilter(exprNode) : null;
  }

  /**
   * Helper method to create {@link FunctionExprNode} from {@link LikeFilter}.
   * @param fieldName Name of the filed on which the like expression is applied.
   * @param likeFilter
   * @return {@link FunctionExprNode} for given arguments. Null if the <code>likeFilter</code> is null.
   */
  private static FunctionExprNode createLikeFunctionExprNode(String fieldName, LikeFilter likeFilter) {
    if (likeFilter == null) {
      return null;
    }

    return new FunctionExprNode(FunctionNames.LIKE,
        likeFilter.hasEscape() ?
            ImmutableList.of(
                new FieldExprNode(fieldName),
                new ConstantExprNode(likeFilter.getPattern()),
                new ConstantExprNode(likeFilter.getEscape())) :
            ImmutableList.of(
                new FieldExprNode(fieldName),
                new ConstantExprNode(likeFilter.getPattern()))
    );
  }

  /**
   * Helper method to create {@link FunctionExprNode} from {@code List<String>}.
   * @param fieldName Name of the filed on which the like expression is applied.
   * @param valuesFilter a list of values
   * @return {@link FunctionExprNode} for given arguments. Null if the <code>valuesFilter</code> is null.
   */
  private static FunctionExprNode createInFunctionExprNode(String fieldName, List<String> valuesFilter) {
    if (valuesFilter == null) {
      return null;
    }

    ImmutableList.Builder<ExprNode> nodes = ImmutableList.builder();
    nodes.add(new FieldExprNode(fieldName));
    for(String type: valuesFilter) {
      nodes.add(new ConstantExprNode(type));
    }

    return new FunctionExprNode(IN_FUNCTION, nodes.build());
  }

  /**
   * Helper method to combine two {@link FunctionExprNode}s with a given <code>functionName</code>. If one of them is
   * null, other one is returned as it is.
   */
  private static FunctionExprNode combineFunctions(final String functionName,
      final FunctionExprNode func1, final FunctionExprNode func2) {
    if (func1 == null) {
      return func2;
    }

    if (func2 == null) {
      return func1;
    }

    return new FunctionExprNode(functionName, ImmutableList.<ExprNode>of(func1, func2));
  }

  /**
   * Helper method to create a {@link PojoRecordReader} for given arguments.
   * @param tableType
   * @param filter
   * @param provider
   * @param userSession
   * @return
   */
  private static <S> PojoRecordReader<S> getPojoRecordReader(final InfoSchemaTableType tableType, final InfoSchemaFilter filter, final DrillConfig config,
      final SchemaTreeProvider provider, final UserSession userSession, final MetastoreRegistry metastoreRegistry) {
    final SchemaPlus rootSchema =
        provider.createRootSchema(userSession.getCredentials().getUserName(), newSchemaConfigInfoProvider(config, userSession, provider));
    return tableType.getRecordReader(rootSchema, filter, userSession.getOptions(), metastoreRegistry);
  }

  /**
   * Helper method to create a {@link SchemaConfigInfoProvider} instance for metadata purposes.
   * @param session
   * @param schemaTreeProvider
   * @return
   */
  private static SchemaConfigInfoProvider newSchemaConfigInfoProvider(final DrillConfig config, final UserSession session, final SchemaTreeProvider schemaTreeProvider) {
    return new SchemaConfigInfoProvider() {
      private final ViewExpansionContext viewExpansionContext = new ViewExpansionContext(config, this);

      @Override
      public ViewExpansionContext getViewExpansionContext() {
        return viewExpansionContext;
      }

      @Override
      public SchemaPlus getRootSchema(String userName) {
        return schemaTreeProvider.createRootSchema(userName, this);
      }

      @Override
      public OptionValue getOption(String optionKey) {
        return session.getOptions().getOption(optionKey);
      }

      @Override
      public String getQueryUserName() {
        return session.getCredentials().getUserName();
      }
    };
  }

  /**
   * Helper method to create {@link DrillPBError} for client response message.
   * @param failedFunction Brief description of the failed function.
   * @param ex Exception thrown
   * @return
   */
  static DrillPBError createPBError(final String failedFunction, final Throwable ex) {
    final String errorId = UUID.randomUUID().toString();
    logger.error("Failed to {}. ErrorId: {}", failedFunction, errorId, ex);

    final DrillPBError.Builder builder = DrillPBError.newBuilder();
    builder.setErrorType(ErrorType.SYSTEM); // Metadata requests shouldn't cause any user errors
    builder.setErrorId(errorId);
    if (ex.getMessage() != null) {
      builder.setMessage(ex.getMessage());
    }

    builder.setException(ErrorHelper.getWrapper(ex));

    return builder.build();
  }
}
