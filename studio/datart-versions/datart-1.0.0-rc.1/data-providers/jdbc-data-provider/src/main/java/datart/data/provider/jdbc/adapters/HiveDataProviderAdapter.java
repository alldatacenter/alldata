package datart.data.provider.jdbc.adapters;

import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

public class HiveDataProviderAdapter extends JdbcDataProviderAdapter {


    @Override
    protected String readCurrDatabase(Connection conn, boolean isCatalog) throws SQLException {
        String url = jdbcProperties.getUrl().replaceFirst(".*://", "jdbc://");
        url = StringUtils.split(url, ";")[0];
        URI uri = URI.create(url);
        String database = uri.getPath().replace("/", "");
        if (StringUtils.isBlank(database)) {
            return null;
        }
        return super.readCurrDatabase(conn, isCatalog);
    }
}
