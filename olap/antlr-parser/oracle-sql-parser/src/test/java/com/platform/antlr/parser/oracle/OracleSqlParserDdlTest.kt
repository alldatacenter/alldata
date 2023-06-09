package com.platform.antlr.parser.oracle

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.common.CommentData
import com.platform.antlr.parser.common.relational.create.CreateMaterializedView
import com.platform.antlr.parser.common.relational.create.CreateTable
import com.platform.antlr.parser.common.relational.create.CreateView
import com.platform.antlr.parser.oracle.OracleSqlHelper
import org.junit.Assert
import org.junit.Test

class OracleSqlParserDdlTest {
    @Test
    fun createTableTest0() {
        val sql = """
            CREATE TABLE employees(
                employee_id number(10) NOT NULL,
                employee_name varchar2(50) NOT NULL,
                city varchar2(50),
                CONSTRAINT employees_pk PRIMARY KEY (employee_id)
            );
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("employees", statement.tableId.tableName)
            Assert.assertEquals(3, statement.columnRels?.size)
            Assert.assertTrue(statement.columnRels?.get(0)?.isPk!!)
            Assert.assertFalse(statement.columnRels?.get(1)?.isPk!!)
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
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
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
                  sum(invoice_amt) as sales_amt
                FROM invoice
                WHERE invoice_date < CURRENT_DATE
                GROUP BY
                  seller_no,
                  invoice_date;
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateMaterializedView) {
            Assert.assertEquals(StatementType.CREATE_MATERIALIZED_VIEW, statement.statementType)
            Assert.assertEquals("sales_summary", statement.tableId.tableName)

            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun commentTest0() {
        val sql = """
            COMMENT ON COLUMN employees.job_id IS 'abbreviated job title';
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CommentData) {
            Assert.assertEquals(StatementType.COMMENT, statement.statementType)
            Assert.assertEquals("employees.job_id", statement.objValue)
            Assert.assertEquals("abbreviated job title", statement.comment)
            Assert.assertFalse(statement.isNull)
        }
    }
}