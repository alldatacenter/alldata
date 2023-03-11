package com.hw.lineage.flink.window;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @description: WindowTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class WindowTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create mysql cdc table ods_mysql_users
        createTableOfOdsMysqlUsers();

        // create hudi sink table dwd_hudi_users
        createTableOfDwdHudiUsers();
    }


    /**
     * insert-select with ROW_NUMBER()
     * <p>
     * insert into hudi table from mysql cdc stream table.
     */
    @Test
    public void testInsertSelectRowNumber() {
        String sql = "INSERT INTO dwd_hudi_users " +
                "SELECT " +
                "   ROW_NUMBER() OVER (PARTITION BY id ORDER BY ts DESC) as rowNum ," +
                "   name ," +
                "   name as company_name ," +
                "   birthday ," +
                "   ts ," +
                "   DATE_FORMAT(birthday, 'yyyyMMdd') " +
                "FROM" +
                "   ods_mysql_users";

        String[][] expectedArray = {
                {"ods_mysql_users", "id", "dwd_hudi_users", "id"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "id"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "company_name"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }

    /**
     * insert-select with two ROW_NUMBER()
     * <p>
     * insert into hudi table from mysql cdc stream table.
     */
    @Test
    public void testInsertSelectTwoRowNumber() {
        String sql = "INSERT INTO dwd_hudi_users " +
                "SELECT " +
                "   ROW_NUMBER() OVER (PARTITION BY id ORDER BY ts DESC) as rowNum ," +
                "   name ," +
                "   CAST(ROW_NUMBER() OVER (PARTITION BY name ORDER BY ts DESC) as STRING) as company_name ," +
                "   birthday ," +
                "   ts ," +
                "   DATE_FORMAT(birthday, 'yyyyMMdd') " +
                "FROM" +
                "   ods_mysql_users";

        String[][] expectedArray = {
                {"ods_mysql_users", "id", "dwd_hudi_users", "id"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "id"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "company_name"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "company_name"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }
}