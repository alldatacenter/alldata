package com.elasticsearch.cloud.monitor.metric.common.metric;

import com.elasticsearch.cloud.monitor.metric.common.utils.MysqlClient;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * 指标实例缓存类
 *
 * @author: fangzong.lyj
 * @date: 2021/09/05 14:11
 */
@Data
public class MetricInstancesAdRuleCache {

    private String teamId;

    private String appId;

    private String metricName;

    /**
     * 缓存时长 TODO 支持缓存条目数
     */
    private long maxDuration;

    private List<MetricInstanceAdRule> metricInstanceAdRuleList = new ArrayList<>();

    public void recovery() {
        List<Metric> metrics = getMetrics(teamId, appId, metricName);
        if (metrics.isEmpty()) {
            return;
        }

        for (Metric metric : metrics) {
            List<MetricAdConfig> metricAdConfigs = getMetricADConfigs(metric.getId());
            if (metricAdConfigs.isEmpty()) {
                continue;
            }

            List<MetricInstance> metricInstances = getMetricInstances(metric.getId());
            if (metricInstances.isEmpty()) {
                continue;
            }

            for(MetricAdConfig adConfig : metricAdConfigs) {
                metricInstances.forEach(metricInstance -> {
                    MetricInstanceAdRule metricInstanceAdRule = new MetricInstanceAdRule();
                    metricInstanceAdRule.setMetricId(metricInstance.getMetricId());
                    metricInstanceAdRule.setMetricName(metricInstance.getMetricName());
                    metricInstanceAdRule.setMetricInstanceId(metricInstance.getId());
                    metricInstanceAdRule.setIndexPath(metricInstance.getIndexPath());
                    metricInstanceAdRule.setIndexTags(metricInstance.getIndexTags());
                    metricInstanceAdRule.setEnable(adConfig.getEnable());
                    metricInstanceAdRule.setAdTitle(adConfig.getTitle());
                    metricInstanceAdRule.setRuleId(adConfig.getRuleId());
                    metricInstanceAdRule.setTeamId(metric.getTeamId());
                    metricInstanceAdRule.setAppId(metric.getAppId());

                    metricInstanceAdRuleList.add(metricInstanceAdRule);
                });
            }
        }
    }

    private List<Metric> getMetrics(String teamId, String appId, String metricName) {
        String sql = "SELECT * FROM metric";
        if (filterByTeam(teamId)) {
            sql += " WHERE team_id='" + teamId + "'";
            if (filterByApp(appId)) {
                sql += " AND app_id='" + appId + "'";
            }

            if (StringUtils.isNotEmpty(metricName)) {
                sql += " AND metric_name='" + metricName + "'";
            }
        } else {
            if (StringUtils.isNotEmpty(metricName)) {
                sql += " WHERE metric_name='" + metricName + "'";
            }
        }

        JdbcTemplate jdbcTemplate = new JdbcTemplate(MysqlClient.getDataSource());
        return jdbcTemplate.query(sql, new MetricRowMapper());
    }

//    private List<MetricInstance> getMetricInstances(Set<String> metricIds) {
//        String metricIdsStr = String.join("','", metricIds);
//        String sql = "SELECT * FROM metric_instance WHERE metric_id in ('" + metricIdsStr + "')";
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(MysqlClient.getDataSource());
//        return jdbcTemplate.query(sql, new MetricInstanceRowMapper());
//    }
//
//    private List<MetricAdConfig> getMetricADConfigs(Set<String> metricIds) {
//        String metricIdsStr = String.join("','", metricIds);
//        String sql = "SELECT * FROM metric_anomaly_detection_config WHERE metric_id in ('" + metricIdsStr + "')";
//        JdbcTemplate jdbcTemplate = new JdbcTemplate(MysqlClient.getDataSource());
//        return jdbcTemplate.query(sql, new MetricAdConfigRowMapper());
//    }

    private List<MetricInstance> getMetricInstances(String metricId) {
        String sql = String.format("SELECT * FROM metric_instance WHERE metric_id=%s", metricId);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(MysqlClient.getDataSource());
        return jdbcTemplate.query(sql, new MetricInstanceRowMapper());
    }

    private List<MetricAdConfig> getMetricADConfigs(String metricId) {
        String sql = String.format("SELECT * FROM metric_anomaly_detection_config WHERE metric_id=%s", metricId);
        JdbcTemplate jdbcTemplate = new JdbcTemplate(MysqlClient.getDataSource());
        return jdbcTemplate.query(sql, new MetricAdConfigRowMapper());
    }

    private boolean filterByTeam(String teamId) {
        return !(StringUtils.isEmpty(teamId) || Long.parseLong(teamId) < 0);
    }

    private boolean filterByApp(String appId) {
        return !(StringUtils.isEmpty(appId) || Long.parseLong(appId) < 0);
    }

}
