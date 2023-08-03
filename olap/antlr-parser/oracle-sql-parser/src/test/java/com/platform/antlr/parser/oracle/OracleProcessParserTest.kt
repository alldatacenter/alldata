package com.platform.antlr.parser.oracle

import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.FunctionId
import com.platform.antlr.parser.common.relational.ProcedureId
import com.platform.antlr.parser.common.relational.TableId
import com.platform.antlr.parser.common.relational.create.CreateFunction
import com.platform.antlr.parser.common.relational.create.CreateProcedure
import com.platform.antlr.parser.oracle.OracleSqlHelper
import org.junit.Assert
import org.junit.Test

class OracleProcessParserTest {
    @Test
    fun processTest0() {
        val sql = """
            DECLARE
                CURSOR C_ORDERS IS
                    SELECT CUSTOMER_NAME, PRICE FROM ORDERS;
            BEGIN
                FOR ORD IN C_ORDERS
                    LOOP
                        dbms_output.put_line(ORD.CUSTOMER_NAME || ORD.PRICE);
                    END LOOP;
            END;
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateProcedure) {
            Assert.assertEquals(StatementType.CREATE_PROCEDURE, statement.statementType)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createFunctionTest() {
        val sql = """
            CREATE FUNCTION test.get_bal(acc_no IN NUMBER) 
               RETURN NUMBER 
               IS acc_bal NUMBER(11,2);
            BEGIN 
               SELECT order_total 
               INTO acc_bal 
               FROM orders 
               WHERE customer_id = acc_no; 
               RETURN(acc_bal); 
             END;
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateFunction) {
            Assert.assertEquals(StatementType.CREATE_FUNCTION, statement.statementType)
            Assert.assertEquals(FunctionId("test", "get_bal"), statement.functionId)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createFunctionTest1() {
        val sql = """
            CREATE FUNCTION get_bal(acc_no IN NUMBER) 
               RETURN NUMBER 
               IS acc_bal NUMBER(11,2);
            BEGIN 
               SELECT order_total 
               INTO acc_bal 
               FROM orders 
               WHERE customer_id = acc_no; 
               RETURN(acc_bal); 
             END;
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateFunction) {
            Assert.assertEquals(StatementType.CREATE_FUNCTION, statement.statementType)
            Assert.assertEquals(FunctionId( "get_bal"), statement.functionId)
            Assert.assertEquals(1, statement.inputTables.size)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createProcedureTest0() {
        val sql = """
            CREATE OR REPLACE Procedure UpdateCourse( name_in IN varchar2 )
            IS
               cnumber number;
               cursor c1 is
               SELECT course_number
                FROM courses_tbl
                WHERE course_name = name_in;
            BEGIN
               open c1;
               fetch c1 into cnumber;
            
               if c1%notfound then
                  cnumber := 9999;
               end if;
            
               INSERT INTO student_courses( course_name,course_number )
               VALUES ( name_in, cnumber );
            
               commit;
               close c1;
            EXCEPTION
            WHEN OTHERS THEN
               raise_application_error(-20001,'An error was encountered - '||SQLCODE||' -ERROR- '||SQLERRM);
            END;
        """.trimIndent()

        val statement = OracleSqlHelper.getStatement(sql)
        
        if (statement is CreateProcedure) {
            Assert.assertEquals(StatementType.CREATE_PROCEDURE, statement.statementType)
            Assert.assertEquals(ProcedureId("UpdateCourse"), statement.procedureId)
            Assert.assertEquals(1, statement.inputTables.size)
            Assert.assertEquals(TableId("courses_tbl"), statement.inputTables.get(0))
            //Assert.assertEquals(1, statement.outputTables.size)
            //Assert.assertEquals(TableId("student_courses"), statement.outputTables.get(0))
        } else {
            Assert.fail()
        }
    }
}