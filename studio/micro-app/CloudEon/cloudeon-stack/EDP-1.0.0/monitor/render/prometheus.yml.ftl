# my global config
global:
  scrape_interval:     15s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: 15s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
  - static_configs:
    - targets:
      # - alertmanager:9093
      <#if serviceRoles['ALERTMANAGER']?size gt 0>
        - ${serviceRoles['ALERTMANAGER'][0].hostname}:${conf['alertmanager.http.port']}
      </#if>


# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"
  - "/opt/edp/${service.serviceName}/conf/rule/*.yml"
<#assign node_exporters=[]>
<#list serviceRoles['NODEEXPORTER'] as role>
  <#assign node_exporters += ["'"+role.hostname + ":" + conf["nodeexporter.http.port"]+"'"]>
</#list>
# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
#  - job_name: 'pushgateway'
#    static_configs:
#    - targets: ['localhost:9091']
  - job_name: 'prometheus'
    static_configs:
    - targets: ['${serviceRoles['PROMETHEUS'][0].hostname}:${conf['prometheus.http.port']}']
  - job_name: 'grafana'
    static_configs:
    - targets: ['${serviceRoles['GRAFANA'][0].hostname}:${conf['grafana.http.port']}']
  - job_name: 'alertmanager'
    static_configs:
    - targets: ['${serviceRoles['ALERTMANAGER'][0].hostname}:${conf['alertmanager.http.port']}']
  - job_name: node_exporter
    honor_timestamps: true
    scrape_interval: 5s
    scrape_timeout: 5s
    metrics_path: /metrics
    scheme: http
    static_configs:
    - targets: [${node_exporters?join(",")}]
  - job_name: 'namenode'
    file_sd_configs:
    - files: ['discovery_configs/namenode.json']
  - job_name: 'datanode'
    file_sd_configs:
    - files: ['discovery_configs/datanode.json']
  - job_name: 'zkfc'
    file_sd_configs:
    - files: ['discovery_configs/zkfc.json']
  - job_name: 'journalnode'
    file_sd_configs:
    - files: ['discovery_configs/journalnode.json']
  - job_name: 'resourcemanager'
    file_sd_configs:
    - files: ['discovery_configs/resourcemanager.json']
  - job_name: 'nodemanager'
    file_sd_configs:
    - files: ['discovery_configs/nodemanager.json']
  - job_name: 'zkserver'
    file_sd_configs:
    - files: ['discovery_configs/zkserver.json']
  - job_name: 'kafkabroker'
    file_sd_configs:
    - files: ['discovery_configs/kafka-broker.json']
  - job_name: 'hbasemaster'
    file_sd_configs:
    - files: ['discovery_configs/hbase-master.json']
  - job_name: 'regionserver'
    file_sd_configs:
    - files: ['discovery_configs/region-server.json']
  - job_name: 'PALO_CLUSTER'
    metrics_path: '/metrics'
    file_sd_configs:
    - files: ['discovery_configs/doris.json']
  - job_name: 'hivemetastore'
    file_sd_configs:
    - files: ['discovery_configs/metastore.json']
  - job_name: 'hiveserver2'
    file_sd_configs:
    - files: ['discovery_configs/hiveserver2.json']
  - job_name: 'sparkhistoryserver'
    file_sd_configs:
    - files: ['discovery_configs/sparkhistoryserver.json']
  - job_name: 'apiserver'
    metrics_path: '/dolphinscheduler/actuator/prometheus'
    file_sd_configs:
    - files: ['discovery_configs/ds-apiserver.json']
  - job_name: 'masterserver'
    metrics_path: '/actuator/prometheus'
    file_sd_configs:
    - files: ['discovery_configs/ds-masterserver.json']
  - job_name: 'workerserver'
    metrics_path: '/actuator/prometheus'
    file_sd_configs:
    - files: ['discovery_configs/ds-workerserver.json']
  - job_name: 'alertserver'
    metrics_path: '/actuator/prometheus'
    file_sd_configs:
    - files: ['discovery_configs/ds-alertserver.json']