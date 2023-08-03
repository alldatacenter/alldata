package com.netease.arctic.server;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.server.catalog.InternalCatalog;
import com.netease.arctic.server.catalog.ServerCatalog;
import com.netease.arctic.server.exception.ObjectNotExistsException;
import com.netease.arctic.server.iceberg.InternalTableOperations;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableService;
import com.netease.arctic.server.utils.IcebergTableUtil;
import com.netease.arctic.utils.CatalogUtil;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import io.javalin.plugin.json.JavalinJackson;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.TableMetadata;
import org.apache.iceberg.TableOperations;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.exceptions.CommitFailedException;
import org.apache.iceberg.exceptions.NoSuchNamespaceException;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.rest.RESTResponse;
import org.apache.iceberg.rest.RESTSerializers;
import org.apache.iceberg.rest.requests.CreateNamespaceRequest;
import org.apache.iceberg.rest.requests.CreateTableRequest;
import org.apache.iceberg.rest.requests.UpdateTableRequest;
import org.apache.iceberg.rest.responses.ConfigResponse;
import org.apache.iceberg.rest.responses.CreateNamespaceResponse;
import org.apache.iceberg.rest.responses.ErrorResponse;
import org.apache.iceberg.rest.responses.GetNamespaceResponse;
import org.apache.iceberg.rest.responses.ListNamespacesResponse;
import org.apache.iceberg.rest.responses.ListTablesResponse;
import org.apache.iceberg.rest.responses.LoadTableResponse;
import org.apache.iceberg.util.LocationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.netease.arctic.server.utils.IcebergTableUtil.loadIcebergTableMetadata;
import static com.netease.arctic.server.utils.IcebergTableUtil.newIcebergFileIo;
import static io.javalin.apibuilder.ApiBuilder.delete;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.head;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;

public class IcebergRestCatalogService extends PersistentBase {

  public static final String ICEBERG_METRIC_LOGGER = "iceberg.metric";

  private static final Logger LOG = LoggerFactory.getLogger(IcebergRestCatalogService.class);
  private static final Logger METRIC_LOG = LoggerFactory.getLogger(ICEBERG_METRIC_LOGGER);

  public static final String ICEBERG_REST_API_PREFIX = "/api/iceberg/rest";

  private static final String ICEBERG_CATALOG_PREFIX_KEY = "prefix";

  private static final Set<String> catalogPropertiesNotReturned = Collections.emptySet();

  private static final Set<String> catalogPropertiesOverwrite = Collections.unmodifiableSet(
      Sets.newHashSet(CatalogMetaProperties.KEY_WAREHOUSE)
  );

  private final JavalinJackson jsonMapper;
  private final ObjectMapper objectMapper;

  private final TableService tableService;

  public IcebergRestCatalogService(TableService tableService) {
    this.tableService = tableService;
    this.objectMapper = jsonMapper();
    this.jsonMapper = new JavalinJackson(objectMapper);
  }

  public EndpointGroup endpoints() {
    return () -> {
      // for iceberg rest catalog api
      path(ICEBERG_REST_API_PREFIX, () -> {
        get("/v1/config", this::getCatalogConfig);
        get("/v1/catalogs/{catalog}/namespaces", this::listNamespaces);
        post("/v1/catalogs/{catalog}/namespaces", this::createNamespace);
        get("/v1/catalogs/{catalog}/namespaces/{namespace}", this::getNamespace);
        delete("/v1/catalogs/{catalog}/namespaces/{namespace}", this::dropNamespace);
        post("/v1/catalogs/{catalog}/namespaces/{namespace}", this::setNamespaceProperties);
        get("/v1/catalogs/{catalog}/namespaces/{namespace}/tables", this::listTablesInNamespace);
        post("/v1/catalogs/{catalog}/namespaces/{namespace}/tables", this::createTable);
        get("/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}", this::loadTable);
        post("/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}", this::commitTable);
        delete("/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}", this::deleteTable);
        head("/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}", this::tableExists);
        post("/v1/catalogs/{catalog}/tables/rename", this::renameTable);
        post("/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}/metrics", this::metricReport);
      });
    };
  }


  public boolean needHandleException(Context ctx) {
    return ctx.req.getRequestURI().startsWith(ICEBERG_REST_API_PREFIX);
  }

  public void handleException(Exception e, Context ctx) {
    IcebergRestErrorCode code = IcebergRestErrorCode.exceptionToCode(e);
    ErrorResponse response = ErrorResponse.builder()
        .responseCode(code.code)
        .withType(e.getClass().getSimpleName())
        .withMessage(e.getMessage())
        .build();
    ctx.res.setStatus(code.code);
    jsonResponse(ctx, response);
    if (code.code >= 500) {
      LOG.warn("InternalServer Error", e);
    }
  }


  /**
   * GET PREFIX/v1/config?warehouse={warehouse}
   */
  public void getCatalogConfig(Context ctx) {
    String warehouse = ctx.req.getParameter("warehouse");
    Preconditions.checkNotNull(warehouse, "lack required params: warehouse");
    InternalCatalog catalog = getCatalog(warehouse);
    Map<String, String> properties = Maps.newHashMap();
    Map<String, String> overwrites = Maps.newHashMap();
    catalog.getMetadata().getCatalogProperties().forEach((k, v) -> {
      if (!catalogPropertiesNotReturned.contains(k)) {
        if (catalogPropertiesOverwrite.contains(k)) {
          overwrites.put(k, v);
        } else {
          properties.put(k, v);
        }
      }
    });
    overwrites.put(ICEBERG_CATALOG_PREFIX_KEY, "catalogs/" + warehouse);
    ConfigResponse configResponse = ConfigResponse.builder()
        .withDefaults(properties)
        .withOverrides(overwrites)
        .build();
    jsonResponse(ctx, configResponse);
  }


  /**
   * GET PREFIX/{catalog}/v1/namespaces
   */
  public void listNamespaces(Context ctx) {
    handleCatalog(ctx, catalog -> {
      String ns = ctx.req.getParameter("parent");
      checkUnsupported(ns == null,
          "The catalog doesn't support multi-level namespaces");
      List<Namespace> nsLists = catalog.listDatabases()
          .stream().map(Namespace::of)
          .collect(Collectors.toList());
      return ListNamespacesResponse.builder()
          .addAll(nsLists)
          .build();
    });

  }

  /**
   * POST PREFIX/{catalog}/v1/namespaces
   */
  public void createNamespace(Context ctx) {
    handleCatalog(ctx, catalog -> {
      CreateNamespaceRequest request = bodyAsClass(ctx, CreateNamespaceRequest.class);
      Namespace ns = request.namespace();
      checkUnsupported(ns.length() == 1,
          "multi-level namespace is not supported now");
      String database = ns.level(0);
      checkAlreadyExists(!catalog.exist(database), "Database", database);
      catalog.createDatabase(database);
      return CreateNamespaceResponse.builder().withNamespace(
          Namespace.of(database)
      ).build();
    });
  }

  /**
   * GET PREFIX/v1/catalogs/{catalog}/namespaces/{namespaces}
   */
  public void getNamespace(Context ctx) {
    handleNamespace(ctx, (catalog, database) -> {
      if (catalog.exist(database)) {
        return GetNamespaceResponse.builder()
            .withNamespace(Namespace.of(database))
            .build();
      }
      throw new NoSuchNamespaceException("database:" + database + " doesn't exist.");
    });
  }

  /**
   * DELETE PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}
   */
  public void dropNamespace(Context ctx) {
    String catalog = ctx.pathParam("catalog");
    String ns = ctx.pathParam("namespace");
    Preconditions.checkNotNull(ns, "namespace is null");
    checkUnsupported(!ns.contains("."), "multi-level namespace is not supported");
    InternalCatalog internalCatalog = getCatalog(catalog);
    internalCatalog.dropDatabase(ns);
  }


  /**
   * POST PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/properties
   */
  public void setNamespaceProperties(Context ctx) {
    throw new UnsupportedOperationException("namespace properties is not supported");
  }

  /**
   * GET PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables
   */
  public void listTablesInNamespace(Context ctx) {
    handleNamespace(ctx, (catalog, database) -> {
      checkDatabaseExist(catalog.exist(database), database);
      List<TableIdentifier> tableIdentifiers = catalog.listTables(database).stream()
          .map(i -> TableIdentifier.of(database, i.getTableName()))
          .collect(Collectors.toList());

      return ListTablesResponse.builder()
          .addAll(tableIdentifiers)
          .build();
    });
  }

  /**
   * POST PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables
   */
  public void createTable(Context ctx) {
    handleNamespace(ctx, (catalog, database) -> {
      checkDatabaseExist(catalog.exist(database), database);

      CreateTableRequest request = bodyAsClass(ctx, CreateTableRequest.class);
      String tableName = request.name();
      checkAlreadyExists(!catalog.exist(database, tableName),
          "Table", database + "." + tableName);
      String location = request.location();
      if (StringUtils.isBlank(location)) {
        String warehouse = catalog.getMetadata().getCatalogProperties().get(CatalogMetaProperties.KEY_WAREHOUSE);
        Preconditions.checkState(StringUtils.isNotBlank(warehouse),
            "catalog warehouse is not configured");
        warehouse = LocationUtil.stripTrailingSlash(warehouse);
        location = warehouse + "/" + database + "/" + tableName;
      } else {
        location = LocationUtil.stripTrailingSlash(location);
      }
      PartitionSpec spec = request.spec();
      SortOrder sortOrder = request.writeOrder();
      TableMetadata tableMetadata = TableMetadata.newTableMetadata(
          request.schema(),
          spec != null ? spec : PartitionSpec.unpartitioned(),
          sortOrder != null ? sortOrder : SortOrder.unsorted(),
          location, request.properties()
      );
      ServerTableIdentifier identifier = ServerTableIdentifier.of(catalog.name(), database, tableName);

      String newMetadataFileLocation = IcebergTableUtil.genNewMetadataFileLocation(null, tableMetadata);
      FileIO io = newIcebergFileIo(catalog.getMetadata());
      try {
        com.netease.arctic.server.table.TableMetadata amsTableMeta = IcebergTableUtil.createTableInternal(
            identifier, catalog.getMetadata(), tableMetadata, newMetadataFileLocation, io
        );
        tableService.createTable(catalog.name(), amsTableMeta);
        TableMetadata current = loadIcebergTableMetadata(io, amsTableMeta);
        return LoadTableResponse.builder()
            .withTableMetadata(current)
            .build();
      } catch (RuntimeException e) {
        io.deleteFile(newMetadataFileLocation);
        throw e;
      } finally {
        io.close();
      }
    });
  }

  /**
   * GET PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}
   */
  public void loadTable(Context ctx) {
    handleTable(ctx, (catalog, tableMeta) -> {
      TableMetadata tableMetadata = null;
      try (FileIO io = newIcebergFileIo(catalog.getMetadata())) {
        tableMetadata = IcebergTableUtil.loadIcebergTableMetadata(io, tableMeta);
      }
      if (tableMetadata == null) {
        throw new NoSuchTableException("failed to load table from metadata file.");
      }
      return LoadTableResponse.builder()
          .withTableMetadata(tableMetadata)
          .build();

    });
  }

  /**
   * POST PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}
   */
  public void commitTable(Context ctx) {
    handleTable(ctx, (catalog, tableMeta) -> {
      UpdateTableRequest request = bodyAsClass(ctx, UpdateTableRequest.class);
      try (FileIO io = newIcebergFileIo(catalog.getMetadata())) {
        TableOperations ops = InternalTableOperations.buildForLoad(tableMeta, io);
        TableMetadata base = ops.current();
        if (base == null) {
          throw new CommitFailedException("table metadata lost.");
        }

        TableMetadata.Builder builder = TableMetadata.buildFrom(base);
        request.requirements().forEach(r -> r.validate(base));
        request.updates().forEach(u -> u.applyTo(builder));
        TableMetadata newMetadata = builder.build();

        ops.commit(base, newMetadata);
        TableMetadata current = ops.current();
        return LoadTableResponse.builder()
            .withTableMetadata(current)
            .build();
      }
    });
  }


  /**
   * DELETE PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}
   */
  public void deleteTable(Context ctx) {
    handleTable(ctx, (catalog, tableMetadata) -> {
      boolean purge = Boolean.parseBoolean(
          Optional.ofNullable(ctx.req.getParameter("purgeRequested")).orElse("false"));
      TableMetadata current = null;
      try (FileIO io = newIcebergFileIo(catalog.getMetadata())) {
        try {
          current = IcebergTableUtil.loadIcebergTableMetadata(io, tableMetadata);
        } catch (Exception e) {
          LOG.warn("failed to load iceberg table metadata, metadata file maybe lost: " + e.getMessage());
        }

        tableService.dropTableMetadata(
            tableMetadata.getTableIdentifier().getIdentifier(), true);
        if (purge && current != null) {
          org.apache.iceberg.CatalogUtil.dropTableData(io, current);
        }
      }


      ctx.status(HttpCode.NO_CONTENT);
      return null;
    });
  }


  /**
   * HEAD PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}
   */
  public void tableExists(Context ctx) {
    handleTable(ctx, ((catalog, tableMeta) -> null));
  }


  /**
   * POST PREFIX/v1/catalogs/{catalog}/tables/rename
   */
  public void renameTable(Context ctx) {
    throw new UnsupportedOperationException("rename is not supported now.");
  }


  /**
   * POST PREFIX/v1/catalogs/{catalog}/namespaces/{namespace}/tables/{table}/metrics
   */
  public void metricReport(Context ctx) {
    handleTable(ctx, (catalog, tableMeta) -> {
      String bodyJson = ctx.body();
      String database = tableMeta.getTableIdentifier().getDatabase();
      String table = tableMeta.getTableIdentifier().getTableName();
      TypeReference<Map<String, Object>> mapTypeReference = new TypeReference<Map<String, Object>>() {
      };
      try {
        Map<String, Object> report = objectMapper.readValue(bodyJson, mapTypeReference);
        report.put("identifier", database + "." + table);
        report.put("catalog", catalog.name());
        String metricJson = objectMapper.writeValueAsString(report);
        METRIC_LOG.info(metricJson);
      } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
      }
      return null;
    });
  }


  private <T> T bodyAsClass(Context ctx, Class<T> clz) {
    return jsonMapper.fromJsonString(ctx.body(), clz);
  }

  private void jsonResponse(Context ctx, RESTResponse rsp) {
    ctx.contentType(ContentType.APPLICATION_JSON)
        .result(jsonMapper.toJsonString(rsp));
  }

  private void handleCatalog(Context ctx, Function<InternalCatalog, ? extends RESTResponse> handler) {
    String catalog = ctx.pathParam("catalog");
    Preconditions.checkNotNull(catalog, "lack require path params: catalog");
    InternalCatalog internalCatalog = getCatalog(catalog);

    RESTResponse r = handler.apply(internalCatalog);
    if (r != null) {
      jsonResponse(ctx, r);
    }
  }


  private void handleNamespace(Context ctx, BiFunction<InternalCatalog, String, ? extends RESTResponse> handler) {
    handleCatalog(ctx, catalog -> {
      String ns = ctx.pathParam("namespace");
      Preconditions.checkNotNull(ns, "namespace is null");
      checkUnsupported(!ns.contains("."), "multi-level namespace is not supported");
      return handler.apply(catalog, ns);
    });
  }


  private void handleTable(
      Context ctx,
      BiFunction<InternalCatalog, com.netease.arctic.server.table.TableMetadata, ? extends RESTResponse> handler) {
    handleNamespace(ctx, (catalog, database) -> {
      checkDatabaseExist(catalog.exist(database), database);

      String tableName = ctx.pathParam("table");
      Preconditions.checkNotNull(tableName, "table name is null");
      com.netease.arctic.server.table.TableMetadata metadata = tableService.loadTableMetadata(
          com.netease.arctic.table.TableIdentifier.of(catalog.name(), database, tableName).buildTableIdentifier()
      );
      Preconditions.checkArgument(metadata.getFormat() == TableFormat.ICEBERG,
          "it's not an iceberg table");
      return handler.apply(catalog, metadata);
    });
  }

  private InternalCatalog getCatalog(String catalog) {
    Preconditions.checkNotNull(catalog, "lack required path variables: catalog");
    ServerCatalog internalCatalog = tableService.getServerCatalog(catalog);
    Preconditions.checkArgument(
        internalCatalog instanceof InternalCatalog,
        "The catalog is not an iceberg rest catalog"
    );
    Set<TableFormat> tableFormats = CatalogUtil.tableFormats(internalCatalog.getMetadata());
    Preconditions.checkArgument(
        tableFormats.size() == 1 && tableFormats.contains(TableFormat.ICEBERG),
        "The catalog is not an iceberg rest catalog"
    );
    return (InternalCatalog) internalCatalog;
  }

  private static void checkUnsupported(boolean condition, String message) {
    if (!condition) {
      throw new UnsupportedOperationException(message);
    }
  }

  private static void checkDatabaseExist(boolean checkCondition, String database) {
    if (!checkCondition) {
      throw new NoSuchNamespaceException("Database: " + database + " doesn't exists");
    }
  }

  private static void checkAlreadyExists(boolean checkCondition, String resourceType, String object) {
    if (!checkCondition) {
      throw new AlreadyExistsException(resourceType + ": " + object + " already exists.");
    }
  }

  private ObjectMapper jsonMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.setPropertyNamingStrategy(new PropertyNamingStrategy.KebabCaseStrategy());
    RESTSerializers.registerAll(mapper);
    return mapper;
  }

  enum IcebergRestErrorCode {
    BadRequest(400),
    NotAuthorized(401),
    Forbidden(403),
    UnsupportedOperation(406),
    AuthenticationTimeout(419),
    NotFound(404),
    Conflict(409),
    InternalServerError(500),
    ServiceUnavailable(503);
    public final int code;

    IcebergRestErrorCode(int code) {
      this.code = code;
    }

    public static IcebergRestErrorCode exceptionToCode(Exception e) {
      if (e instanceof UnsupportedOperationException) {
        return UnsupportedOperation;
      } else if (e instanceof ObjectNotExistsException) {
        return NotFound;
      } else if (e instanceof CommitFailedException) {
        return Conflict;
      } else if (e instanceof NoSuchTableException) {
        return NotFound;
      } else if (e instanceof NoSuchNamespaceException) {
        return NotFound;
      } else if (e instanceof AlreadyExistsException) {
        return Conflict;
      }
      return InternalServerError;
    }
  }
}
