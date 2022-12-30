package datart.data.provider.sql.examples;

import datart.data.provider.sql.common.TestSqlDialects;
import datart.data.provider.sql.entity.SqlTestEntity;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.dialect.ClickHouseSqlDialect;

import java.util.ArrayList;
import java.util.List;

public class NormalSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    static {
        initScripts(TestSqlDialects.getAllSqlDialects());
        initMysqlScripts();
        initOracleScripts();
    }

    private static void initScripts(List<SqlDialect> sqlDialects){
        for (SqlDialect sqlDialect : sqlDialects) {
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "select * from test where age = 18", "select * from test where age = 18"));
        }
    }

    private static void initMysqlScripts() {
        SqlDialect sqlDialect = new ClickHouseSqlDialect(ClickHouseSqlDialect.DEFAULT_CONTEXT);
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "-- comment\n" + "SELECT t.name FROM test_table t ",
                "SELECT t.name FROM test_table t"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "/*test\n" + "multiline \n" + "comment*/" + "SELECT * FROM test_table WHERE name='a' ORDER BY id DESC ",
                "SELECT * FROM test_table WHERE name='a' ORDER BY id DESC"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000",
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000 ",
                "SELECT * FROM test_table WHERE name not like 'a' and id <> '123' and age != 0 and year between 1990 and 2000"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select `date` from tableName",
                "select `date` from tableName"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select IFNULL(id),SUM(num),MAX(age),AVG(score),TRIM(content) from tableName",
                "select IFNULL(id),SUM(num),MAX(age),AVG(score),TRIM(content) from tableName"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select distinct age from tableName union select distinct age from tableName2",
                "select distinct age from tableName union select distinct age from tableName2"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select concat(concat('1', age), id) from test_table",
                "select concat(concat('1', age), id) from test_table"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "with RECURSIVE c(n) as " +
                        " (select 1   union all select n + 1 from c where n < 10) " +
                        " select n from c",
                "with RECURSIVE c(n) as " +
                        " (select 1   union all select n + 1 from c where n < 10) " +
                        " select n from c"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select INSERT('Football',0,4,'Play') AS col1 from test_table",
                "select INSERT('Football',0,4,'Play') AS col1 from test_table"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select date_add(oclife_time, interval-day(oclife_time)+1 day) as dt from ttt",
                "select date_add(oclife_time, interval-day(oclife_time)+1 day) as dt from ttt"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT JSON_ARRAY('a', 1, NOW()) 'a', JSON_OBJECT('key1', 1, 'key2', 'abc') 'o', JSON_ARRAY('x')=JSON_ARRAY('X') as 'x', JSON_VALID('null') 'n', \n" +
                        "\tsentence->>\"$.mascot\" 's' from facts",
                "SELECT JSON_ARRAY('a', 1, NOW()) 'a', JSON_OBJECT('key1', 1, 'key2', 'abc') 'o', JSON_ARRAY('x')=JSON_ARRAY('X') as 'x', JSON_VALID('null') 'n',  \tsentence->>\"$.mascot\" 's' from facts"));
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "SELECT SQL_CALC_FOUND_ROWS * FROM tbl_name WHERE id > 100 LIMIT 10",
                "SELECT SQL_CALC_FOUND_ROWS * FROM tbl_name WHERE id > 100 LIMIT 10"));
    }

    private static void initOracleScripts() {
        SqlDialect sqlDialect = TestSqlDialects.ORACLE;
        sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                "select * from test_table where age between 0 and 20",
                "select * from test_table where age between 0 and 20"));
    }

}