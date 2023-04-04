package datart.data.provider.jdbc.adapters;

import datart.core.base.PageInfo;
import datart.core.data.provider.Dataframe;
import datart.data.provider.script.SqlStringUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MsSqlDataProviderAdapter extends JdbcDataProviderAdapter {

    @Override
    protected String readCurrDatabase(Connection conn, boolean isCatalog) throws SQLException {
        String databaseName = StringUtils.substringAfterLast(jdbcProperties.getUrl().toLowerCase(), "databasename=");
        databaseName = StringUtils.substringBefore(databaseName, ";");
        if (StringUtils.isBlank(databaseName)) {
            return null;
        }
        return super.readCurrDatabase(conn, isCatalog);
    }

    @Override
    public int executeCountSql(String sql) throws SQLException {
        try (Connection connection = getConn()) {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.last();
            return resultSet.getRow();
        }
    }

    @Override
    protected Dataframe execute(String selectSql, PageInfo pageInfo) throws SQLException {
        selectSql = SqlStringUtils.rebuildSqlWithFragment(selectSql);
        return super.execute(selectSql, pageInfo);
    }
}
