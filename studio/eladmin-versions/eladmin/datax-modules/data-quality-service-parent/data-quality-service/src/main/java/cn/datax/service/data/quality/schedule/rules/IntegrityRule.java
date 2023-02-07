package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 完整性核查
 * 核查项:非空
 * select sum(case when column is not null and trim(column) != '' then 1 else 0 end), count(*) from table;
 */
public class IntegrityRule implements RuleItem {

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT SUM(CASE WHEN ").append(column).append(" IS NOT NULL AND TRIM(").append(column).append(") != '' THEN 0 ELSE 1 END), COUNT(*) FROM ").append(table);
        return builder.toString();
    }

    @Override
    public String code() {
        return "integrity_key";
    }
}
