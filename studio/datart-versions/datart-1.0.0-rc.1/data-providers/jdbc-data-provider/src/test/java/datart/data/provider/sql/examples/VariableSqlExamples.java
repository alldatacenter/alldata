package datart.data.provider.sql.examples;

import datart.data.provider.sql.entity.SqlTestEntity;
import datart.data.provider.sql.common.TestSqlDialects;
import org.apache.calcite.sql.SqlDialect;

import java.util.ArrayList;
import java.util.List;

public class VariableSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    static {
        initMysqlScripts();
        initOracleScripts();
        initSqlServerScripts();
        initH2Scripts();
        initHiveScripts();
        initPostgresqlScripts();
        initPrestoScripts();
    }

    private static void genScripts(SqlDialect sqlDialect){
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table where 部门=$部门$ and `age`>$age$ ",
                "SELECT * FROM test_table where 部门 = '销售部' and `age` > 20"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table where 部门 IN ($部门$) ",
                "SELECT * FROM test_table where 部门 IN ('销售部')"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table where 部门 IN ($部门$) and 部门 IN ($部门$) and 部门 = $部门$",
                "SELECT * FROM test_table where 部门 IN ('销售部') and 部门 IN ('销售部') and 部门 = '销售部'"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT name,age,$部门$ FROM test_table order by $部门$ limit $max$",
                "SELECT name,age,'销售部' FROM test_table order by '销售部' limit 100"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT content,count(1) FROM test_table group by $str$",
                "SELECT content,count(1) FROM test_table group by 'content'"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM `test_table` where AGE BETWEEN $min$ AND $max$ ",
                "SELECT * FROM `test_table` where AGE BETWEEN 0 AND 100"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select date_format(create_time, '%Y-%m') as month, date_add(create_time, interval-day(create_time)+1 day) dt, sum(price) price,bitmap_count(bitmap_union(id)) as num " +
                        "from db.test_table " +
                        "where create_time >= $datetime$ and id in (10000, 10001) group by month,dt,id",
                "select date_format(create_time, '%Y-%m') as month, date_add(create_time, interval-day(create_time)+1 day) dt, sum(price) price,bitmap_count(bitmap_union(id)) as num " +
                        "from db.test_table " +
                        "where create_time >= '2020-01-01 00:00:00' and id in (10000, 10001) group by month,dt,id"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select INSERT('Football',$min$,4,'Play') AS col1 from test_table limit $max$",
                "select INSERT('Football',0,4,'Play') AS col1 from test_table limit 100"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table where age BETWEEN $min$ AND 100 ",
                "SELECT * FROM test_table where AGE BETWEEN 0 AND 100"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM 'test_table' where age BETWEEN $min$ AND 100 ",
                "SELECT * FROM 'test_table' where age BETWEEN 0 AND 100"));
    }

    private static void initMysqlScripts() {
        SqlDialect sqlDialect = TestSqlDialects.MYSQL;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > TIMESTAMP '2020-01-01 00:00:00'"));
    }

    private static void initOracleScripts() {
        SqlDialect sqlDialect = TestSqlDialects.ORACLE;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and CREATE_TIME > TO_TIMESTAMP('2020-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS.FF')"));
    }

    private static void initSqlServerScripts() {
        SqlDialect sqlDialect = TestSqlDialects.MSSQL;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > '2020-01-01 00:00:00'"));
    }

    private static void initH2Scripts() {
        SqlDialect sqlDialect = TestSqlDialects.H2;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > TIMESTAMP '2020-01-01 00:00:00'"));
    }

    private static void initHiveScripts() {
        SqlDialect sqlDialect = TestSqlDialects.HIVE;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > TIMESTAMP '2020-01-01 00:00:00'"));
    }

    private static void initPostgresqlScripts() {
        SqlDialect sqlDialect = TestSqlDialects.POSTGRESQL;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > TIMESTAMP '2020-01-01 00:00:00'"));
    }

    private static void initPrestoScripts() {
        SqlDialect sqlDialect = TestSqlDialects.PRESTO;
        genScripts(sqlDialect);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where $where$ and create_time > $date$",
                "select * from test_table where 1=1 and create_time > TIMESTAMP '2020-01-01 00:00:00'"));
    }

}