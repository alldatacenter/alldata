package com.platform.flink;

public class Constants {

    public static final String KAFKA_BOOTSTRAP_SERVERS_KEY = "kafka.bootstrap.servers";

    public static final String KAFKA_GROUP_KEY = "kafka.group.id";

    public static final String KAFKA_TOPIC_KEY = "kafka.topic";

    public static final String ES_HOSTS_KEY = "es.hosts";

    public static final String CAL_CONF_KEY = "item.session.conf";

    public static final String PROPERTIES_FILE_NAME = "./application.properties";

    public static final String HDFS_CHECKPOINT_PATH_KEY = "checkpoint.hdfs.path";

    public static final String CHECKPOINT_ENABLE_KEY = "checkpoint.enable";

    public static final String PARALLELISM_KEY = "flink.parallelism";

    public static final String SINK_TIME_INTERVAL_SEC_KEY = "flink.sink.interval";

    public static final String TOTAL_SCENE_ID = "total";

    public static final String WS_URL = "ws://localhost:8082/ws";

    public static final String MAIN_CLASS = "com._4paradigm.cess.job.recall.Main";

    public static final String TOPIC = "uv_pv";

    public static final String KAFKA_BOOTSTRAP_SERVER = "master:9092,node01:9092,node02:9092";

    public static final String KAFKA_SERIALIZER = "kafka.serializer.DefaultEncoder";
    

}
