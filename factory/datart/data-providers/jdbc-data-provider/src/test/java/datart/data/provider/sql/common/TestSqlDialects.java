package datart.data.provider.sql.common;

import com.google.common.collect.Lists;
import datart.core.common.Application;
import datart.data.provider.JdbcDataProvider;
import datart.data.provider.jdbc.JdbcProperties;
import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import org.apache.calcite.sql.SqlDialect;
import org.junit.platform.commons.util.ReflectionUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TestSqlDialects {

    public static SqlDialect MYSQL;
    public static SqlDialect ORACLE;

    public static SqlDialect MSSQL;

    public static SqlDialect H2;

    public static SqlDialect HIVE;

    public static SqlDialect POSTGRESQL;

    public static SqlDialect PRESTO;

    static {
        String userDir = Application.userDir();
        File file = new File(userDir);
        String rootPath = file.getParentFile().getParent();
        System.setProperty("user.dir", rootPath);
        Field[] fields = TestSqlDialects.class.getFields();
        for (Field field : fields) {
            String name = field.getName();
            try {
                field.setAccessible(true);
                field.set(TestSqlDialects.class, getSqlDialect(name));
            } catch (Exception ignore) {ignore.printStackTrace();}
        }
        System.setProperty("user.dir", userDir);
    }

    private static SqlDialect getSqlDialect(String dbType) {
        JdbcProperties properties = new JdbcProperties();
        properties.setDbType(dbType);
        properties.setUrl("");
        properties.setUser("");
        properties.setPassword("");
        properties.setDriverClass("");
        properties.setProperties(new Properties());
        properties.setEnableSpecialSql(false);
        JdbcDataProviderAdapter dataProvider = JdbcDataProvider.ProviderFactory.createDataProvider(properties, true);
        SqlDialect sqlDialect = dataProvider.getSqlDialect();
        dataProvider.close();
        return sqlDialect;
    }

    public static List<SqlDialect> getAllSqlDialects() {
        List<SqlDialect> sqlDialects = new ArrayList<>();
        Field[] fields = TestSqlDialects.class.getFields();
        List objects = ReflectionUtils.readFieldValues(Lists.newArrayList(fields), TestSqlDialects.class);
        return (List<SqlDialect>) objects;
    }

}
