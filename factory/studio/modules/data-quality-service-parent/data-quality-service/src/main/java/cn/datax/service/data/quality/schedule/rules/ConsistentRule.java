package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 一致性核查
 * 核查项:字典
 * select sum(case when column not in ('0', '1') then 1 else 0 end), count(*) from table;
 */
public class ConsistentRule implements RuleItem {

    private static String GB_ITEM = "gb_item";

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT SUM(CASE WHEN ").append(column).append(" NOT IN (").append(map.get(GB_ITEM)).append(") THEN 1 ELSE 0 END), COUNT(*) FROM ").append(table);
        return builder.toString();
    }

    @Override
    public String code() {
        return "consistent_key";
    }
}
