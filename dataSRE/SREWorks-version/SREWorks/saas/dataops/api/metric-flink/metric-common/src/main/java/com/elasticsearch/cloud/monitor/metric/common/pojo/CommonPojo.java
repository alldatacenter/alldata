package com.elasticsearch.cloud.monitor.metric.common.pojo;

import com.google.common.base.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * @author xiaoping
 * @date 2020/5/7
 */
public class CommonPojo {
    @Setter
    @Getter
    @NoArgsConstructor
    public static class OssRoutingData {
        private String tenant;
        private String projectId;

        private RoutingInfo routingInfo;
        private EsClusterConf es;

        public OssRoutingData(String tenant, String projectId,
            RoutingInfo routingInfo, EsClusterConf es) {
            this.tenant = tenant;
            this.projectId = projectId;
            this.routingInfo = routingInfo;
            this.es = es;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class EsClusterConf {
        private String user;
        private String password;
        private String esHost;
        private String httpType;
        private Integer port;
        private String defaultIndexPre;
        private Map<String, Integer> rebalanceTotalLevel;
        private String routingTagKey;
        private boolean searchFilterEnable;
        private boolean forceSearch;
        private boolean closeRouting;
        private boolean multiValueEnable;
        private boolean dataStreamSearchEnable = false;
        private String metricMappingKey;

        public EsClusterConf(String user, String password, String esHost, String httpType, Integer port) {
            this.user = user;
            this.password = password;
            this.esHost = esHost;
            this.httpType = httpType;
            this.port = port;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            EsClusterConf that = (EsClusterConf)o;
            return Objects.equal(user, that.user) &&
                Objects.equal(password, that.password) &&
                Objects.equal(esHost, that.esHost) &&
                Objects.equal(httpType, that.httpType) &&
                Objects.equal(port, that.port);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(user, password, esHost, httpType, port);
        }
    }

    @Data
    @NoArgsConstructor
    public static class RoutingInfo {
        private String clusterName;
        private String slsProject;
        private String slsLogStore;
        private String accessKey;
        private String accessSecret;
        private String slsEndpoint;

        public RoutingInfo(String clusterName, String slsProject, String slsLogStore, String accessKey,
            String accessSecret, String slsEndpoint) {
            this.clusterName = clusterName;
            this.slsProject = slsProject;
            this.slsLogStore = slsLogStore;
            this.accessKey = accessKey;
            this.accessSecret = accessSecret;
            this.slsEndpoint = slsEndpoint;
        }
    }
}
