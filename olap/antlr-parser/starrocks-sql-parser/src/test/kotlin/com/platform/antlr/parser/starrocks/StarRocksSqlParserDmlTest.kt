package com.platform.antlr.parser.starrocks

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.create.CreateTable
import com.platform.antlr.parser.common.relational.dml.DeleteTable
import com.platform.antlr.parser.common.relational.dml.InsertTable
import com.platform.antlr.parser.common.relational.dml.QueryStmt
import com.platform.antlr.parser.common.relational.dml.UpdateTable
import com.platform.antlr.parser.starrocks.StarRocksHelper
import org.junit.Assert
import org.junit.Test

class StarRocksSqlParserDmlTest {

    @Test
    fun selectTest0() {
        val sql = """
            SELECT * FROM hive1.hive_db.hive_table limit 10 offset 20
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(10, statement.limit)
            Assert.assertEquals(20, statement.offset)
            Assert.assertEquals(TableId("hive1", "hive_db", "hive_table"),
                statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun selectTest1() {
        val sql = """
            with t1 as (select * from bigdata.users),
                t2 as (select 2)
            select * from t1 union all select * from t2;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(TableId("bigdata", "users"),
                statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest0() {
        val sql = """
            DELETE FROM my_table PARTITION p1 WHERE k1 = 3;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("my_table", statement.tableId.tableName)
            Assert.assertEquals(0, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest1() {
        val sql = """
            DELETE FROM score_board
            WHERE name IN (select name from users where country = "China");
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("score_board", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("users", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest2() {
        val sql = """
            WITH foo_producers as (
                SELECT * from producers
                where producers.name = 'foo'
            )
            DELETE FROM films USING foo_producers
            WHERE producer_id = foo_producers.id;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("films", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("producers", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest1() {
        val sql = """
            UPDATE employees
            SET sales_count = sales_count + 1           
            FROM accounts
            WHERE accounts.name = 'Acme Corporation'
               AND employees.id = accounts.sales_person;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals("employees", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("accounts", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest2() {
        val sql = """
            WITH acme_accounts as (
                SELECT * from accounts
                 WHERE accounts.name = 'Acme Corporation'
            )
            UPDATE employees SET sales_count = sales_count + 1
            FROM acme_accounts
            WHERE employees.id = acme_accounts.sales_person;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals("employees", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("accounts", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest1() {
        val sql = """
            INSERT INTO test SELECT * FROM test2;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("test", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("test2", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest2() {
        val sql = """
            INSERT INTO test PARTITION(p1, p2) WITH LABEL `label1` SELECT * FROM test2;
        """.trimIndent()

        val statement = StarRocksHelper.getStatement(sql)
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("test", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("test2", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }
}