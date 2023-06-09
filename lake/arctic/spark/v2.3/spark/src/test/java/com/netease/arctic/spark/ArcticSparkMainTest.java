package com.netease.arctic.spark;


import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.util.Map;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestKeyedTableDataFrameAPI.class,
    TestUnKeyedTableDataFrameAPI.class,
    TestComplexType.class,
    TestCreateTableDDL.class,
    TestCreateTableAsSelectDDL.class,
    TestKeyedHiveTableInsertOverwriteDynamic.class,
    TestKeyedHiveTableMergeOnRead.class,
    TestOptimizeWrite.class,
    TestUnkeyedHiveTableInsertOverwriteDynamic.class,
    TestUnkeyedHiveTableMergeOnRead.class
})
public class ArcticSparkMainTest {
    @BeforeClass
    public static void suiteSetup() throws IOException, ClassNotFoundException {
        Map<String, String> configs = Maps.newHashMap();
        Map<String, String> arcticConfigs = SparkTestContext.setUpTestDirAndArctic();
        Map<String, String> hiveConfigs = SparkTestContext.setUpHMS();
        configs.putAll(arcticConfigs);
        configs.putAll(hiveConfigs);
        SparkTestContext.setUpSparkSession(configs);
    }

    @AfterClass
    public static void suiteTeardown() {
        SparkTestContext.cleanUpAms();
        SparkTestContext.cleanUpHive();
        SparkTestContext.cleanUpSparkSession();
    }
}
