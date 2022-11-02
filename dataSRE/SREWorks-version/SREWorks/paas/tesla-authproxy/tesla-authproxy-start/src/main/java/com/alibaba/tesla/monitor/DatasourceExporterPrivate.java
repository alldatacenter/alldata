package com.alibaba.tesla.monitor;

import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 指标导出
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Component
public class DatasourceExporterPrivate extends Collector {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        // Datasource
        GaugeMetricFamily datasourceStatus = new GaugeMetricFamily("mysql_datasource_status",
            "jdbc datasource status", Collections.singletonList("type"));
        DataSource datasource = (DataSource)jdbcTemplate.getDataSource();
        datasourceStatus.addMetric(Collections.singletonList("max_active"), datasource.getMaxActive());
        datasourceStatus.addMetric(Collections.singletonList("num_active"), datasource.getNumActive());
        datasourceStatus.addMetric(Collections.singletonList("max_idle"), datasource.getMaxIdle());
        datasourceStatus.addMetric(Collections.singletonList("num_idle"), datasource.getNumIdle());
        datasourceStatus.addMetric(Collections.singletonList("borrowed_count"), datasource.getBorrowedCount());
        datasourceStatus.addMetric(Collections.singletonList("created_count"), datasource.getCreatedCount());
        datasourceStatus.addMetric(Collections.singletonList("reconnected_count"), datasource.getReconnectedCount());
        datasourceStatus.addMetric(Collections.singletonList("released_count"), datasource.getReleasedCount());
        datasourceStatus.addMetric(Collections.singletonList("returned_count"), datasource.getReturnedCount());
        datasourceStatus.addMetric(Collections.singletonList("wait_count"), datasource.getWaitCount());
        datasourceStatus.addMetric(Collections.singletonList("released_idle_count"),
            datasource.getReleasedIdleCount());
        datasourceStatus.addMetric(Collections.singletonList("remove_abandoned_count"),
            datasource.getRemoveAbandonedCount());
        mfs.add(datasourceStatus);

        return mfs;
    }
}