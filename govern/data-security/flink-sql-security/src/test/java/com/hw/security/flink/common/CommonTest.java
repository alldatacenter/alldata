package com.hw.security.flink.common;

import com.google.common.collect.ImmutableList;
import com.hw.security.flink.basic.AbstractBasicTest;
import com.hw.security.flink.model.ColumnEntity;
import com.hw.security.flink.model.TableEntity;
import org.apache.flink.types.Row;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.junit.Assert.assertEquals;


/**
 * @description: CommonTest
 * @author: HamaWhite
 */
public class CommonTest extends AbstractBasicTest {

    @BeforeClass
    public static void createTable() {
        // create mysql cdc table orders
        createTableOfOrders();
    }

    @Test
    public void testGetTable() {
        TableEntity actual = securityContext.getTable(TABLE_ORDERS);
        List<ColumnEntity> columnList = ImmutableList.of(
                new ColumnEntity("order_id", "INT"),
                new ColumnEntity("order_date", "TIMESTAMP(0)"),
                new ColumnEntity("customer_name", "STRING"),
                new ColumnEntity("product_id", "INT"),
                new ColumnEntity("price", "DECIMAL(10, 5)"),
                new ColumnEntity("order_status", "BOOLEAN"),
                new ColumnEntity("region", "STRING")
        );
        TableEntity expected = new TableEntity(TABLE_ORDERS, columnList);
        assertEquals(expected, actual);
    }


    /**
     * Call the system function that comes with Hive in FlinkSQL (under the default database),
     * <p>so that Hive UDF can be reused when desensitizing Flink SQL data.
     *
     * <p>Note:
     * <ol>
     *  <li>Ranger's masking strategy is also implemented by calling Hive's UDF.
     *  <li>Uppercase letters are converted to "X"
     *  <li>Lowercase letters are converted to "x"
     *  <li>Numbers are converted to "n"
     * </ol>
     */
    @Test
    public void testHiveSystemFunction() {
        executeHiveFunction("select mask('hive-HDFS-8765-4321')","xxxx-XXXX-nnnn-nnnn");
        executeHiveFunction("select mask_first_n('hive-HDFS-8765-4321', 4)","xxxx-HDFS-8765-4321");
        executeHiveFunction("select mask_last_n('hive-HDFS-8765-4321', 4)","hive-HDFS-8765-nnnn");
        executeHiveFunction("select mask_show_first_n('hive-HDFS-8765-4321', 4)","hive-XXXX-nnnn-nnnn");
        executeHiveFunction("select mask_show_last_n('hive-HDFS-8765-4321', 4)","xxxx-XXXX-nnnn-4321");
        executeHiveFunction("select mask_hash('flink')","7f025323639628aa5e5d24bd56f43317552b140c71406d0eb5a555671bd534d2");
    }

    private void executeHiveFunction(String sql, String result) {
        List<Row> rowList = securityContext.execute(sql);
        assertThat(rowList).isNotNull()
                .extracting(e -> e.getField(0))
                .contains(result, atIndex(0));
    }
}
