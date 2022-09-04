package com.elasticsearch.cloud.monitor.metric.mapping.pojo;

import com.google.common.base.Objects;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author xiaoping
 * @date 2021/6/23
 */
@Setter
@Getter
@NoArgsConstructor
public class MetricMappingInfo {
    private String tenant;
    private String metric;
    private Boolean metric_pk;
    private Integer number;
    private Long timestamp;

    private String referenceTenant;
    private String referenceMetric;

    public MetricMappingInfo(String tenant, String metric) {
        this.tenant = tenant;
        this.metric = metric;
    }

    public MetricMappingInfo(String metric) {
        this.metric = metric;
    }

    public MetricMappingInfo(String tenant, String metric, String referenceTenant, String referenceMetric) {
        this.tenant = tenant;
        this.metric = metric;
        this.referenceMetric = referenceMetric;
        this.referenceTenant = referenceTenant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        MetricMappingInfo that = (MetricMappingInfo)o;
        return Objects.equal(tenant, that.tenant) &&
            Objects.equal(metric, that.metric) &&
            Objects.equal(referenceTenant, that.referenceTenant) &&
            Objects.equal(referenceMetric, that.referenceMetric);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tenant, metric, referenceTenant, referenceMetric);
    }
}
