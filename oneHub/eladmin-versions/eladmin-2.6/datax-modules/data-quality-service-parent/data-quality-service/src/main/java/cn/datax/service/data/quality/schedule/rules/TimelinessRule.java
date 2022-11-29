package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 及时性核查
 */
public class TimelinessRule implements RuleItem {

    private static String THRESHOLD = "threshold";

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        switch (dbType) {
            case ORACLE:
            case ORACLE_12C:
                builder.append("SELECT SUM(CASE WHEN ROUND(TO_NUMBER(SYSDATE - ").append(column).append(")) >= ").append(map.get(THRESHOLD)).append(" THEN 1 ELSE 0 END), COUNT(*) FROM ").append(table);
                break;
            case MYSQL:
            case MARIADB:
                builder.append("SELECT SUM(CASE WHEN DATEDIFF(NOW(), ").append(column).append(") >= ").append(map.get(THRESHOLD)).append(" THEN 1 ELSE 0 END), COUNT(*) FROM ").append(table);
                break;
            case SQL_SERVER:
            case SQL_SERVER2008:
                builder.append("SELECT SUM(CASE WHEN DATEDIFF(DAY, ").append(column).append(", GETDATE()) >= ").append(map.get(THRESHOLD)).append(" THEN 1 ELSE 0 END), COUNT(*) FROM ").append(table);
                break;
            case POSTGRE_SQL:
            case OTHER:
            default:
                break;
        }
        return builder.toString();
    }

    @Override
    public String code() {
        return "timeliness_key";
    }
}
