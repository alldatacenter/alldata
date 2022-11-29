package cn.datax.service.data.quality.schedule.rules;

import cn.datax.common.database.constants.DbType;

import java.util.Map;

public interface RuleItem {

    String parse(DbType dbType, String table, String column, Map<String, Object> map);

    String code();
}
