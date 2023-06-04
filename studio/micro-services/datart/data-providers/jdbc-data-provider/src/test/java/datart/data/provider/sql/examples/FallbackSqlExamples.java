package datart.data.provider.sql.examples;

import datart.data.provider.sql.entity.SqlTestEntity;
import datart.data.provider.sql.common.TestSqlDialects;
import org.apache.calcite.sql.SqlDialect;

import java.util.ArrayList;
import java.util.List;

public class FallbackSqlExamples {

    public static List<SqlTestEntity> sqlList = new ArrayList<>();

    static {
        initScripts(TestSqlDialects.MYSQL, TestSqlDialects.ORACLE);
    }

    private static void initScripts(SqlDialect... sqlDialects){
        for (SqlDialect sqlDialect : sqlDialects) {
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table where 部门=$部门$ and `age`>$age$ ",
                    "SELECT * _FROM test_table where 部门 = '销售部' and `age` > 20"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table where 部门 IN ($部门$) ",
                    "SELECT * _FROM test_table where 部门 IN ('销售部')"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table where 部门 = ($部门$) and 部门 = ($部门$) ",
                    "SELECT * _FROM test_table where 部门 = ('销售部') and 部门 = ('销售部')"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table where age not like $min$% and name is not null",
                    "SELECT * _FROM test_table where age NOT LIKE 0% and name is not null"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table where age between $min$ and $max$ ",
                    "SELECT * _FROM test_table where age between 0 and 100"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT * _FROM test_table order by $部门$ limit $max$",
                    "SELECT * _FROM test_table order by '销售部' limit 100"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT name,age,$部门$ _FROM test_table order by $部门$ limit $max$",
                    "SELECT name,age,'销售部' _FROM test_table order by '销售部' limit 100"));
            sqlList.add(SqlTestEntity.createValidateSql(sqlDialect,
                    "SELECT content,count(1) _FROM test_table group by $str$",
                    "SELECT content,count(1) _FROM test_table group by 'content'"));
        }
    }

}