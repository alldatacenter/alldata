package com.hw.lineage.flink.tablefuncion;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * https://nightlies.apache.org/flink/flink-docs-release-1.14/docs/dev/table/functions/udfs/#table-functions
 *
 * @description: TableFunctionTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class TableFunctionTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create mysql cdc table ods_mysql_users
        createTableOfOdsMysqlUsers();

        // create my_split_udtf
        createFunctionOfMySplit();

        // create hudi sink table dwd_hudi_users
        createTableOfDwdHudiUsers();
    }


    /**
     * insert-select with my_split_udtf
     * <p>
     * insert into hudi table from mysql cdc stream table.
     */
    @Test
    public void testInsertSelectWithUDTF() {
        String sql = "INSERT INTO dwd_hudi_users " +
                "SELECT " +
                "   length ," +
                "   name ," +
                "   word as company_name ," +
                "   birthday ," +
                "   ts ," +
                "   DATE_FORMAT(birthday, 'yyyyMMdd') " +
                "FROM" +
                "   ods_mysql_users ," +
                "   LATERAL TABLE(my_split_udtf(name))";

        String[][] expectedArray = {
                {"ods_mysql_users", "name", "dwd_hudi_users", "id", "my_split_udtf(name).length"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "company_name", "my_split_udtf(name).word"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }


    /**
     * insert-select left join with my_split_udtf
     * <p>
     * insert into hudi table from mysql cdc stream table.
     */
    @Test
    public void testInsertSelectLeftJoinUDTF() {
        String sql = "INSERT INTO dwd_hudi_users " +
                "SELECT " +
                "   length ," +
                "   name ," +
                "   word as company_name ," +
                "   birthday ," +
                "   ts ," +
                "   DATE_FORMAT(birthday, 'yyyyMMdd') " +
                "FROM" +
                "   ods_mysql_users " +
                "LEFT JOIN " +
                "   LATERAL TABLE(my_split_udtf(name)) ON TRUE";

        String[][] expectedArray = {
                {"ods_mysql_users", "name", "dwd_hudi_users", "id", "my_split_udtf(name).length"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "company_name", "my_split_udtf(name).word"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }


    /**
     * insert-select left join with my_split_udtf and rename fields of the function in SQL
     * <p>
     * insert into hudi table from mysql cdc stream table.
     */
    @Test
    public void testInsertSelectLeftJoinAndRenameUDTF() {
        String sql = "INSERT INTO dwd_hudi_users " +
                "SELECT " +
                "   new_length ," +
                "   name ," +
                "   new_word as company_name ," +
                "   birthday ," +
                "   ts ," +
                "   DATE_FORMAT(birthday, 'yyyyMMdd') " +
                "FROM" +
                "   ods_mysql_users " +
                "LEFT JOIN " +
                "   LATERAL TABLE(my_split_udtf(name)) AS T(new_word, new_length) ON TRUE";

        String[][] expectedArray = {
                {"ods_mysql_users", "name", "dwd_hudi_users", "id", "my_split_udtf(name).length"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "company_name", "my_split_udtf(name).word"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }


    /**
     * Create my_split_udtf
     */
    private void createFunctionOfMySplit() {
        context.execute("DROP FUNCTION IF EXISTS my_split_udtf");

        context.execute("CREATE FUNCTION IF NOT EXISTS my_split_udtf " +
                "AS 'com.hw.lineage.flink.tablefuncion.MySplitFunction'"
        );
    }
}
