package com.platform.antlr.parser.mysql

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.create.CreateTableAsSelect
import com.platform.antlr.parser.common.relational.dml.QueryStmt
import com.platform.antlr.parser.common.relational.dml.InsertTable
import com.platform.antlr.parser.common.relational.dml.DeleteTable
import com.platform.antlr.parser.common.relational.dml.UpdateTable
import org.junit.Assert
import org.junit.Test

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class MySQLParserDmlTest {

    @Test
    fun selectTest0() {
        val sql = "select * from users limit 5, 10"

        val statement = MySQLHelper.getStatement(sql)
        if(statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(5, statement.offset)
            Assert.assertEquals(10, statement.limit)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun selectTest1() {
        val sql = "select * from users a left outer join address b on a.address_id = b.id limit 10 offset 100"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(2, statement.inputTables.size)
            Assert.assertEquals(10, statement.limit)
            Assert.assertEquals(100, statement.offset)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest0() {
        val sql = """
            DELETE FROM films
            WHERE producer_id IN (SELECT id FROM producers WHERE name = 'foo');
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("films", statement.tableId?.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest1() {
        val sql = """
            DELETE t1, t2 FROM t1 INNER JOIN t2 INNER JOIN t3
            WHERE t1.id=t2.id AND t2.id=t3.id;
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("t1", statement.tableId.tableName)
            Assert.assertEquals(2, statement.outputTables.size)
            Assert.assertEquals(3, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest2() {
        val sql = """
            DELETE FROM t1, t2 USING t1 INNER JOIN t2 INNER JOIN t3
            WHERE t1.id=t2.id AND t2.id=t3.id;
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("t1", statement.tableId.tableName)
            Assert.assertEquals(2, statement.outputTables.size)
            Assert.assertEquals(3, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deleteTest3() {
        val sql = """
            DELETE FROM users
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("users", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest0() {
        val sql = """
            UPDATE employees SET sales_count = sales_count + 1 WHERE id =
            (SELECT sales_person FROM accounts WHERE name = 'Acme Corporation');
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals("employees", statement.tableId?.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun updateTest1() {
        val sql = """
            UPDATE product p LEFT JOIN product_price pp ON p.productid= pp.productid 
            SET p.isdelete = 1 WHERE pp.productid IS NULL;
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals("product", statement.tableId.tableName)
            Assert.assertEquals(2, statement.outputTables.size)
            Assert.assertEquals(0, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertValuesTest() {
        val sql = """
            insert into bigdata."user" select * from users a left outer join address b on a.address_id = b.id
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("bigdata", statement.tableId?.schemaName)
            Assert.assertEquals("user", statement.tableId?.tableName)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun replaceValuesTest() {
        //val sql = "insert into user values('name')"
        val sql = "REPLACE into bigdata.user select * from users a left outer join address b on a.address_id = b.id"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("bigdata", statement.tableId?.schemaName)
            Assert.assertEquals("user", statement.tableId?.tableName)
            Assert.assertTrue(statement.mysqlReplace)
            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun countCondTest() {
        val sql = "select count(type='mac' or null) From test_table where a=2"

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.SELECT, statement.statementType)
        
        if (statement is QueryStmt) {
            Assert.assertEquals("test_table", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun cteTest0() {
        val sql = """
            WITH
              cte1 AS (SELECT a, b FROM table1),
              cte2 AS (SELECT c, d FROM table2)
            SELECT b, d FROM cte1 JOIN cte2
            WHERE cte1.a = cte2.c;
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.SELECT, statement.statementType)
        
        if (statement is QueryStmt) {
            Assert.assertEquals("table1", statement.inputTables.get(0).tableName)

            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun ctasTest0() {
        val sql = "create table demo1 as select * from demo"

        val statement = MySQLHelper.getStatement(sql)
        if (statement is CreateTableAsSelect) {
            Assert.assertEquals(StatementType.CREATE_TABLE_AS_SELECT, statement.statementType)
            Assert.assertEquals("demo1", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }
}

