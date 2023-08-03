package com.platform.antlr.parser.mysql

import com.platform.antlr.parser.common.*
import com.platform.antlr.parser.common.StatementType
import com.platform.antlr.parser.common.relational.*
import com.platform.antlr.parser.common.relational.common.UseDatabase
import com.platform.antlr.parser.common.relational.create.CreateDatabase
import com.platform.antlr.parser.common.relational.create.CreateTable
import com.platform.antlr.parser.common.relational.drop.DropDatabase
import com.platform.antlr.parser.common.relational.drop.DropTable
import com.platform.antlr.parser.common.relational.table.TruncateTable
import org.junit.Assert
import org.junit.Test

/**
 *
 * Created by libinsong on 2018/1/10.
 */
class MySQLParserDdlTest {

    @Test
    fun createDatabaseTest() {
        val sql = """
            CREATE DATABASE IF NOT EXISTS "bigdata"
        """.trimIndent()

        val statement = MySQLHelper.getStatement(sql)
        if(statement is CreateDatabase) {
            val name = statement.databaseName
            Assert.assertEquals(StatementType.CREATE_DATABASE, statement.statementType)
            Assert.assertEquals("bigdata", name)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropDatabaseTest() {
        val sql = "DROP DATABASE IF EXISTS bigdata"

        val statement = MySQLHelper.getStatement(sql)
        if (statement is DropDatabase) {
            val name = statement.databaseName
            Assert.assertEquals(StatementType.DROP_DATABASE, statement.statementType)
            Assert.assertEquals("bigdata", name)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest() {
        val sql = "CREATE TABLE bigdata.dc_config (\n" +
                "  `id` int(11) NOT NULL AUTO_INCREMENT comment 'id',\n" +
                "  `appname` varchar(64) NOT NULL,\n" +
                "  `profile` varchar(64) NOT NULL,\n" +
                "  `width` DECIMAL(5, 2) NOT NULL,\n" +
                "  `config_text` longtext,\n" +
                "  `content` json,\n" +
                "  `content1` text COLLATE utf8mb4_unicode_ci comment 'test',\n" +
                "  `creater` varchar(45) NOT NULL,\n" +
                "  `modifier` varchar(45) DEFAULT NULL,\n" +
                "  `gmt_created` datetime NOT NULL,\n" +
                "  `gmt_modified` datetime DEFAULT NULL,\n" +
                "  PRIMARY KEY (`id`),\n" +
                "  UNIQUE KEY `appname_UNIQUE` (`appname`,`profile`)\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8 ROW_FORMAT=COMPACT COMMENT='系统参数配置';"

        val statement = MySQLHelper.getStatement(sql)
        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("bigdata", statement.tableId.schemaName)
            Assert.assertEquals("dc_config", statement.tableId.tableName)
            Assert.assertEquals("系统参数配置", statement.comment)

            statement.columnRels?.get(0)?.let { Assert.assertTrue(it.isPk) }
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest1() {
        val sql = "CREATE TABLE `box_partner` (\n" +
                "    `id` bigint(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
                "    `code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '客户编码',\n" +
                "    `name` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT '' COMMENT '客户名称',\n" +
                "    `role` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户角色',\n" +
                "    `key` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户key',\n" +
                "    `logo` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户logo',\n" +
                "    `info` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户简介',\n" +
                "    `contacts` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '联系方式',\n" +
                "    `token` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户唯一标识',\n" +
                "    `callback_url` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '客户提供的回调地址',\n" +
                "    `attachment` varchar(512) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '扩展字段',\n" +
                "    `create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                "    `modified` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',\n" +
                "    `callback_url_status` int(11) DEFAULT NULL COMMENT '回调地址状态:成功0,失败1,通讯中断2',\n" +
                "    `callback_url_detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '回调事件',\n" +
                "    PRIMARY KEY (`id`),\n" +
                "    UNIQUE KEY `token` (`token`)\n" +
                "    ) ENGINE=InnoDB AUTO_INCREMENT=7564 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"

        val statement = MySQLHelper.getStatement(sql)

        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("box_partner", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest2() {
        val sql = " CREATE TABLE `decision_flow_model` (\n" +
                "  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',\n" +
                "  `model_uuid` char(32) NOT NULL COMMENT '关联holmes_model表的uuid',\n" +
                "  `partner_code` varchar(128) NOT NULL COMMENT '合作方',\n" +
                "  `decision_flow_uuid` char(32) NOT NULL COMMENT '关联decision_flow表的uuid',\n" +
                "  `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',\n" +
                "  `gmt_modify` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',\n" +
                "  PRIMARY KEY (`id`)\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=4475 DEFAULT CHARSET=utf8 COMMENT='决策流与模型关系表'"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("decision_flow_model", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest3() {
        val sql = "CREATE TABLE `app_channel_daily_report` (\n" +
                "  `id` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
                "  `normal_counting` bigint(20) NOT NULL DEFAULT '0' COMMENT '正常用户',\n" +
                "  `abnormal_counting` bigint(20) NOT NULL DEFAULT '0' COMMENT '异常用户',\n" +
                "  `emulator_counting` bigint(20) NOT NULL DEFAULT '0' COMMENT '模拟器用户',\n" +
                "  `gmt_create` datetime NOT NULL COMMENT '创建时间',\n" +
                "  `gmt_modified` datetime DEFAULT NULL COMMENT '更新时间',\n" +
                "  `create_by` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '创建者',\n" +
                "  `update_by` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '更新者',\n" +
                "  `event_type` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '事件类型',\n" +
                "  `partner_code` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '合作方编码',\n" +
                "  `app_channel` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT '渠道',\n" +
                "  `app_name` varchar(32) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'app名称',\n" +
                "  `app_version` varchar(32) COLLATE utf8_unicode_ci NOT NULL DEFAULT 'default',\n" +
                "  PRIMARY KEY (`id`,`gmt_create`),\n" +
                "  KEY `channel_report_index` (`gmt_create`,`partner_code`,`app_channel`,`app_name`,`app_version`,`event_type`)\n" +
                ") ENGINE=InnoDB AUTO_INCREMENT=33703438 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='渠道反作弊日统计表'\n" +
                " PARTITION BY RANGE (month(gmt_create)-1)\n" +
                "(PARTITION part0 VALUES LESS THAN (1) COMMENT = '1月份' ENGINE = InnoDB,\n" +
                " PARTITION part1 VALUES LESS THAN (2) COMMENT = '2月份' ENGINE = InnoDB,\n" +
                " PARTITION part2 VALUES LESS THAN (3) COMMENT = '3月份' ENGINE = InnoDB,\n" +
                " PARTITION part3 VALUES LESS THAN (4) COMMENT = '4月份' ENGINE = InnoDB,\n" +
                " PARTITION part4 VALUES LESS THAN (5) COMMENT = '5月份' ENGINE = InnoDB,\n" +
                " PARTITION part5 VALUES LESS THAN (6) COMMENT = '6月份' ENGINE = InnoDB,\n" +
                " PARTITION part6 VALUES LESS THAN (7) COMMENT = '7月份' ENGINE = InnoDB,\n" +
                " PARTITION part7 VALUES LESS THAN (8) COMMENT = '8月份' ENGINE = InnoDB,\n" +
                " PARTITION part8 VALUES LESS THAN (9) COMMENT = '9月份' ENGINE = InnoDB,\n" +
                " PARTITION part9 VALUES LESS THAN (10) COMMENT = '10月份' ENGINE = InnoDB,\n" +
                " PARTITION part10 VALUES LESS THAN (11) COMMENT = '11月份' ENGINE = InnoDB,\n" +
                " PARTITION part11 VALUES LESS THAN (12) COMMENT = '12月份' ENGINE = InnoDB)"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("app_channel_daily_report", statement.tableId.tableName)
            Assert.assertEquals("PARTITION", statement.partitionType)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest4() {
        val sql = """
            CREATE TABLE IF NOT EXISTS `decision_flow_model` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
    `model_uuid` char(32) NOT NULL COMMENT '关联holmes_model表的uuid',
    `partner_code` varchar(128) NOT NULL COMMENT '合作方',
    `decision_flow_uuid` char(32) NOT NULL COMMENT '关联decision_flow表的uuid',
    `gmt_create` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modify` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    PRIMARY KEY (`id`)
    ) ENGINE=InnoDB AUTO_INCREMENT=77 DEFAULT CHARSET=utf8
            """

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("decision_flow_model", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createTableTest5() {
        val sql = """
            CREATE TABLE `dw_job_analysis_detail` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` varchar(255) DEFAULT NULL,
  `job_id` int(11) DEFAULT NULL,
  `job_type` varchar(255) DEFAULT NULL,
  `job_status` int(255) DEFAULT NULL,
  `create_time` timestamp(6) NULL DEFAULT NULL,
  `creater` varchar(255) DEFAULT NULL,
  `modifier` varchar(255) DEFAULT NULL,
  `gmt_created` timestamp(6) NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  `gmt_modified` datetime(6) DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=75306 DEFAULT CHARSET=utf8
            """

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is CreateTable) {
            Assert.assertEquals(StatementType.CREATE_TABLE, statement.statementType)
            Assert.assertEquals("dw_job_analysis_detail", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropTableTest() {
        val sql = "DROP table IF EXISTS bigdata.users"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is DropTable) {
            Assert.assertEquals(StatementType.DROP_TABLE, statement.statementType)
            Assert.assertEquals("bigdata", statement.tableId?.schemaName)
            Assert.assertEquals("users", statement.tableId?.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun renameTableTest() {
        val sql = "RENAME TABLE `datacompute`.`users_quan` TO  `datacompute`.`dc_users`\n"
        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is AlterTable) {
            Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
            Assert.assertEquals(AlterType.RENAME_TABLE, statement.alterType)
            Assert.assertEquals("datacompute", statement.tableId.schemaName)
            Assert.assertEquals("users_quan", statement.tableId.tableName)
            val action = statement.firstAction() as AlterTableAction
            Assert.assertEquals("dc_users", action.newTableId?.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun analyzeTableTest() {
        val sql = "analyze table bigdata.users"

        val statement = MySQLHelper.getStatement(sql)
        
        if (statement is AnalyzeTable) {
            val table = statement.tableIds.get(0)

            Assert.assertEquals(StatementType.ANALYZE_TABLE, statement.statementType)
            Assert.assertEquals("bigdata", table.schemaName)
            Assert.assertEquals("users", table.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun truncateTableTest() {
        val sql = "TRUNCATE TABLE test.user"

        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is TruncateTable) {
            Assert.assertEquals(StatementType.TRUNCATE_TABLE, statement.statementType)
            Assert.assertEquals("test", statement.tableId.schemaName)
            Assert.assertEquals("user", statement.tableId.tableName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun changeColumnTest() {
        val sql = "ALTER TABLE `datacompute`.`log_collect_config` \n" +
                "CHANGE COLUMN `partition_type` `partition_type1` VARCHAR(45) NULL DEFAULT 'day' COMMENT '分区类型：day, hour, minute' ;"
        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is AlterTable) {
            Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
            Assert.assertEquals("datacompute", statement.tableId.schemaName)
            Assert.assertEquals("log_collect_config", statement.tableId.tableName)

            val action = statement.firstAction() as AlterColumnAction
            Assert.assertEquals("partition_type", action.columName)
            Assert.assertEquals("partition_type1", action.newColumName)
            Assert.assertEquals("分区类型：day, hour, minute", action.comment)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun modifyColumnTest() {
        val sql = "ALTER TABLE t1 MODIFY age BIGINT NOT NULL;"
        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is AlterTable) {
            Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
            Assert.assertEquals("t1", statement.tableId.tableName)

            val action = statement.firstAction() as AlterColumnAction
            Assert.assertEquals(AlterType.ALTER_COLUMN, statement.alterType)
            Assert.assertEquals("age", action.columName)
            Assert.assertEquals("BIGINT", action.dataType)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun addColumnTest() {
        val sql = "ALTER TABLE `datacompute`.`users_quan` ADD COLUMN `age` VARCHAR(45) NULL DEFAULT 18 COMMENT '年龄' AFTER `username`"
        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is AlterTable) {
            Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
            Assert.assertEquals("datacompute", statement.tableId.schemaName)
            Assert.assertEquals("users_quan", statement.tableId.tableName)

            val action = statement.firstAction() as AlterColumnAction
            Assert.assertEquals(AlterType.ADD_COLUMN, statement.alterType)
            Assert.assertEquals("age", action.columName)
            Assert.assertEquals("年龄", action.comment)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun dropColumnTest() {
        val sql = "ALTER TABLE `datacompute`.`users_quan` DROP COLUMN `age`;"
        val statement = MySQLHelper.getStatement(sql)
        
        if(statement is AlterTable) {
            Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
            Assert.assertEquals("datacompute", statement.tableId.schemaName)
            Assert.assertEquals("users_quan", statement.tableId.tableName)

            val action = statement.firstAction() as DropColumnAction
            Assert.assertEquals(AlterType.DROP_COLUMN, statement.alterType)
            Assert.assertEquals("age", action.columNames.get(0))
        } else {
            Assert.fail()
        }
    }

    @Test
    fun addUNIQUETest() {
        val sql = "ALTER TABLE `datacompute`.`dc_project_member`\n" +
                "    ADD UNIQUE INDEX `uk_prj_user` (`project_code` ASC, `user_id` ASC);"
        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun addUNIQUETest1() {
        val sql = "ALTER TABLE amount_all ADD UNIQUE uk_type_time(etype, time)"
        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun addIndexTest() {
        val sql = "ALTER TABLE sj_resource_charges add index index_test (name);"
        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            val createIndex = statement.firstAction() as CreateIndex
            Assert.assertEquals("index_test", createIndex.indexName)
        }
    }

    @Test
    fun addIndexTest1() {
        val sql = "ALTER TABLE tablename ADD INDEX INDEX_NAME  (school_id, settlement_time)"
        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun dropIndexTest1() {
        val sql = "ALTER TABLE table_name DROP INDEX index_name"
        val sqls = MySQLHelper.splitAlterSql(sql);
        val statement = MySQLHelper.getStatement(sqls.get(0))
        Assert.assertEquals("ALTER TABLE table_name DROP INDEX index_name", sqls.get(0))
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun splitSqlTest() {
        val sql = "ALTER TABLE \n`datacompute`.`users_quan` \n" +
                "DROP COLUMN \n`address`,\n" +
                "CHANGE COLUMN `name` `username` VARCHAR(255) NULL DEFAULT NULL \nCOMMENT 'username' ,\n" +
                "ADD COLUMN \n`age` VARCHAR(45) NULL COMMENT 'Age' AFTER `username`"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(3, sqls.size)
    }

    @Test
    fun splitSqlTest1() {
        val sql = "ALTER TABLE `resume_upload` ADD (\n" +
                "  `part_id` int(10) DEFAULT 0 COMMENT '文件分块ID',\n" +
                "  `file_type` varchar(128) DEFAULT NULL COMMENT '文件类型')"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(2, sqls.size)
        Assert.assertEquals("ALTER TABLE `resume_upload` ADD COLUMN `part_id` int(10) DEFAULT 0 COMMENT '文件分块ID'", sqls.get(0))
        Assert.assertEquals("ALTER TABLE `resume_upload` ADD COLUMN \n" +
                "  `file_type` varchar(128) DEFAULT NULL COMMENT '文件类型'", sqls.get(1))
    }

    @Test
    fun splitSqlTest2() {
        val sql = "alter table `dw_table_detail` add column ( customer_tag varchar(256) DEFAULT NULL COMMENT '客户,标签', dim_tag varchar(256) DEFAULT NULL COMMENT '关联维度标签')"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(2, sqls.size)

        Assert.assertEquals("alter table `dw_table_detail` ADD COLUMN  customer_tag varchar(256) DEFAULT NULL COMMENT '客户,标签'", sqls.get(0))
        Assert.assertEquals("alter table `dw_table_detail` ADD COLUMN  dim_tag varchar(256) DEFAULT NULL COMMENT '关联维度标签'", sqls.get(1))
    }

    @Test
    fun splitSqlTest3() {
        val sql = "ALTER TABLE `channel_status_log` ADD `config_content` TEXT  NULL  COMMENT '配置内容'  AFTER `district_name`;"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(1, sqls.size)
        Assert.assertEquals("ALTER TABLE `channel_status_log` ADD `config_content` TEXT  NULL  COMMENT '配置内容'  AFTER `district_name`", sqls.get(0))

        val statement = MySQLHelper.getStatement(sqls.get(0))
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun splitSqlTest4() {
        val sql = "ALTER TABLE `channel_status_log` ADD `config_content` TEXT  NULL  COMMENT '配置内容'  AFTER `district_name`"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(1, sqls.size)
        Assert.assertEquals("ALTER TABLE `channel_status_log` ADD `config_content` TEXT  NULL  COMMENT '配置内容'  AFTER `district_name`", sqls.get(0))

        val statement = MySQLHelper.getStatement(sqls.get(0))
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun splitSqlTest5() {
        val sql = "ALTER TABLE `freya`.`sample_set` DROP COLUMN `create_id`, " +
                "DROP COLUMN `create_partner`, " +
                "DROP COLUMN `modify_id`, " +
                "DROP COLUMN `modify_partner`;"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(4, sqls.size)
        Assert.assertEquals("ALTER TABLE `freya`.`sample_set` DROP COLUMN `create_id`", sqls.get(0))

        val statement = MySQLHelper.getStatement(sqls.get(0))
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun splitSqlTest6() {
        val sql = "alter table event_field_total add (\n" +
                "  event_total int(11) DEFAULT '0' COMMENT '事件总数',\n" +
                "  day_date date COMMENT '数据日期'\n" +
                ")"
        val sqls = MySQLHelper.splitAlterSql(sql);

        Assert.assertEquals(2, sqls.size)
        Assert.assertEquals("alter table event_field_total ADD COLUMN event_total int(11) DEFAULT '0' COMMENT '事件总数'", sqls.get(0))

        val statement = MySQLHelper.getStatement(sqls.get(0))
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)
    }

    @Test
    fun useTest() {
        var sql = "use bigdata"

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.USE, statement.statementType)
        
        if (statement is UseDatabase) {
            Assert.assertEquals("bigdata", statement.databaseName)
        } else {
            Assert.fail()
        }
    }

    @Test
    fun createIndexTest() {
        val sql = "CREATE INDEX test_index ON demo.orders (column_name)"

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(TableId("demo", "orders"), statement.tableId)
            val createIndex = statement.firstAction() as CreateIndex
            Assert.assertEquals("test_index", createIndex.indexName)
        }
    }

    @Test
    fun dropIndexTest() {
        val sql = "DROP INDEX test_index ON demo.orders"

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(AlterType.DROP_INDEX, statement.alterType)
            Assert.assertEquals(TableId("demo", "orders"), statement.tableId)
            val dropIndex = statement.firstAction() as DropIndex
            Assert.assertEquals("test_index", dropIndex.indexName)
        }
    }

    @Test
    fun truncatePartitionTest() {
        val sql = "ALTER TABLE demo.orders TRUNCATE PARTITION p1998\n"

        val statement = MySQLHelper.getStatement(sql)
        Assert.assertEquals(StatementType.ALTER_TABLE, statement.statementType)

        
        if (statement is AlterTable) {
            Assert.assertEquals(AlterType.TRUNCATE_PARTITION, statement.alterType)
            Assert.assertEquals(TableId("demo", "orders"), statement.tableId)
        }
    }
}

