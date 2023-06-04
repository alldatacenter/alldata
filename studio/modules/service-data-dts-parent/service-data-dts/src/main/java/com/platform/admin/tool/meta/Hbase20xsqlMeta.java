package com.platform.admin.tool.meta;

/**
 * MySQL数据库 meta信息查询
 *
 * @author AllDataDC
 */
public class Hbase20xsqlMeta extends BaseDatabaseMeta implements DatabaseInterface {

    private volatile static Hbase20xsqlMeta single;

    public static Hbase20xsqlMeta getInstance() {
        if (single == null) {
            synchronized (Hbase20xsqlMeta.class) {
                if (single == null) {
                    single = new Hbase20xsqlMeta();
                }
            }
        }
        return single;
    }


    @Override
    public String getSQLQueryTables(String... tableSchema) {
        return null;
    }
}
