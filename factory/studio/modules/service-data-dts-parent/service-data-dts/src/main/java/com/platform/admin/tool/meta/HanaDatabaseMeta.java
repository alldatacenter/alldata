package com.platform.admin.tool.meta;

/**
 * HANA数据库 meta信息查询
 *
 * @author AllDataDC
 * @ClassName HANADatabaseMeta
 */
public class HanaDatabaseMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static HanaDatabaseMeta single;

    public static HanaDatabaseMeta getInstance() {
        if (single == null) {
            synchronized (HanaDatabaseMeta.class) {
                if (single == null) {
                    single = new HanaDatabaseMeta();
                }
            }
        }
        return single;
    }

    @Override
    public String getSQLQueryComment(String schemaName, String tableName, String columnName) {
        return String.format("SELECT COMMENTS FROM public.table_columns where SCHEMA_NAME = '%s' and TABLE_NAME = '%s' and COLUMN_NAME = '%s'", schemaName, tableName, columnName);
    }

    @Override
    public String getSQLQueryPrimaryKey() {
        return "select column_name from public.table_columns where SCHEMA_NAME=? and TABLE_NAME=? and INDEX_TYPE = 'FULL'";
    }

    @Override
    public String getSQLQueryTablesNameComments() {
        return "select schema_name||'.'||table_name,comments from public.tables";
    }

    @Override
    public String getSQLQueryTableNameComment() {
        return "select schema_name||'.'||table_name,comments from public.tables where schema_name=? and table_name = ?";
    }

    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return "select distinct schema_name||'.'||table_name from public.tables where schema_name='" + tableSchema[0] + "'";
    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return "select distinct schema_name from public.tables";
    }

    @Override
    public String getSQLQueryColumns(String... args) {
        return "select column_name from public.table_columns where schema_name=? and table_name = ?";
    }
}
