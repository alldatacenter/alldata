package cn.datax.common.database.constants;

/**
 * 数据库类型
 *
 * @author AllDataDC
 * @date 2022-11-14
 */
public enum DbType {

    /**
     * MYSQL
     */
    MYSQL("1", "MySql数据库", "jdbc:mysql://${host}:${port}/${dbName}?useUnicode=true&characterEncoding=utf-8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8"),
    /**
     * MARIADB
     */
    MARIADB("2", "MariaDB数据库", "jdbc:mariadb://${host}:${port}/${dbName}"),
    /**
     * ORACLE
     */
    ORACLE("3", "Oracle11g及以下数据库", "jdbc:oracle:thin:@${host}:${port}:${sid}"),
    /**
     * oracle12c new pagination
     */
    ORACLE_12C("4", "Oracle12c+数据库", "jdbc:oracle:thin:@${host}:${port}:${sid}"),
    /**
     * POSTGRESQL
     */
    POSTGRE_SQL("5", "PostgreSql数据库", "jdbc:postgresql://${host}:${port}/${dbName}"),
    /**
     * SQLSERVER2005
     */
    SQL_SERVER2008("6", "SQLServer2008及以下数据库", "jdbc:sqlserver://${host}:${port};DatabaseName=${dbName}"),
    /**
     * SQLSERVER
     */
    SQL_SERVER("7", "SQLServer2012+数据库", "jdbc:sqlserver://${host}:${port};DatabaseName=${dbName}"),
    /**
     * UNKONWN DB
     */
    OTHER("8", "其他数据库", "");

    /**
     * 数据库名称
     */
    private final String db;

    /**
     * 描述
     */
    private final String desc;

    /**
     * url
     */
    private final String url;

    public String getDb() {
        return this.db;
    }

    public String getDesc() {
        return this.desc;
    }

    public String getUrl() {
        return this.url;
    }

    DbType(String db, String desc, String url) {
        this.db = db;
        this.desc = desc;
        this.url = url;
    }

    /**
     * 获取数据库类型
     *
     * @param dbType 数据库类型字符串
     */
    public static DbType getDbType(String dbType) {
        for (DbType type : DbType.values()) {
            if (type.db.equals(dbType)) {
                return type;
            }
        }
        return OTHER;
    }
}
