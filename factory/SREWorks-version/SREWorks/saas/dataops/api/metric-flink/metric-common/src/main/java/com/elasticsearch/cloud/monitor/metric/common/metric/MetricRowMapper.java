package com.elasticsearch.cloud.monitor.metric.common.metric;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class MetricRowMapper implements RowMapper<Metric> {

    @Override
    public Metric mapRow(ResultSet rs, int rowNum) throws SQLException {
        Metric metric = new Metric();
        metric.setId(rs.getString("id"));
        metric.setName(rs.getString("name"));
        metric.setType(rs.getString("type"));
        metric.setIndexPath(rs.getString("index_path"));
        metric.setTags(rs.getString("tags"));
        metric.setTeamId(rs.getString("team_id"));
        metric.setAppId(rs.getString("app_id"));

        return metric;
    }

}
