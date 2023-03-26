package com.platform.admin.tool.meta;

/**
 * meta信息interface
 *
 * @author AllDataDC
 * @ClassName BaseDatabaseMeta
 */
public abstract class BaseDatabaseMeta implements DatabaseInterface {

    @Override
    public String getSQLQueryFields(String tableName) {
        return "SELECT * FROM " + tableName + " where 1=0";
    }

    @Override
    public String getSQLQueryTablesNameComments() {
        return "select table_name,table_comment from information_schema.tables where table_schema=?";
    }

    @Override
    public String getSQLQueryTableNameComment() {
        return "select table_name,table_comment from information_schema.tables where table_schema=? and table_name = ?";
    }

    @Override
    public String getSQLQueryPrimaryKey() {
        return null;
    }

    @Override
    public String getSQLQueryComment(String schemaName, String tableName, String columnName) {
        return null;
    }

    @Override
    public String getSQLQueryColumns(String... args) {
        return null;
    }

    @Override
    public String getMaxId(String tableName, String primaryKey) {
        return String.format("select max(%s) from %s",primaryKey,tableName);
    }

    @Override
    public String getSQLQueryTableSchema(String... args) {
        return null;
    }

    @Override
    public String getSQLQueryTables() {
        return null;
    }

    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return null;
    }
}
