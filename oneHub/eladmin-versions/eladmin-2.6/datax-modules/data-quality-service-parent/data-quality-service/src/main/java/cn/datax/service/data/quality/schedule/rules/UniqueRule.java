package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

/**
 * 唯一性核查
 * 核查项:主键
 * select count(distinct id), count(*) from table;
 */
public class UniqueRule implements RuleItem {

    @Override
    public String parse(DbType dbType, String table, String column, Map<String, Object> map) {
        final StringBuilder builder = new StringBuilder();
        builder.append("SELECT totalCount - errorCount AS errorCount, totalCount FROM (");
        builder.append("SELECT COUNT(DISTINCT ").append(column).append(") AS errorCount, COUNT(*) AS totalCount FROM ").append(table);
        builder.append(") TEMP");
        return builder.toString();
    }

    @Override
    public String code() {
        return "unique_key";
    }
}
