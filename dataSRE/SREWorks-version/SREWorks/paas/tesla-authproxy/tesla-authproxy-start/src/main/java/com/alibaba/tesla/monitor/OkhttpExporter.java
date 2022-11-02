package com.alibaba.tesla.monitor;

import com.alibaba.tesla.common.utils.TeslaOKHttpClient;
import io.prometheus.client.Collector;
import io.prometheus.client.GaugeMetricFamily;
import okhttp3.OkHttpClient;
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
public class OkhttpExporter extends Collector {

    @Override
    public List<MetricFamilySamples> collect() {
        List<MetricFamilySamples> mfs = new ArrayList<>();

        // Okhttp
        GaugeMetricFamily okhttpStatus = new GaugeMetricFamily("okhttp_status",
            "okhttp status", Collections.singletonList("type"));
        OkHttpClient okhttp = TeslaOKHttpClient.getOkHttpClient();
        int httpConnectionCount = okhttp.connectionPool().connectionCount();
        int httpIdleCount = okhttp.connectionPool().idleConnectionCount();
        okhttpStatus.addMetric(Collections.singletonList("connection_count"), httpConnectionCount);
        okhttpStatus.addMetric(Collections.singletonList("idle_count"), httpIdleCount);
        mfs.add(okhttpStatus);

        return mfs;
    }
}