package com.platform.antlr.parser.presto

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.dml.QueryStmt
import com.platform.antlr.parser.common.relational.create.CreateTableAsSelect
import com.platform.antlr.parser.common.relational.drop.DropTable
import com.platform.antlr.parser.presto.PrestoSqlHelper
import org.junit.Assert
import org.junit.Test

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class PrestoSqlParserTest {

    @Test
    fun queryTest0() {
        val sql = """
            select a.* from datacompute1.datacompute.dc_job a left join datacompute1.datacompute.dc_job_scheduler b on a.id=b.job_id
        """.trimIndent()

        val statement = PrestoSqlHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryTest1() {
        val sql = """
            SELECT COUNT(app_name) AS "应用名" FROM (SELECT * FROM ops.dwd_app_to_container_wt 
            WHERE ds=date_format(CURRENT_DATE - interval '1' DAY, "%Y%m%d") ) tdbi_view
        """.trimIndent()

        val statement = PrestoSqlHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryLimitTest() {
        val sql = """
            select * from preso_table limit 10
        """.trimIndent()

        val statement = PrestoSqlHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(10, statement.limit)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableSelectTest() {
        val sql = """
            create table dd_s_s as select * from bigdata.test_demo_test limit 1
        """.trimIndent()

        val statement = PrestoSqlHelper.getStatement(sql)
        if (statement is CreateTableAsSelect) {
            Assert.assertEquals(StatementType.CREATE_TABLE_AS_SELECT, statement.statementType)
            Assert.assertEquals("dd_s_s", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables?.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropTableTest() {
        val sql = """
            drop table if exists bigdata.tdl_small_files_2
        """.trimIndent()

        val statement = PrestoSqlHelper.getStatement(sql)
        if (statement is DropTable) {
            Assert.assertEquals(StatementType.DROP_TABLE, statement.statementType)
            Assert.assertEquals("bigdata", statement.tableId?.schemaName)
            Assert.assertEquals("tdl_small_files_2", statement.tableId?.tableName)
        } else {
            Assert.fail()
        }
    }
}
