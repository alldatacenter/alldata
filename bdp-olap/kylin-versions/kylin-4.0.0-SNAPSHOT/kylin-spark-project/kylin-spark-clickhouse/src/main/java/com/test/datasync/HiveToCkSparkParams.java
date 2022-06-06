package com.test.datasync;

import java.util.List;

/**
 * 参数pojo
 */
public class HiveToCkSparkParams {
    /**
     * {
     *     "appName":"Spark Hive To Clickhouse",
     *     "hiveSQL":"select * from default.lineitem_hdfs_test1_local3",
     *     "clusterName":"test_cluster",
     *     "database":"default",
     *     "localTable":"lineitem_hdfs_test1_local_load1",
     *     "driver":"ru.yandex.clickhouse.ClickHouseDriver",
     *     "user":"default",
     *     "password":"test1234",
     *     "batchSize":10000,
     *     "port":"8123",
     *     "shardInsertParallelism":5,
     *     "sortKey":[
     *         "A",
     *         "B"
     *     ],
     *     "activeServers":[
     *         "10.58.3.41",
     *         "10.58.3.42",
     *         "10.58.3.43"
     *     ]
     * }
     */
    private String appName;
    private List<String> hiveSqls;
    private String shardKey;
    private String clusterName;
    private String database;
    private String localTable;
    private String driver;
    private String user;
    private String password;
    private int batchSize;
    private String port;
    private int shardInsertParallelism;
    private List<String> sortKey;
    private List<String> activeServers;


    public String getShardKey() {
        return shardKey;
    }

    public void setShardKey(String shardKey) {
        this.shardKey = shardKey;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getHiveSqls() {
        return hiveSqls;
    }

    public void setHiveSqls(List<String> hiveSqls) {
        this.hiveSqls = hiveSqls;
    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getLocalTable() {
        return localTable;
    }

    public void setLocalTable(String localTable) {
        this.localTable = localTable;
    }

    public String getDriver() {
        return driver;
    }

    public void setDriver(String driver) {
        this.driver = driver;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getShardInsertParallelism() {
        return shardInsertParallelism;
    }

    public void setShardInsertParallelism(int shardInsertParallelism) {
        this.shardInsertParallelism = shardInsertParallelism;
    }

    public List<String> getSortKey() {
        return sortKey;
    }

    public void setSortKey(List<String> sortKey) {
        this.sortKey = sortKey;
    }

    public List<String> getActiveServers() {
        return activeServers;
    }

    public void setActiveServers(List<String> activeServers) {
        this.activeServers = activeServers;
    }

    public HiveToCkSparkParams() {
    }
}