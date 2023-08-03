package com.platform.antlr.parser.flink

import com.platform.antlr.parser.flink.FlinkCdcCreateDatabase
import com.platform.antlr.parser.flink.FlinkCdcCreateTable
import com.platform.antlr.parser.flink.FlinkSQLHelper
import org.junit.Assert
import org.junit.Test

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class FlinkSqlParserTest {

    @Test
    fun createTableTest() {
        val sql = """
            CREATE TABLE IF NOT EXISTS user
            WITH ('jdbcWriteBatchSize' = '1024')
            AS TABLE mysql
            OPTIONS('server-id'='8001-8004')
            ADD COLUMN (
              `t_idx` AS COALESCE(SPLIT_INDEX(`tbl`, 'r', 1), 'default') FIRST,
              `c_id` AS `id` + 10 AFTER `id`)
        """.trimIndent()

        val statement = FlinkSQLHelper.getStatement(sql)
        if (statement is FlinkCdcCreateTable) {
            val table = statement.sinkTableId
            Assert.assertEquals("user", table.tableName)
            Assert.assertEquals(2, statement.computeCols?.size)
            Assert.assertEquals("COALESCE(SPLIT_INDEX(`tbl`, 'r', 1), 'default')", statement.computeCols?.get(0)?.expression)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createDatabaseTest() {
        val sql = """
            CREATE DATABASE IF NOT EXISTS holo_tpcds 
            WITH ('sink.parallelism' = '4') 
            AS DATABASE mysql.tpcds 
            INCLUDING ALL TABLES 
            EXCLUDING TABLE "test"
            OPTIONS('server-id'='8001-8004')
        """.trimIndent()

        val statement = FlinkSQLHelper.getStatement(sql)
        if (statement is FlinkCdcCreateDatabase) {
            Assert.assertEquals("holo_tpcds", statement.sinkDatabaseName)
            Assert.assertEquals("test", statement.excludeTable)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createDatabaseFromKafkaTest() {
        val sql = """
            CREATE DATABASE IF NOT EXISTS flink_cdc_demos 
            WITH (
                'connector' = 'jdbc',
                'url' = 'jdbc:mysql://172.18.1.55:3306/flink_cdc_demos?characterEncoding=utf-8&useSSL=false',
                'username' = 'root',
                'password' = 'Datac@123',
                'table-name' = '${'$'}{tableName}',
                'sink.buffer-flush.interval' = '2s',
                'sink.buffer-flush.max-rows' = '100',
                'sink.max-retries' = '5'
            ) 
            AS DATABASE `demo1, demo2, demo3, demo4`
            OPTIONS(
            	'connector' = 'kafka',
                'brokers' = '172.18.1.56:9093'
            )
        """.trimIndent()

        val statement = FlinkSQLHelper.getStatement(sql)
        if (statement is FlinkCdcCreateDatabase) {
            Assert.assertEquals("demo1, demo2, demo3, demo4", statement.sourceDatabaseName)
            Assert.assertEquals("172.18.1.56:9093", statement.sourceOptions?.get("brokers"))
        } else {
            Assert.fail()
        }
    }
}
