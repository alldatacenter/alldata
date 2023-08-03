package com.netease.arctic.server;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.io.reader.GenericIcebergDataReader;
import com.netease.arctic.server.catalog.InternalCatalog;
import com.netease.arctic.server.table.TableService;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableMetaStore;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Streams;
import org.apache.iceberg.rest.RESTCatalog;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TestIcebergRestCatalogService {
  private static final Logger LOG = LoggerFactory.getLogger(TestIcebergRestCatalogService.class);


  static AmsEnvironment ams = AmsEnvironment.getIntegrationInstances();
  static String restCatalogUri = IcebergRestCatalogService.ICEBERG_REST_API_PREFIX;


  private final String database = "test_ns";
  private final String table = "test_iceberg_tbl";

  private final Namespace ns = Namespace.of(database);
  private final TableIdentifier identifier = TableIdentifier.of(ns, table);

  private final Schema schema = BasicTableTestHelper.TABLE_SCHEMA;
  private final PartitionSpec spec = BasicTableTestHelper.SPEC;

  private String location;

  @BeforeAll
  public static void beforeAll() throws Exception {
    ams.start();
  }

  @AfterAll
  public static void afterAll() throws IOException {
    ams.stop();
  }

  TableService service;
  InternalCatalog serverCatalog;

  @BeforeEach
  public void before() {
    service = ams.serviceContainer().getTableService();
    serverCatalog = (InternalCatalog) service.getServerCatalog(AmsEnvironment.INTERNAL_ICEBERG_CATALOG);
    location = serverCatalog.getMetadata().getCatalogProperties().get(CatalogMetaProperties.KEY_WAREHOUSE) +
        "/" + database + "/" + table;
  }


  @Nested
  public class CatalogPropertiesTest {
    @Test
    public void testCatalogProperties() {
      CatalogMeta meta = serverCatalog.getMetadata();
      CatalogMeta oldMeta = meta.deepCopy();
      meta.putToCatalogProperties("cache-enabled", "false");
      meta.putToCatalogProperties("cache.expiration-interval-ms", "10000");
      serverCatalog.updateMetadata(meta);
      String warehouseInAMS = meta.getCatalogProperties().get(CatalogMetaProperties.KEY_WAREHOUSE);

      Map<String, String> clientSideConfiguration = Maps.newHashMap();
      clientSideConfiguration.put("warehouse", "/tmp");
      clientSideConfiguration.put("cache-enabled", "true");

      try (RESTCatalog catalog = loadCatalog(clientSideConfiguration)) {
        Map<String, String> finallyConfigs = catalog.properties();
        // overwrites properties using value from ams
        Assertions.assertEquals(warehouseInAMS, finallyConfigs.get("warehouse"));
        // default properties using value from client then properties.
        Assertions.assertEquals("true", finallyConfigs.get("cache-enabled"));
        Assertions.assertEquals("10000", finallyConfigs.get("cache.expiration-interval-ms"));
      } catch (IOException e) {
        throw new RuntimeException(e);
      } finally {
        serverCatalog.updateMetadata(oldMeta);
      }
    }
  }


  @Nested
  public class NamespaceTests {
    RESTCatalog nsCatalog;

    @BeforeEach
    public void setup() {
      nsCatalog = loadCatalog(Maps.newHashMap());
    }

    @Test
    public void testNamespaceOperations() throws IOException {
      Assertions.assertTrue(nsCatalog.listNamespaces().isEmpty());
      nsCatalog.createNamespace(Namespace.of(database));
      Assertions.assertEquals(1, nsCatalog.listNamespaces().size());
      nsCatalog.dropNamespace(Namespace.of(database));
      Assertions.assertTrue(nsCatalog.listNamespaces().isEmpty());
    }
  }


  @Nested
  public class TableTests {
    RESTCatalog nsCatalog;
    List<Record> newRecords = Lists.newArrayList(
        DataTestHelpers.createRecord(7, "777", 0, "2022-01-01T12:00:00"),
        DataTestHelpers.createRecord(8, "888", 0, "2022-01-02T12:00:00"),
        DataTestHelpers.createRecord(9, "999", 0, "2022-01-03T12:00:00")
    );

    @BeforeEach
    public void setup() {
      nsCatalog = loadCatalog(Maps.newHashMap());
      serverCatalog.createDatabase(database);
    }

    @AfterEach
    public void clean() {
      nsCatalog.dropTable(identifier);
      if (serverCatalog.exist(database, table)) {
        serverCatalog.dropTable(database, table);

      }
      serverCatalog.dropDatabase(database);
    }

    @Test
    public void testCreateTableAndListing() throws IOException {
      Assertions.assertTrue(nsCatalog.listTables(ns).isEmpty());

      LOG.info("Assert create iceberg table");
      nsCatalog.createTable(identifier, schema);
      Assertions.assertEquals(1, nsCatalog.listTables(ns).size());
      Assertions.assertEquals(identifier, nsCatalog.listTables(ns).get(0));

      LOG.info("Assert load iceberg table");
      Table tbl = nsCatalog.loadTable(identifier);
      Assertions.assertNotNull(tbl);
      Assertions.assertEquals(schema.asStruct(), tbl.schema().asStruct());
      Assertions.assertEquals(location, tbl.location());

      LOG.info("Assert table exists");
      Assertions.assertTrue(nsCatalog.tableExists(identifier));
      nsCatalog.dropTable(identifier);
      Assertions.assertFalse(nsCatalog.tableExists(identifier));
    }

    @Test
    public void testTableWriteAndCommit() throws IOException {
      Table tbl = nsCatalog.createTable(identifier, schema);

      List<DataFile> files = DataTestHelpers.writeIceberg(tbl, newRecords);
      AppendFiles appendFiles = tbl.newAppend();
      files.forEach(appendFiles::appendFile);
      appendFiles.commit();

      tbl = nsCatalog.loadTable(identifier);
      List<FileScanTask> tasks = Streams.stream(tbl.newScan().planFiles()).collect(Collectors.toList());
      Assertions.assertEquals(files.size(), tasks.size());
    }

    @Test
    public void testTableTransaction() throws IOException {
      Table tbl = nsCatalog.createTable(identifier, schema, spec);
      List<DataFile> files = DataTestHelpers.writeIceberg(tbl, newRecords);

      Transaction tx = tbl.newTransaction();
      AppendFiles appendFiles = tx.newAppend();
      files.forEach(appendFiles::appendFile);
      appendFiles.commit();

      UpdateProperties properties = tx.updateProperties();
      properties.set("k1", "v1");
      properties.commit();

      Table loadedTable = nsCatalog.loadTable(identifier);
      Assertions.assertNull(loadedTable.currentSnapshot());
      List<FileScanTask> tasks = Streams.stream(tbl.newScan().planFiles()).collect(Collectors.toList());
      Assertions.assertEquals(0, tasks.size());

      tx.commitTransaction();
      loadedTable.refresh();

      Assertions.assertNotNull(loadedTable.currentSnapshot());
      Assertions.assertEquals("v1", loadedTable.properties().get("k1"));
      tasks = Streams.stream(tbl.newScan().planFiles()).collect(Collectors.toList());
      Assertions.assertEquals(files.size(), tasks.size());
    }


    @Test
    public void testArcticCatalogLoader() {
      Table tbl = nsCatalog.createTable(identifier, schema, spec);
      List<DataFile> files = DataTestHelpers.writeIceberg(tbl, newRecords);
      AppendFiles appendFiles = tbl.newAppend();
      files.forEach(appendFiles::appendFile);
      appendFiles.commit();

      ArcticCatalog catalog = ams.catalog(AmsEnvironment.INTERNAL_ICEBERG_CATALOG);
      ArcticTable arcticTable = catalog.loadTable(com.netease.arctic.table.TableIdentifier.of(
          AmsEnvironment.INTERNAL_ICEBERG_CATALOG, database, table));

      Assertions.assertEquals(TableFormat.ICEBERG, arcticTable.format());
      GenericIcebergDataReader reader = new GenericIcebergDataReader(
          arcticTable.io(),
          arcticTable.schema(),
          arcticTable.schema(),
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false
      );
      List<Record> records = DataTestHelpers.readBaseStore(
          arcticTable, reader, Expressions.alwaysTrue());
      Assertions.assertEquals(newRecords.size(), records.size());
    }

  }


  private RESTCatalog loadCatalog(Map<String, String> clientProperties) {
    clientProperties.put("uri", ams.getHttpUrl() + restCatalogUri);
    clientProperties.put("warehouse", AmsEnvironment.INTERNAL_ICEBERG_CATALOG);

    CatalogMeta catalogMeta = serverCatalog.getMetadata();
    TableMetaStore store = com.netease.arctic.utils.CatalogUtil.buildMetaStore(catalogMeta);

    return (RESTCatalog) CatalogUtil.loadCatalog(
        "org.apache.iceberg.rest.RESTCatalog", "test",
        clientProperties, store.getConfiguration()
    );
  }

}
