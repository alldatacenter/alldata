package com.datasophon.api.strategy;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServiceRoleStrategyContext {
    private static final Map<String,ServiceRoleStrategy> map = new ConcurrentHashMap<>();

    static {
        map.put("NameNode",new NameNodeHandlerStrategy());
        map.put("ResourceManager",new RMHandlerStrategy());
        map.put("HiveMetaStore",new HiveMetaStroreHandlerStrategy());
        map.put("HiveServer2",new HiveServer2HandlerStrategy());
        map.put("Grafana",new GrafanaHandlerStrategy());
        map.put("ZkServer",new ZkServerHandlerStrategy());
        map.put("HistoryServer",new HistoryServerHandlerStrategy());
        map.put("TrinoCoordinator",new TrinoHandlerStrategy());
        map.put("JournalNode",new JournalNodeHandlerStrategy());
        map.put("ZKFC",new ZKFCHandlerStrategy());
        map.put("SRFE",new FEHandlerStartegy());
        map.put("DorisFE",new FEHandlerStartegy());
        map.put("SRBE",new BEHandlerStartegy());
        map.put("DorisBE",new BEHandlerStartegy());
        map.put("Krb5Kdc",new Krb5KdcHandlerStrategy());
        map.put("KAdmin",new KAdminHandlerStrategy());
        map.put("RangerAdmin",new RangerAdminHandlerStrategy());
        map.put("ElasticSearch",new ElasticSearchHandlerStrategy());
        map.put("Prometheus",new PrometheusHandlerStrategy());
        map.put("AlertManager",new AlertManagerHandlerStrategy());

        map.put("RANGER",new RangerAdminHandlerStrategy());
        map.put("ZOOKEEPER",new ZkServerHandlerStrategy());
        map.put("YARN",new RMHandlerStrategy());
        map.put("HDFS",new NameNodeHandlerStrategy());
        map.put("HIVE",new HiveServer2HandlerStrategy());
        map.put("KAFKA",new KafkaHandlerStrategy());
        map.put("HBASE",new HBaseHandlerStrategy());
        map.put("FLINK",new FlinkHandlerStrategy());
    }

    public static ServiceRoleStrategy getServiceRoleHandler(String type){
        if(StringUtils.isBlank(type)){
            return null;
        }
        return map.get(type);
    }
}
