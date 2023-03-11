package com.hw.lineage.flink.lookup.join;

import com.hw.lineage.flink.basic.AbstractBasicTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @description: LookupJoinTest
 * @author: HamaWhite
 * @version: 1.0.0
 */
public class LookupJoinTest extends AbstractBasicTest {

    @Before
    public void createTable() {
        // create mysql cdc table ods_mysql_users
        createTableOfOdsMysqlUsers();

        // create mysql dim table dim_mysql_company
        createTableOfDimMysqlCompany();

        // create hudi sink table dwd_hudi_users
        createTableOfDwdHudiUsers();
    }


    /**
     * insert-select-two-table lookup join.
     * <p>
     * insert into hudi table from mysql cdc stream lookup join mysql dim table, which has system
     * udf CONCAT
     */
    @Test
    public void testInsertSelectTwoLookupJoin() {
        String sql = "INSERT into dwd_hudi_users " +
                "SELECT " +
                "       a.id as id1," +
                "       CONCAT(a.name,b.company_name) , " +
                "       b.company_name , " +
                "       a.birthday ," +
                "       a.ts ," +
                "       DATE_FORMAT(a.birthday, 'yyyyMMdd') as p " +
                "FROM" +
                "       ods_mysql_users as a " +
                "JOIN " +
                "   dim_mysql_company FOR SYSTEM_TIME AS OF a.proc_time AS b " +
                "ON " +
                "   a.id = b.user_id";

        String[][] expectedArray = {
                {"ods_mysql_users", "id", "dwd_hudi_users", "id"},
                {"ods_mysql_users", "name", "dwd_hudi_users", "name", "CONCAT(ods_mysql_users.name, dim_mysql_company.company_name)"},
                {"dim_mysql_company", "company_name", "dwd_hudi_users", "name", "CONCAT(ods_mysql_users.name, dim_mysql_company.company_name)"},
                {"dim_mysql_company", "company_name", "dwd_hudi_users", "company_name"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "birthday"},
                {"ods_mysql_users", "ts", "dwd_hudi_users", "ts"},
                {"ods_mysql_users", "birthday", "dwd_hudi_users", "partition", "DATE_FORMAT(birthday, 'yyyyMMdd')"}
        };

        parseFieldLineage(sql, expectedArray);
    }

}
