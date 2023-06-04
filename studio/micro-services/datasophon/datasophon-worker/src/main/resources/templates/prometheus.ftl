# my global config
global:
  scrape_interval:     ${scrape_interval}s # Set the scrape interval to every 15 seconds. Default is every 1 minute.
  evaluation_interval: ${evaluation_interval}s # Evaluate rules every 15 seconds. The default is every 1 minute.
  # scrape_timeout is set to the global default (10s).

# Alertmanager configuration
alerting:
  alertmanagers:
  - static_configs:
    - targets:
       - localhost:9093

# Load rules once and periodically evaluate them according to the global 'evaluation_interval'.
rule_files:
  # - "first_rules.yml"
  # - "second_rules.yml"
  - "alert_rules/*.yml"
# A scrape configuration containing exactly one endpoint to scrape:
# Here it's Prometheus itself.
scrape_configs:
  - job_name: 'prometheus'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    static_configs:
    - targets: ['localhost:9090']
  - job_name: 'grafana'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    static_configs:
    - targets: ['localhost:3000']
  - job_name: 'alertmanager'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    static_configs:
    - targets: ['localhost:9093']
  - job_name: 'pushgateway'

    # metrics_path defaults to '/metrics'
    # scheme defaults to 'http'.
    static_configs:
    - targets: ['localhost:9091']


  - job_name: 'node' #自定义名称,用于监控linux基础服务
    file_sd_configs:
     - files:
       - configs/linux.json  #linux机器IP地址json文件
  - job_name: 'namenode'  #用于监控HDFS组件
    file_sd_configs:
     - files:
       - configs/namenode.json  #hdfs参数获取地址
  - job_name: 'datanode'  #用于监控HDFS组件
    file_sd_configs:
     - files:
       - configs/datanode.json  #hdfs参数获取地址
  - job_name: 'resourcemanager' #用于监控Yarn组件
    file_sd_configs:
     - files:
       - configs/resourcemanager.json #yarn参数获取地址
  - job_name: 'zkserver' #用于监控zk组件
    file_sd_configs:
     - files:
       - configs/zkserver.json #zk参数获取地址
  - job_name: 'hiveserver2'
    file_sd_configs:
     - files:
       - configs/hiveserver2.json
  - job_name: 'spark'
    file_sd_configs:
     - files:
       - configs/spark.json
  - job_name: 'worker'
    file_sd_configs:
     - files:
       - configs/worker.json
  - job_name: 'master'
    file_sd_configs:
     - files:
       - configs/master.json
  - job_name: 'nodemanager'
    file_sd_configs:
     - files:
       - configs/nodemanager.json
  - job_name: 'kafkabroker'
    file_sd_configs:
     - files:
       - configs/kafkabroker.json
  - job_name: 'hbasemaster'
    file_sd_configs:
     - files:
       - configs/hbasemaster.json
  - job_name: 'regionserver'
    file_sd_configs:
     - files:
       - configs/regionserver.json
  - job_name: 'zkfc'
    file_sd_configs:
     - files:
       - configs/zkfc.json
  - job_name: 'journalnode'
    file_sd_configs:
     - files:
       - configs/journalnode.json
  - job_name: 'historyserver'
    file_sd_configs:
     - files:
       - configs/historyserver.json
  - job_name: 'hivemetastore'
    file_sd_configs:
     - files:
       - configs/hivemetastore.json
  - job_name: 'trinocoordinator'
    file_sd_configs:
     - files:
       - configs/trinocoordinator.json
  - job_name: 'trinoworker'
    file_sd_configs:
     - files:
       - configs/trinoworker.json
  - job_name: 'StarRocks'
    metrics_path: '/metrics'
    file_sd_configs:
     - files:
       - configs/starrocks.json
  - job_name: 'doris'
    metrics_path: '/metrics'
    file_sd_configs:
     - files:
       - configs/doris.json
  - job_name: 'rangeradmin'
    file_sd_configs:
     - files:
       - configs/rangeradmin.json
  - job_name: 'jobmanager'
    file_sd_configs:
     - files:
       - configs/jobmanager.json
  - job_name: 'taskmanager'
    file_sd_configs:
     - files:
       - configs/taskmanager.json
  - job_name: 'esexporter'
    file_sd_configs:
     - files:
       - configs/esexporter.json
  - job_name: 'apiserver'
    file_sd_configs:
     - files:
       - configs/apiserver.json
  - job_name: 'masterserver'
    file_sd_configs:
     - files:
       - configs/masterserver.json
  - job_name: 'workerserver'
    file_sd_configs:
     - files:
       - configs/workerserver.json
  - job_name: 'alertserver'
    file_sd_configs:
     - files:
       - configs/alertserver.json
  - job_name: 'streampark'
    file_sd_configs:
     - files:
       - configs/streampark.json
  - job_name: 'dinky'
    file_sd_configs:
     - files:
       - configs/dinky.json


