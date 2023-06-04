package datart.data.provider.sql.entity;

import datart.core.data.provider.ExecuteParam;
import lombok.Data;
import org.apache.calcite.sql.SqlDialect;

@Data
public class SqlTestEntity {

    private SqlDialect sqlDialect;

    private String sql;

    private String desireSql;

    private ExecuteParam executeParam;

    public SqlTestEntity() {
    }

    public static SqlTestEntity createForbiddenSql(SqlDialect sqlDialect, String sql) {
        SqlTestEntity rec = new SqlTestEntity();
        rec.setSqlDialect(sqlDialect);
        rec.setSql(sql);
        return rec;
    }

    public static SqlTestEntity createValidateSql(SqlDialect sqlDialect, String sql, String desireSql) {
        SqlTestEntity rec = new SqlTestEntity();
        rec.setSqlDialect(sqlDialect);
        rec.setSql(sql);
        rec.setDesireSql(desireSql);
        return rec;
    }

    @Override
    public String toString() {
        return " sqlDialect: " + sqlDialect.getDatabaseProduct().name() + '\n' +
                " the origin sql: " + sql + '\n' +
                " the desire sql: " + desireSql + '\n';
    }
}
