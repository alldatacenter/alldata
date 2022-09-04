package com.elasticsearch.cloud.monitor.metric.common.metric;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetricInstanceRowMapper implements RowMapper<MetricInstance> {

    @Override
    public MetricInstance mapRow(ResultSet rs, int rowNum) throws SQLException {
        MetricInstance metricInstance = new MetricInstance();
        metricInstance.setId(rs.getString("id"));
        metricInstance.setName(rs.getString("name"));
        metricInstance.setIndexPath(rs.getString("index_path"));
        metricInstance.setIndexTags(rs.getString("index_tags"));
        metricInstance.setMetricId(rs.getString("metric_id"));
        metricInstance.setMetricName(rs.getString("metric_name"));

        return metricInstance;
    }

}