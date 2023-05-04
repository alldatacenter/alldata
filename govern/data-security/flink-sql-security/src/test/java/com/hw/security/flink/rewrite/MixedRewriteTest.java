package com.hw.security.flink.rewrite;

import com.hw.security.flink.basic.AbstractBasicTest;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Add row-level filter and column masking, then return new SQL.
 *
 * @author: HamaWhite
 */
public class MixedRewriteTest extends AbstractBasicTest {

    @BeforeClass
    public static void init() {
        // create mysql cdc table orders
        createTableOfOrders();

        // create mysql cdc table products
        createTableOfProducts();

        // add row filter policy
        policyManager.addPolicy(rowFilterPolicy(USER_A, TABLE_ORDERS, "region = 'beijing'"));
        policyManager.addPolicy(rowFilterPolicy(USER_A, TABLE_PRODUCTS, "name = 'hammer'"));

        // add data mask policies
        policyManager.addPolicy(dataMaskPolicy(USER_A, TABLE_ORDERS, "customer_name", "MASK"));
        policyManager.addPolicy(dataMaskPolicy(USER_A, TABLE_PRODUCTS, "name","MASK_SHOW_LAST_4"));
    }

    /**
     * Only select
     */
    @Test
    public void testSelect() {
        String sql = "SELECT * FROM orders";

        // the alias is equal to the table name orders
        String expected = "SELECT                   " +
                "       *                           " +
                "FROM (                             " +
                "       SELECT                      " +
                "               order_id           ," +
                "               order_date         ," +
                "               CAST(mask(customer_name) AS STRING) AS customer_name ," +
                "               product_id         ," +
                "               price              ," +
                "               order_status       ," +
                "               region              " +
                "       FROM                        " +
                "               orders              " +
                "     ) AS orders                   " +
                "WHERE                              " +
                "       region = 'beijing'          ";

        mixedRewrite(USER_A, sql, expected);
    }

    /**
     * The two tables of products and orders are left joined.
     * <p> products have an alias p, order has no alias
     */
    @Test
    public void testJoin() {
        String sql = "SELECT                        " +
                "       orders.*                   ," +
                "       p.name                     ," +
                "       p.description               " +
                "FROM                               " +
                "       orders                      " +
                "LEFT JOIN                          " +
                "       products AS p               " +
                "ON                                 " +
                "       orders.product_id = p.id    ";

        String expected = "SELECT                   " +
                "       orders.*                   ," +
                "       p.name                     ," +
                "       p.description               " +
                "FROM (                             " +
                "       SELECT                      " +
                "               order_id           ," +
                "               order_date         ," +
                "               CAST(mask(customer_name) AS STRING) AS customer_name ," +
                "               product_id         ," +
                "               price              ," +
                "               order_status       ," +
                "               region              " +
                "       FROM                        " +
                "               orders              " +
                "     ) AS orders                   " +
                "LEFT JOIN (                        " +
                "       SELECT                      " +
                "               id                 ," +
                "               CAST(mask_show_last_n(name, 4, 'x', 'x', 'x', -1, '1') AS STRING) AS name, " +
                "               description         " +
                "       FROM                        " +
                "               products            " +
                "       ) AS p                      " +
                "ON                                 " +
                "       orders.product_id = p.id    " +
                "WHERE                              " +
                "       orders.region = 'beijing'   " +
                "       AND p.name = 'hammer'       ";

        mixedRewrite(USER_A, sql, expected);
    }
}
