package com.platform.antlr.parser.postgre

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.create.CreateTableAsSelect
import com.platform.antlr.parser.common.relational.dml.*
import com.platform.antlr.parser.postgre.PostgreSqlHelper
import org.junit.Assert
import org.junit.Test

/**
 * Created by libinsong on 2020/6/30 11:04 上午
 */
class PostgreSqlParserDmlTest {

    @Test
    fun queryTest0() {
        val sql = """
            select a.* from datacompute1.datacompute.dc_job a left join datacompute1.datacompute.dc_job_scheduler b on a.id=b.job_id
            LIMIT 3, 2
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(2, statement.inputTables.size)
            Assert.assertEquals(3, statement.limit)
            Assert.assertEquals(2, statement.offset)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryTest1() {
        val sql = """
            SELECT * FROM public.usertest LIMIT 3 OFFSET 2;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(3, statement.limit)
            Assert.assertEquals(2, statement.offset)
            Assert.assertEquals(TableId("public", "usertest"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryTest2() {
        val sql = """
            SELECT * FROM public.usertest FETCH FIRST 5 ROWS ONLY;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)

        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(5, statement.limit)
            Assert.assertEquals(TableId("public", "usertest"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun queryTest3() {
        val sql = """
            SELECT * FROM public.usertest 
            OFFSET 10
            FETCH FIRST 10 ROWS ONLY;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)

        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(10, statement.limit)
            Assert.assertEquals(10, statement.offset)
            Assert.assertEquals(TableId("public", "usertest"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun cteSqlTest0() {
        val sql = """
            WITH regional_sales AS (
                SELECT region, SUM(amount) AS total_sales
                FROM orders
                GROUP BY region
            ), 
            top_regions AS (
                SELECT region
                FROM regional_sales
                WHERE total_sales > (SELECT SUM(total_sales)/10 FROM regional_sales)
            )
            SELECT region,
                   product,
                   SUM(quantity) AS product_units,
                   SUM(amount) AS product_sales
            FROM orders
            WHERE region IN (SELECT region FROM top_regions)
            GROUP BY region, product;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is QueryStmt) {
            Assert.assertEquals(StatementType.SELECT, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(TableId("orders"), statement.inputTables.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createAsQueryTest0() {
        val sql = """
            CREATE TABLE films_recent AS
            SELECT * FROM films WHERE date_prod >= '2002-01-01';
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is CreateTableAsSelect) {
            Assert.assertEquals(StatementType.CREATE_TABLE_AS_SELECT, statement.statementType)
            Assert.assertEquals("films_recent", statement.tableId.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals("films", statement.inputTables.get(0).tableName)
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

        val statement = PostgreSqlHelper.getStatement(sql)
        
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
            DELETE FROM films USING producers
            WHERE producer_id = producers.id AND producers.name = 'foo';
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is DeleteTable) {
            Assert.assertEquals(StatementType.DELETE, statement.statementType)
            Assert.assertEquals("films", statement.tableId?.tableName)
            Assert.assertEquals(1, statement.inputTables.size)

            Assert.assertEquals("producers", statement.inputTables.get(0).tableName)
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

        val statement = PostgreSqlHelper.getStatement(sql)
        
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
            UPDATE product p
            SET net_price = price - price * discount
            FROM product_segment s
            WHERE p.segment_id = s.id;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is UpdateTable) {
            Assert.assertEquals(StatementType.UPDATE, statement.statementType)
            Assert.assertEquals("product", statement.tableId?.tableName)
            Assert.assertEquals(1, statement.inputTables.size)

            Assert.assertEquals("product_segment", statement.inputTables.get(0).tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest0() {
        val sql = """
            INSERT INTO films (code, title, did, date_prod, kind) VALUES
            ('B6717', 'Tampopo', 110, '1985-02-10', 'Comedy'),
            ('HG120', 'The Dinner Game', 140, DEFAULT, 'Comedy');
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("films", statement.tableId?.tableName)
            Assert.assertEquals(0, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest1() {
        val sql = """
            WITH upd AS (
              UPDATE employees SET sales_count = sales_count + 1 WHERE id =
                (SELECT sales_person FROM accounts WHERE name = 'Acme Corporation')
                RETURNING *
            )
            INSERT INTO employees_log SELECT *, current_timestamp FROM upd;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("employees_log", statement.tableId?.tableName)
            Assert.assertEquals(2, statement.outputTables.size)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertTest2() {
        val sql = """
            INSERT INTO films SELECT * FROM tmp_films WHERE date_prod < '2004-05-07';
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("films", statement.tableId?.tableName)
            Assert.assertEquals(1, statement.outputTables.size)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun deltaMergeTest() {
        val sql = """
            MERGE INTO wines w
            USING wine_stock_changes s
            ON s.winename = w.winename
            WHEN NOT MATCHED AND s.stock_delta > 0 THEN
              INSERT VALUES(s.winename, s.stock_delta)
            WHEN MATCHED AND w.stock + s.stock_delta > 0 THEN
              UPDATE SET stock = w.stock + s.stock_delta
            WHEN MATCHED THEN
              DELETE;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is MergeTable) {
            Assert.assertEquals(StatementType.MERGE, statement.statementType)
            Assert.assertEquals("wines", statement.targetTable.tableName)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }
}
