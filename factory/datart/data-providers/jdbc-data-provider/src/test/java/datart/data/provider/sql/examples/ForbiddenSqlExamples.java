package datart.data.provider.sql.examples;

import datart.data.provider.sql.common.TestSqlDialects;
import datart.data.provider.sql.entity.SqlTestEntity;
import org.apache.calcite.sql.SqlDialect;

import java.util.ArrayList;
import java.util.List;

public class ForbiddenSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    static {
        initScripts(TestSqlDialects.MYSQL, TestSqlDialects.ORACLE);
        initMysqlScripts();
        initOracleScripts();
    }

    private static void initScripts(SqlDialect... sqlDialects) {
        for (SqlDialect sqlDialect : sqlDialects) {
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "create database test"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "drop database test"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "use test; delete from test_table"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "create table test_table(id int)"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "alter table test_table add age int"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "drop table test_table"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "insert into test_table value(1)"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "update test_table set age=18 where id=1"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "delete from test_table where id=1"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "commit"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "rollback"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "CREATE INDEX indexName ON table_name (column_name)"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "ALTER table tableName ADD INDEX indexName(columnName)"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "DROP INDEX [indexName] ON mytable"));
            sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                    "ALTER table tableName ADD INDEX indexName(columnName)"));
        }
    }

    private static void initMysqlScripts(){
        SqlDialect sqlDialect = TestSqlDialects.MYSQL;
        sqlList.add(SqlTestEntity.createForbiddenSql(sqlDialect,
                "replace INTO test_table (id) VALUES(123)"));
    }

    private static void initOracleScripts(){
        SqlDialect sqlDialect = TestSqlDialects.ORACLE;

    }

}
