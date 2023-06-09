package com.platform.antlr.parser.spark

import com.platform.antlr.parser.spark.SparkStreamSqlHelper
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.common.SetStatement
import com.platform.antlr.parser.common.relational.create.CreateTable
import com.platform.antlr.parser.common.relational.dml.InsertTable
import org.junit.Assert
import org.junit.Test

/**
 *
 * Created by libinsong on 2018/3/15.
 */
class SparkStreamSqlParserTest {

    @Test
    fun createTableTest() {
        val sql = "CREATE Stream TABLE student_scores (\n" +
                "  student_number string comment '学号', \n" +
                "  student_name string comment '姓名', \n" +
                "  subject string comment '学科',\n" +
                "  score '/data/score' INT comment '成绩' \n" +
                ")\n" +
                "WITH (\n" +
                "  type = \"dis\",\n" +
                "  format = \"json\",\n" +
                "  kafka.group.id = \"cn-north-1\"\n" +
                ")"

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("student_scores", statement.tableId.tableName)
            Assert.assertEquals("cn-north-1", statement.properties?.get("kafka.group.id"))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest4() {
        val sql = """
            CREATE Stream TABLE orders (
                userid string,
                money bigint
            )
            WITH (
                type = 'kafka',
                topic = 'flink-topic-input',
                encode = 'json',
                kafka.bootstrap.servers = 'localhost:9092',
                kafka.group.id = 'flink-group'
            );
            
            insert into stat_orders SELECT userid, SUM(money) as total_money FROM orders
            GROUP BY TUMBLE(proctime, INTERVAL '10' SECOND), userid;
            
            set spark.test = 'hello world';
            set spark.test = setsd,sd,resr;
            set spark.test = hello world;
            set spark.test = hello-world;
            set spark.test = hello $\{usename} test;
            #set spark.test = hello comment;
            set spark.test = hello 'test' world;
            set spark.test = hello "test" world;
            set spark.test = hdfs://user/hive;
            set spark.test = 12,12;
            set spark.test = 3.45;
            set spark.test = ibdex.json;
            set spark.test = dw.eset_sdfe_sd;
            set spark.test = demo.test;
            set spark.test = dsd(id)%=2;
            """

        val statementDatas = SparkStreamSqlHelper.getStatement(sql)
        Assert.assertEquals(16, statementDatas.size)
        val statement = statementDatas.get(0)
        if (statement is CreateTable) {
            Assert.assertEquals("orders", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest5() {
        val sql = "CREATE Stream TABLE tdl_hudi_stream_json_dt\n" +
                "    WITH (\n" +
                "    type = 'hudi',\n" +
                "    databaseName = \"bigdata\",\n" +
                "    tableName = \"hudi_stream_json_dt\"" +
                "    )"

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("tdl_hudi_stream_json_dt", statement.tableId.tableName)
            Assert.assertEquals("hudi", statement.properties?.get("type"))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun setConfigTest() {
        val sql = "set spark.test = false";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is SetStatement) {
            Assert.assertEquals(StatementType.SET, statement.statementType)
            Assert.assertEquals("spark.test", statement.key)
            Assert.assertEquals("false", statement.value)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun setConfigTest1() {
        val sql = "set spark.test = 'hello world'";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is SetStatement) {
            Assert.assertEquals(StatementType.SET, statement.statementType)
            Assert.assertEquals("spark.test", statement.key)
            Assert.assertEquals("hello world", statement.value)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun setConfigTest2() {
        val sql = "set spark.test = 12 ";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is SetStatement) {
            Assert.assertEquals(StatementType.SET, statement.statementType)
            Assert.assertEquals("spark.test", statement.key)
            Assert.assertEquals("12", statement.value)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun setConfigTest3() {
        val sql = "set spark.test = 'demo' ";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is SetStatement) {
            Assert.assertEquals(StatementType.SET, statement.statementType)
            Assert.assertEquals("spark.test", statement.key)
            Assert.assertEquals("demo", statement.value)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertSqlTest() {
        val sql = "insert into bigdata.test_result1 select * from users";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("test_result1", statement.tableId?.tableName)
            Assert.assertEquals("select * from users", statement.querySql)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun insertUDTFSqlTest() {
        val sql = "insert into stat_orders_kafka\n" +
        "SELECT \n" +
                "    TUMBLE_START(proctime, INTERVAL '10' SECOND),\n" +
                "    TUMBLE_END(proctime, INTERVAL '10' SECOND),\n" +
                "    userid, \n" +
                "    SUM(money) as total_money \n" +
                "FROM (select userid, money, proctime from orders LEFT JOIN LATERAL TABLE(demoFunc(a)) as T(newuserid) ON TRUE) a\n" +
                "GROUP BY TUMBLE(proctime, INTERVAL '10' SECOND), userid;";

        val statement = SparkStreamSqlHelper.getStatement(sql).get(0)
        
        if (statement is InsertTable) {
            Assert.assertEquals(StatementType.INSERT, statement.statementType)
            Assert.assertEquals("stat_orders_kafka", statement.tableId?.tableName)
        } else {
            Assert.fail()
        }
    }
}
