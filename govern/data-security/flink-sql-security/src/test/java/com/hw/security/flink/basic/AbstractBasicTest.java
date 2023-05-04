package com.hw.security.flink.basic;

import apache.flink.table.catalog.hive.HiveTestUtils;
import com.hw.security.flink.PolicyManager;
import com.hw.security.flink.SecurityContext;
import com.hw.security.flink.policy.DataMaskPolicy;
import com.hw.security.flink.policy.RowFilterPolicy;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.types.Row;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Start the local hive metastore and build a test for the Hive catalog.
 *
 * @author: HamaWhite
 */
public abstract class AbstractBasicTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBasicTest.class);


    private static final String CATALOG_NAME = "hive";
    private static final String HIVE_VERSION = "3.1.2";
    private static final String DATABASE = "default";

    protected static final String USER_A = "user_A";
    protected static final String USER_B = "user_B";

    protected static final String TABLE_ORDERS = "orders";
    protected static final String TABLE_PRODUCTS = "products";
    protected static final String TABLE_SHIPMENTS = "shipments";

    protected static PolicyManager policyManager;
    protected static SecurityContext securityContext;
    private static HiveCatalog hiveCatalog;

    @BeforeClass
    public static void setup() {
        policyManager = new PolicyManager();
        // use hive catalog, so that flink can use hive's data masking function,  such as mask_hash, mask_first_n
        hiveCatalog = HiveTestUtils.createHiveCatalog(CATALOG_NAME, DATABASE, HIVE_VERSION);
        hiveCatalog.open();
        securityContext = new SecurityContext(policyManager);
        securityContext.useCatalog(hiveCatalog);

        securityContext.execute(String.format("LOAD MODULE hive WITH ('hive-version' = '%s')", HIVE_VERSION));
    }

    @AfterClass
    public static void closeCatalog() {
        if (hiveCatalog != null) {
            hiveCatalog.close();
        }
        HiveTestUtils.deleteTemporaryFolder();
    }

    public static RowFilterPolicy rowFilterPolicy(String username, String tableName, String condition) {
        return new RowFilterPolicy(username, CATALOG_NAME, DATABASE, tableName, condition);
    }

    public static DataMaskPolicy dataMaskPolicy(String username, String tableName, String columnName, String condition) {
        return new DataMaskPolicy(username, CATALOG_NAME, DATABASE, tableName, columnName, condition);
    }

    protected void execute(String sql, Object[][] expected) {
        List<Row> rowList = securityContext.execute(sql, expected.length);
        assertExecuteResult(expected, rowList);
    }

    protected void executeRowFilter(String username, String sql, Object[][] expected) {
        List<Row> rowList = securityContext.executeRowFilter(username, sql, expected.length);
        assertExecuteResult(expected, rowList);
    }

    protected void executeDataMask(String username, String sql, Object[][] expected) {
        List<Row> rowList = securityContext.executeDataMask(username, sql, expected.length);
        assertExecuteResult(expected, rowList);
    }

    protected void mixedExecute(String username, String sql, Object[][] expected) {
        List<Row> rowList = securityContext.mixedExecute(username, sql, expected.length);
        assertExecuteResult(expected, rowList);
    }

    protected void assertExecuteResult(Object[][] expectedArray, List<Row> actualList) {
        Object[][] actualArray = actualList.stream()
                .map(e -> {
                    Object[] array = new Object[e.getArity()];
                    for (int pos = 0; pos < e.getArity(); pos++) {
                        array[pos] = e.getField(pos);
                    }
                    return array;
                }).collect(Collectors.toList())
                .toArray(new Object[0][0]);

        assertThat(actualArray).isEqualTo(expectedArray);
    }

    protected void rewriteRowFilter(String username, String inputSql, String expectedSql) {
        String resultSql = securityContext.rewriteRowFilter(username, inputSql);
        assertRewriteResult(inputSql, expectedSql, resultSql);
    }

    protected void rewriteDataMask(String username, String inputSql, String expectedSql) {
        String resultSql = securityContext.rewriteDataMask(username, inputSql);
        assertRewriteResult(inputSql, expectedSql, resultSql);
    }

    protected void mixedRewrite(String username, String inputSql, String expectedSql) {
        String resultSql = securityContext.mixedRewrite(username, inputSql);
        assertRewriteResult(inputSql, expectedSql, resultSql);
    }

    protected void assertRewriteResult(String inputSql, String expectedSql, String resultSql) {
        inputSql = minifySql(inputSql);
        expectedSql = minifySql(expectedSql);

        resultSql = resultSql.replace("\n", " ").replace("`", "");
        LOG.info("Input  SQL: {}", inputSql);
        LOG.info("Result SQL: {}\n", resultSql);
        assertEquals(expectedSql, resultSql);
    }


    /**
     * Simplify some problems with indentation and spaces
     */
    private String minifySql(String sql) {
        return sql.replaceAll("\\s+", " ")
                .replace(" ,", ",")
                .replace("( ", "(")
                .replace(" )", ")")
                .trim();
    }

    /**
     * Create mysql cdc table orders
     */
    protected static void createTableOfOrders() {
        securityContext.execute("DROP TABLE IF EXISTS " + TABLE_ORDERS);

        securityContext.execute("CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS + " (" +
                "       order_id            INT PRIMARY KEY NOT ENFORCED ," +
                "       order_date          TIMESTAMP(0)                 ," +
                "       customer_name       STRING                       ," +
                "       product_id          INT                          ," +
                "       price               DECIMAL(10, 5)               ," +
                "       order_status        BOOLEAN                      ," +
                "       region              STRING                        " +
                ") WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '192.168.90.150'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'root@123456'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = '" + TABLE_ORDERS + "' " +
                ")"
        );
    }

    /**
     * Create mysql cdc table products
     */
    protected static void createTableOfProducts() {
        securityContext.execute("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);

        securityContext.execute("CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTS + " (" +
                "       id                  INT PRIMARY KEY NOT ENFORCED ," +
                "       name                STRING                       ," +
                "       description         STRING                        " +
                ") WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '192.168.90.150'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'root@123456'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = '" + TABLE_PRODUCTS + "' " +
                ")"
        );
    }

    /**
     * Create mysql cdc table shipments
     */
    protected static void createTableOfShipments() {
        securityContext.execute("DROP TABLE IF EXISTS " + TABLE_SHIPMENTS);

        securityContext.execute("CREATE TABLE IF NOT EXISTS " + TABLE_SHIPMENTS + " (" +
                "       shipment_id          INT PRIMARY KEY NOT ENFORCED ," +
                "       order_id             INT                          ," +
                "       origin               STRING                       ," +
                "       destination          STRING                       ," +
                "       is_arrived           BOOLEAN                       " +
                ") WITH ( " +
                "       'connector' = 'mysql-cdc'            ," +
                "       'hostname'  = '192.168.90.150'       ," +
                "       'port'      = '3306'                 ," +
                "       'username'  = 'root'                 ," +
                "       'password'  = 'root@123456'          ," +
                "       'server-time-zone' = 'Asia/Shanghai' ," +
                "       'database-name' = 'demo'             ," +
                "       'table-name'    = '" + TABLE_SHIPMENTS + "' " +
                ")"
        );
    }

    /**
     * Create mysql cdc table print_sink
     */
    protected static void createTableOfPrintSink() {
        securityContext.execute("DROP TABLE IF EXISTS print_sink ");

        securityContext.execute("CREATE TABLE IF NOT EXISTS print_sink (" +
                "       order_id            INT PRIMARY KEY NOT ENFORCED ," +
                "       order_date          TIMESTAMP(0)                 ," +
                "       customer_name       STRING                       ," +
                "       product_id          INT                          ," +
                "       price               DECIMAL(10, 5)               ," +
                "       order_status        BOOLEAN                      ," +
                "       region              STRING                        " +
                ") WITH ( " +
                "       'connector' = 'print'            " +
                ")"
        );
    }
}
