package com.hw.lineage.flink.catalog;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hw.lineage.common.enums.TableKind;
import com.hw.lineage.common.result.ColumnResult;
import com.hw.lineage.common.result.TableResult;
import com.hw.lineage.flink.LineageServiceImpl;
import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.apache.flink.table.api.ValidationException;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

/**
 * Note: this unit test depends on the external Hive and Mysql environment,
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class CatalogTest {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractBasicTest.class);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private static final String database = "lineage_db";

    private static final String tableName = "ods_mysql_users_watermark";

    private final LineageServiceImpl context = new LineageServiceImpl();

    @Test
    public void testMemoryCatalog() {
        // the default is default_catalog
        String catalogName = "memory_catalog";
        useMemoryCatalog(catalogName);
        assertThat(context.getCurrentCatalog(), is(catalogName));
    }


    private void useMemoryCatalog(String catalogName) {
        context.execute(String.format("CREATE CATALOG %s with ( 'type'='generic_in_memory','default-database'='" + database + "' )", catalogName));
        context.execute(String.format("USE CATALOG %s", catalogName));
    }

    @Test
    public void testJdbcCatalog() {
        String catalogName = "jdbc_catalog";
        expectedException.expect(ValidationException.class);
        expectedException.expectMessage(String.format("Unable to create catalog '%s'.", catalogName));
        useJdbcCatalog(catalogName);
    }


    private void useJdbcCatalog(String catalogName) {
        // The database must be created in advance, otherwise an error will be reported when creating the catalog
        context.execute("CREATE CATALOG " + catalogName + " with (  " +
                "       'type' = 'jdbc'                                     ," +
                "       'default-database' = '" + database + "'             ," +
                "       'username' = 'root'                                 ," +
                "       'password' = 'root@123456'                          ," +
                "       'base-url' = 'jdbc:mysql://192.168.90.150:3306'      " +
                ")"
        );
        context.execute(String.format("USE CATALOG %s", catalogName));
    }

    @Test
    @Ignore
    public void testHiveCatalog() {
        String catalogName = "hive_catalog";
        useHiveCatalog(catalogName);
        assertThat(context.getCurrentCatalog(), is(catalogName));
    }

    private void useHiveCatalog(String catalogName) {
        // The database must be created in advance, otherwise an error will be reported when creating the catalog
        context.execute("CREATE CATALOG " + catalogName + " with (          " +
                "       'type' = 'hive'                                              ," +
                "       'default-database' = '" + database + "'                      ," +
                "       'hive-conf-dir' = '../data/hive-conf-dir'                    ," +
                "       'hive-version' = '3.1.2'                                      " +
                ")"
        );
        context.execute(String.format("USE CATALOG %s", catalogName));
    }


    @Test
    public void testQueryMemoryCatalogInfo() throws Exception {
        String catalogName = "memory_catalog";
        useMemoryCatalog(catalogName);
        checkQueryCatalogInfo(catalogName);
    }


    @Test
    @Ignore
    public void testQueryHiveCatalogInfo() throws Exception {
        String catalogName = "hive_catalog";
        useHiveCatalog(catalogName);
        checkQueryCatalogInfo(catalogName);
    }


    private void checkQueryCatalogInfo(String catalogName) throws Exception {
        createTableOfOdsMysqlUsersWatermark();
        assertThat(context.listDatabases(catalogName), hasItem(database));
        assertThat(context.listTables(catalogName, database), hasItem(tableName));
        assertEquals(Collections.emptyList(), context.listViews(catalogName, database));

        TableResult tableResult = new TableResult()
                .setTableName(tableName)
                .setTableKind(TableKind.TABLE)
                .setComment("Users Table")
                .setColumnList(
                        ImmutableList.of(
                                new ColumnResult("id", "BIGINT", "", true),
                                new ColumnResult("name", "STRING", "", false),
                                new ColumnResult("birthday", "TIMESTAMP(3)", "", false),
                                new ColumnResult("ts", "TIMESTAMP(3)", "", false),
                                /**
                                 * TODO optimize,this should be PROCTIME(),but [PROCTIME()]
                                 * Because the asSummaryString method of different subclasses of Expression is different
                                 */
                                new ColumnResult("proc_time", "[PROCTIME()]", "", false)
                        )
                )
                .setWatermarkSpecList(Collections.singletonList("WATERMARK FOR `ts` AS [`ts` - INTERVAL '5' SECOND]"))
                .setPropertiesMap(
                        ImmutableMap.of(
                                "connector", "mysql-cdc",
                                "hostname", "127.0.0.1",
                                "port", "3306",
                                "username", "root",
                                "password", "xxx",
                                "server-time-zone", "Asia/Shanghai",
                                "database-name", "demo",
                                "table-name", "users"
                        )
                );
        LOG.info("tableResult: {}", tableResult);
        assertEquals(tableResult, context.getTable(catalogName, database, tableName));
    }


    private void createTableOfOdsMysqlUsersWatermark() {
        context.execute("DROP TABLE IF EXISTS " + tableName);

        context.execute("CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "       id                  BIGINT PRIMARY KEY NOT ENFORCED ," +
                "       name                STRING  COMMENT 'User Name'     ," +
                "       birthday            TIMESTAMP(3)                    ," +
                "       ts                  TIMESTAMP(3)                    ," +
                "       proc_time as proctime()                             ," +
                "       WATERMARK FOR ts AS ts - INTERVAL '5' SECOND         " +
                " ) " +
                " COMMENT 'Users Table' " +
                " WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '127.0.0.1'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'xxx'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = 'users' " +
                ")"
        );
    }
}
