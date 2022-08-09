package com.test.datasync;


/**
 * clickhouse配置类
 */
public interface ClickhouseJdbcConfig {
    String getClusterName();

    String getDatabase();

    /**
     * 使用本地表，因为数据存储在在本地表中，并且导入本地表有利于数据块的合并
     */
    String getLocalTable();

    String getDriver();

    String getAutoCommit();

    String getUser();

    String getPassword();

    int getBatchSize();

    String getPort();
}

