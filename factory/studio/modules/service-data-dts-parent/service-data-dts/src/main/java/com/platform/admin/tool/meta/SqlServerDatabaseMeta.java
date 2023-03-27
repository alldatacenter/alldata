package com.platform.admin.tool.meta;

/**
 * SqlServer数据库 meta信息查询
 *
 * @author AllDataDC
 */
public class SqlServerDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {
    private volatile static SqlServerDatabaseMeta single;

    public static SqlServerDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (SqlServerDatabaseMeta.class) {
                if (single == null) {
                    single = new SqlServerDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryTables() {
        return "SELECT Name FROM SysObjects Where XType='U' ORDER BY Name";
    }

    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return "select schema_name(schema_id)+'.'+object_name(object_id) from sys.objects \n" +
                "where type ='U' \n" +
                "and schema_name(schema_id) ='" + tableSchema[0] + "'";

    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return "select distinct schema_name(schema_id) from sys.objects where type ='U';";
    }

}
