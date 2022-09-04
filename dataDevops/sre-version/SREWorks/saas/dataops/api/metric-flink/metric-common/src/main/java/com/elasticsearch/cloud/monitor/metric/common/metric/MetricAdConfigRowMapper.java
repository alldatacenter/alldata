package com.elasticsearch.cloud.monitor.metric.common.metric;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetricAdConfigRowMapper implements RowMapper<MetricAdConfig> {

    @Override
    public MetricAdConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
        MetricAdConfig metricAdConfig = new MetricAdConfig();
        metricAdConfig.setId(rs.getString("id"));
        metricAdConfig.setTitle(rs.getString("title"));
        metricAdConfig.setRuleId(rs.getLong("rule_id"));
        metricAdConfig.setMetricId(rs.getString("metric_id"));
        metricAdConfig.setEnable(rs.getBoolean("enable"));
        return metricAdConfig;
    }

}
