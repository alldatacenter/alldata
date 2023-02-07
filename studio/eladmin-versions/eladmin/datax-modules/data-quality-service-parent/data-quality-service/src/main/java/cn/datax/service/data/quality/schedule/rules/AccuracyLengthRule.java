package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 准确性核查
 * 核查项:最大长度
 * select sum(case when length(column) > 15 then 1 else 0 end), count(*) from table;
 */
public class AccuracyLengthRule implements RuleItem {

    private static String MAX_LENGTH = "max_length";

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT SUM(CASE WHEN LENGTH(").append(column).append(") > ").append(map.get(MAX_LENGTH)).append(" THEN 1 ELSE 0 END), COUNT(*) FROM ").append(table);
        return builder.toString();
    }

    @Override
    public String code() {
        return "accuracy_key_length";
    }
}
