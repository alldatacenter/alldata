package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 关联性核查
 * select SUM(errorCount) errorCount, SUM(totalCount) totalCount
 * FROM (
 * select count(*) errorCount, 0 as totalCount from MAIN_TABLE a where not exists(select 1 from FOLLOW_TWO b where a.NAME = b.NAME)
 * union select 0 as errorCount, count(*) totalCount from MAIN_TABLE
 * ) temp;
 */
public class RelevanceRule implements RuleItem {

    private static String RELATED_TABLE = "related_table";
    private static String RELATED_COLUMN = "related_column";

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT SUM(errorCount) AS errorCount, SUM(totalCount) AS totalCount FROM (")
                .append("SELECT COUNT(*) AS errorCount, 0 AS totalCount FROM ")
                .append(table).append(" a WHERE NOT EXISTS (SELECT 1 FROM ").append(map.get(RELATED_TABLE)).append(" b WHERE a.").append(column).append(" = b.").append(map.get(RELATED_COLUMN)).append(")")
                .append("UNION SELECT 0 AS errorCount, COUNT(*) AS totalCount FROM ").append(table).append(") TEMP");
        return builder.toString();
    }

    @Override
    public String code() {
        return "relevance_key";
    }
}
