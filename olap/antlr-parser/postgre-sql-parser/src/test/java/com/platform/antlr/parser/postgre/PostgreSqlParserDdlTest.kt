package com.platform.antlr.parser.postgre

import com.platform.antlr.parser.common.AlterType
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.*
import com.platform.antlr.parser.common.relational.common.CommentData
import com.platform.antlr.parser.common.relational.create.CreateMaterializedView
import com.platform.antlr.parser.common.relational.create.CreateTable
import com.platform.antlr.parser.common.relational.create.CreateView
import com.platform.antlr.parser.common.relational.drop.DropTable
import com.platform.antlr.parser.postgre.PostgreSqlHelper
import org.junit.Assert
import org.junit.Test

/**
 * Created by libinsong on 2020/6/30 11:04 上午
 */
class PostgreSqlParserDdlTest {

    @Test
    fun createTable0() {
        val sql = """
            CREATE TEMPORARY TABLE test.public.authors (
                id INTEGER NOT NULL PRIMARY KEY,
                last_name TEXT,
                first_name TEXT,
                age int not null
            ) PARTITION BY RANGE (age); 
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertTrue(statement.temporary)
            Assert.assertEquals(TableId("test", "public", "authors"), statement.tableId)
            Assert.assertEquals(4, statement.columnRels?.size)
            Assert.assertTrue(statement.columnRels?.get(0)?.isPk!!)

            Assert.assertEquals("RANGE", statement.partitionType)
            Assert.assertEquals(1, statement.partitionColumnNames?.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createView0() {
        val sql = """
            CREATE OR REPLACE VIEW comedies AS
            SELECT f.*,
                   country_code_to_name(f.country_code) AS country,
                   (SELECT avg(r.rating)
                    FROM user_ratings r
                    WHERE r.film_id = f.id) AS avg_rating
            FROM films f
            WHERE f.kind = 'Comedy'
            WITH CASCADED CHECK OPTION;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is CreateView) {
            Assert.assertEquals(StatementType.CREATE_VIEW, statement.statementType)
            Assert.assertEquals("comedies", statement.tableId.tableName)

            Assert.assertEquals(2, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createMatView0() {
        val sql = """
            CREATE MATERIALIZED VIEW sales_summary AS
              SELECT
                  seller_no,
                  invoice_date,
                  sum(invoice_amt)::numeric(13,2) as sales_amt
                FROM invoice
                WHERE invoice_date < CURRENT_DATE
                GROUP BY
                  seller_no,
                  invoice_date;
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is CreateMaterializedView) {
            Assert.assertEquals(StatementType.CREATE_MATERIALIZED_VIEW, statement.statementType)
            Assert.assertEquals("sales_summary", statement.tableId.tableName)

            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropTable0() {
        val sql = """
            drop TABLE test.public.authors
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is DropTable) {
            Assert.assertEquals(StatementType.DROP_TABLE, statement.statementType)
            Assert.assertEquals(TableId("test", "public", "authors"), statement.tableId)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropTable1() {
        val sql = """
            drop TABLE authors, tests
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is DropTable) {
            Assert.assertEquals(StatementType.DROP_TABLE, statement.statementType)
            Assert.assertEquals(TableId("authors"), statement.tableId)
            Assert.assertEquals(2, statement.tableIds.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createIndexTest() {
        val sql = "CREATE UNIQUE INDEX title_idx ON films (title) INCLUDE (director, rating);\n"

        val statement = PostgreSqlHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(AlterType.ADD_INDEX, statement.alterType)
            Assert.assertEquals(TableId("films"), statement.tableId)
            val createIndex = statement.firstAction() as CreateIndex
            Assert.assertEquals("title_idx", createIndex.indexName)
        }
    }

    @Test
    fun dropIndexTest() {
        val sql = "DROP INDEX title_idx"

        val statement = PostgreSqlHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(AlterType.DROP_INDEX, statement.alterType)
            val dropIndex = statement.firstAction() as DropIndex
            Assert.assertEquals("title_idx", dropIndex.indexName)
        }
    }

    @Test
    fun addPartitonTest() {
        val sql = "create table pkslow_person_r1 partition of pkslow_person_r for values from (MINVALUE) to (10);  \n"

        val statement = PostgreSqlHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(AlterType.ADD_PARTITION, statement.alterType)
            Assert.assertEquals("pkslow_person_r", statement.tableId.tableName)

            val action = statement.firstAction() as AlterTableAction
            Assert.assertEquals("pkslow_person_r1", action.newTableId?.tableName)
        }
    }

    @Test
    fun commentTest0() {
        val sql = """
            COMMENT ON TABLE my_schema.my_table IS 'Employee Information';
        """.trimIndent()

        val statement = PostgreSqlHelper.getStatement(sql)
        
        if (statement is CommentData) {
            Assert.assertEquals(StatementType.COMMENT, statement.statementType)
            Assert.assertEquals("Employee Information", statement.comment)
            Assert.assertFalse(statement.isNull)
        }
    }
}
