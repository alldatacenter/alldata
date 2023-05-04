package com.hw.security.flink.execute;

import com.hw.security.flink.basic.AbstractBasicTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Execute SQL based on row filter.
 *
 * <p> Note: Depending on the external Mysql environment, you can run it manually.
 *
 * @author: HamaWhite
 */
@Ignore
public class ExecuteRowFilterTest extends AbstractBasicTest {

    @BeforeClass
    public static void init() {
        // create mysql cdc table orders
        createTableOfOrders();

        // add row filter policies
        policyManager.addPolicy(rowFilterPolicy(USER_A, TABLE_ORDERS, "region = 'beijing'"));
        policyManager.addPolicy(rowFilterPolicy(USER_B, TABLE_ORDERS, "region = 'hangzhou'"));
    }

    /**
     * Execute without row-level filter
     */
    @Test
    public void testExecute() {
        String sql = "SELECT order_id, customer_name, product_id, region FROM orders";

        Object[][] expected = {
                {10001, "Jack", 102, "beijing"},
                {10002, "Sally", 105, "beijing"},
                {10003, "Edward", 106, "hangzhou"},
                {10004, "John", 103, "hangzhou"},
                {10005, "Edward", 104, "shanghai"},
                {10006, "Jack", 103, "shanghai"}
        };
        execute(sql, expected);
    }


    /**
     * User A can only view data in the beijing region
     */
    @Test
    public void testExecuteByUserA() {
        String sql = "SELECT order_id, customer_name, product_id, region FROM orders";

        Object[][] expected = {
                {10001, "Jack", 102, "beijing"},
                {10002, "Sally", 105, "beijing"}
        };
        executeRowFilter(USER_A, sql, expected);
    }

    /**
     * User B can only view data in the hangzhou region
     */
    @Test
    public void testExecuteByUserB() {
        String sql = "SELECT order_id, customer_name, product_id, region FROM orders";

        Object[][] expected = {
                {10003, "Edward", 106, "hangzhou"},
                {10004, "John", 103, "hangzhou"}
        };
        executeRowFilter(USER_B, sql, expected);
    }
}
