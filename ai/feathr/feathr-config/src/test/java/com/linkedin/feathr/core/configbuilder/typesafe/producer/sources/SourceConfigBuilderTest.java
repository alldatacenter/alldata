package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.linkedin.feathr.core.configbuilder.typesafe.AbstractConfigBuilderTest;
import com.linkedin.feathr.core.config.ConfigObj;
import com.linkedin.feathr.core.configbuilder.ConfigBuilderException;
import com.typesafe.config.Config;
import java.util.function.BiFunction;
import org.testng.annotations.Test;


public class SourceConfigBuilderTest extends AbstractConfigBuilderTest {

  BiFunction<String, Config, ConfigObj> configBuilder = SourceConfigBuilder::build;

  @Test(description = "Tests HDFS config without 'type' field")
  public void hdfsConfigTest1() {
    testConfigBuilder(SourcesFixture.hdfsSource1ConfigStr, configBuilder, SourcesFixture.expHdfsSource1ConfigObj);
  }

  @Test(description = "Tests HDFS config with 'type' field")
  public void hdfsConfigTest2() {
    testConfigBuilder(SourcesFixture.hdfsSource2ConfigStr, configBuilder, SourcesFixture.expHdfsSource2ConfigObj);
  }

  @Test(description = "Tests HDFS config with Dali URI")
  public void hdfsConfigTest3() {
    testConfigBuilder(SourcesFixture.hdfsSource3ConfigStr, configBuilder, SourcesFixture.expHdfsSource3ConfigObj);
  }

  @Test(description = "Tests HDFS config with sliding time window")
  public void hdfsConfigTest4() {
    testConfigBuilder(SourcesFixture.hdfsSource4ConfigStr, configBuilder, SourcesFixture.expHdfsSource4ConfigObj);
  }

  @Test(description = "Tests HDFS config with timePartitionPattern")
  public void hdfsConfigTest5WithTimePartitionPattern() {
    testConfigBuilder(
        SourcesFixture.hdfsSource5ConfigStrWithTimePartitionPattern, configBuilder, SourcesFixture.expHdfsSource5ConfigObjWithTimePartitionPattern);
  }

  @Test(description = "Tests HDFS config with sliding time window")
  public void hdfsConfigTest6WithLegacyTimeWindowParameters() {
    testConfigBuilder(
        SourcesFixture.hdfsSource6ConfigStrWithLegacyTimeWindowParameters, configBuilder, SourcesFixture.expHdfsSource6ConfigObjWithLegacyTimeWindowParameters);
  }

  @Test(description = "It should fail if both timePartitionPattern and isTimeSeries is set.", expectedExceptions = ConfigBuilderException.class)
  public void hdfsConfigTestWithTimePartitionPatternAndIsTimeSeries() {
    buildConfig(SourcesFixture.invalidHdfsSourceconfigStrWithTimePartitionPatternAndIsTimeSeries, configBuilder);
  }

  @Test(description = "It should fail if both hasTimeSnapshot and isTimeSeries is set.", expectedExceptions = ConfigBuilderException.class)
  public void hdfsConfigTestWithHasTimeSnapshotAndIsTimeSeries() {
    buildConfig(SourcesFixture.invalidHdfsSourceconfigStrWithHasTimeSnapshotAndIsTimeSeries, configBuilder);
  }

  @Test(description = "Tests Espresso config")
  public void espressoConfigTest1() {
    testConfigBuilder(SourcesFixture.espressoSource1ConfigStr, configBuilder, SourcesFixture.expEspressoSource1ConfigObj);
  }

  @Test(description = "Tests Venice config with Avro key")
  public void veniceConfigTest1() {
    testConfigBuilder(SourcesFixture.veniceSource1ConfigStr, configBuilder, SourcesFixture.expVeniceSource1ConfigObj);
  }

  @Test(description = "Tests Venice config with integer key")
  public void veniceConfigTest2() {
    testConfigBuilder(SourcesFixture.veniceSource2ConfigStr, configBuilder, SourcesFixture.expVeniceSource2ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and path spec")
  public void restliConfigTest1() {
    testConfigBuilder(SourcesFixture.restliSource1ConfigStr, configBuilder, SourcesFixture.expRestliSource1ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and REST request params containing 'json' object")
  public void restliConfigTest2() {
    testConfigBuilder(SourcesFixture.restliSource2ConfigStr, configBuilder, SourcesFixture.expRestliSource2ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and REST request params containing 'jsonArray' array")
  public void restliConfigTest3() {
    testConfigBuilder(SourcesFixture.restliSource3ConfigStr, configBuilder, SourcesFixture.expRestliSource3ConfigObj);
  }

  @Test(description = "Tests RestLi config with key expression, REST request params containing 'mvel' expression")
  public void restliConfigTest4() {
    testConfigBuilder(SourcesFixture.restliSource4ConfigStr, configBuilder, SourcesFixture.expRestliSource4ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and "
      + "REST request params containg 'json' whose value is a string enclosing an object")
  public void restliConfigTest5() {
    testConfigBuilder(SourcesFixture.restliSource5ConfigStr, configBuilder, SourcesFixture.expRestliSource5ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and REST request params containg 'json' object"
      + "but the 'json' object is empty.")
  public void restliConfigTest6() {
    testConfigBuilder(SourcesFixture.restliSource6ConfigStr, configBuilder, SourcesFixture.expRestliSource6ConfigObj);
  }

  @Test(description = "Tests RestLi config with entity type and REST request params containing 'jsonArray' array,"
      + " but the 'json' array is empty")
  public void restliConfigTest7() {
    testConfigBuilder(SourcesFixture.restliSource7ConfigStr, configBuilder, SourcesFixture.expRestliSource7ConfigObj);
  }

  @Test(description = "Tests RestLi config with finder field")
  public void restliConfigTest8() {
    testConfigBuilder(SourcesFixture.restliSource8ConfigStr, configBuilder, SourcesFixture.expRestliSource8ConfigObj);
  }

  @Test(description = "Tests RestLi config with both keyExpr and finder field")
  public void restliConfigTest9() {
    testConfigBuilder(SourcesFixture.restliSource9ConfigStr, configBuilder, SourcesFixture.expRestliSource9ConfigObj);
  }

  @Test(description = "Tests RestLi config missing both keyExpr and finder fields results in an error", expectedExceptions = ConfigBuilderException.class)
  public void restliConfigTest10() {
    testConfigBuilder(SourcesFixture.restliSource10ConfigStr, configBuilder, null);
  }

  @Test(description = "Tests Kafka config")
  public void kafkaConfigTest1() {
    testConfigBuilder(SourcesFixture.kafkaSource1ConfigStr, configBuilder, SourcesFixture.expKafkaSource1ConfigObj);
  }

  @Test(description = "Tests Kafka config with sliding window aggregation")
  public void kafkaConfigTest2() {
    testConfigBuilder(SourcesFixture.kafkaSource2ConfigStr, configBuilder, SourcesFixture.expKafkaSource2ConfigObj);
  }

  @Test(description = "Tests RocksDB config with keyExpr field")
  public void rocksDbConfigTest1() {
    testConfigBuilder(SourcesFixture.rocksDbSource1ConfigStr, configBuilder, SourcesFixture.expRocksDbSource1ConfigObj);
  }

  @Test(description = "Tests RocksDB config without keyExpr field")
  public void rocksDbConfigTest2() {
    testConfigBuilder(SourcesFixture.rocksDbSource2ConfigStr, configBuilder, SourcesFixture.expRocksDbSource2ConfigObj);
  }

  @Test(description = "Tests PassThrough config")
  public void passThroughConfigTest1() {
    testConfigBuilder(
        SourcesFixture.passThroughSource1ConfigStr, configBuilder, SourcesFixture.expPassThroughSource1ConfigObj);
  }

  @Test(description = "Tests Couchbase config")
  public void couchbaseConfigTest1() {
    testConfigBuilder(
        SourcesFixture.couchbaseSource1ConfigStr, configBuilder, SourcesFixture.expCouchbaseSource1ConfigObj);
  }

  @Test(description = "Tests Couchbase config name with special characters")
  public void couchbaseConfigTest1WithSpecialCharacters() {
    testConfigBuilder(
        SourcesFixture.couchbaseSource1ConfigStrWithSpecialChars, configBuilder, SourcesFixture.expCouchbaseSourceWithSpecialCharsConfigObj);
  }

  @Test(description = "Tests Pinot config")
  public void pinotConfigTest() {
    testConfigBuilder(SourcesFixture.pinotSource1ConfigStr, configBuilder, SourcesFixture.expPinotSource1ConfigObj);
  }
}

