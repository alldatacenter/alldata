package datart.data.provider.calcite.dialect;

import org.apache.calcite.sql.dialect.PostgresqlSqlDialect;

public class PostgresqlSqlDialectSupport extends PostgresqlSqlDialect implements FetchAndOffsetSupport{

    public PostgresqlSqlDialectSupport() {
        this(DEFAULT_CONTEXT);
    }
    private PostgresqlSqlDialectSupport(Context context) { super(context); }

}
