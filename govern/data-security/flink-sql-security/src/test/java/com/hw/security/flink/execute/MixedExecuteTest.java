package com.hw.security.flink.execute;

import com.hw.security.flink.basic.AbstractBasicTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Execute the single sql with user row-level filter and data mask policies.
 *
 * <p> Note: Depending on the external Mysql environment, you can run it manually.
 *
 * @author: HamaWhite
 */
@Ignore
public class MixedExecuteTest extends AbstractBasicTest {

    @BeforeClass
    public static void init() {
        // create mysql cdc table orders
        createTableOfOrders();

        // add row filter policies
        policyManager.addPolicy(rowFilterPolicy(USER_A, TABLE_ORDERS, "region = 'beijing'"));
        policyManager.addPolicy(rowFilterPolicy(USER_B, TABLE_ORDERS, "region = 'hangzhou'"));

        // add data mask policies
        policyManager.addPolicy(dataMaskPolicy(USER_A, TABLE_ORDERS, "customer_name", "MASK"));
        policyManager.addPolicy(dataMaskPolicy(USER_B, TABLE_ORDERS, "customer_name", "MASK_SHOW_FIRST_4"));
    }

    /**
     * Execute without row-level filter or data mask
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
     * User A can only view data in the beijing region and the customer_name after mask
     */
    @Test
    public void testExecuteByUserA() {
        String sql = "SELECT order_id, customer_name, product_id, region FROM orders";

        Object[][] expected = {
                {10001, "Xxxx", 102, "beijing"},
                {10002, "Xxxxx", 105, "beijing"}
        };
        mixedExecute(USER_A, sql, expected);
    }


    /**
     * User B can only view data in the hangzhou region and the customer_name after mask_show_first_4
     */
    @Test
    public void testExecuteByUserB() {
        String sql = "SELECT order_id, customer_name, product_id, region FROM orders";

        Object[][] expected = {
                {10003, "Edwaxx", 106, "hangzhou"},
                {10004, "John", 103, "hangzhou"}
        };
        mixedExecute(USER_B, sql, expected);
    }
}
