package com.linkedin.feathr.core.configbuilder.typesafe.producer.sources;

import com.google.common.collect.ImmutableMap;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.feathr.core.config.producer.sources.CouchbaseConfig;
import com.linkedin.feathr.core.config.producer.sources.EspressoConfig;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithRegularData;
import com.linkedin.feathr.core.config.producer.sources.HdfsConfigWithSlidingWindow;
import com.linkedin.feathr.core.config.producer.sources.KafkaConfig;
import com.linkedin.feathr.core.config.producer.sources.PassThroughConfig;
import com.linkedin.feathr.core.config.producer.sources.PinotConfig;
import com.linkedin.feathr.core.config.producer.sources.RestliConfig;
import com.linkedin.feathr.core.config.producer.sources.RocksDbConfig;
import com.linkedin.feathr.core.config.producer.sources.SlidingWindowAggrConfig;
import com.linkedin.feathr.core.config.producer.sources.SourceConfig;
import com.linkedin.feathr.core.config.producer.sources.SourcesConfig;
import com.linkedin.feathr.core.config.producer.sources.TimeWindowParams;
import com.linkedin.feathr.core.config.producer.sources.VeniceConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class SourcesFixture {
  /*
   * HDFS sources
   */
  // Source with just HDFS location path
  static final String hdfsSource1ConfigStr = String.join("\n",
      "member_derived_data: {",
      "  location: {path: \"/data/test/#LATEST\"}",
      "}");

  public static final HdfsConfigWithRegularData expHdfsSource1ConfigObj;
  static {
    String path = "/data/test/#LATEST";
    expHdfsSource1ConfigObj = new HdfsConfigWithRegularData("member_derived_data", path, false);
  }

  // Source with type HDFS and location
  static final String hdfsSource2ConfigStr = String.join("\n",
      "member_derived_data2: {",
      "  type: \"HDFS\"",
      "  location: {path: \"/data/test/#LATEST\"}",
      "}");

  static final HdfsConfigWithRegularData expHdfsSource2ConfigObj;
  static {
    String path = "/data/test/#LATEST";
    expHdfsSource2ConfigObj = new HdfsConfigWithRegularData("member_derived_data2", path, false);
  }

  // hdfsSource1ConfigStr and hdfsSource2ConfigStr have been removed
  static final String hdfsSource3ConfigStr = String.join("\n",
      "member_derived_data_dali: {",
      "  location: {path: ",
      "\"dalids:///standardizationwaterloomembersstddata_mp.standardization_waterloo_members_std_data\"}",
      "}");

  static final HdfsConfigWithRegularData expHdfsSource3ConfigObj;
  static {
    String path = "dalids:///standardizationwaterloomembersstddata_mp.standardization_waterloo_members_std_data";
    expHdfsSource3ConfigObj = new HdfsConfigWithRegularData("member_derived_data_dali", path, false);
  }

  static final String hdfsSource4ConfigStr = String.join("\n",
      "swaSource: {",
      "  type: \"HDFS\"",
      "  location: { path: \"dalids://sample_database.fact_data_table\" }",
      "  timeWindowParameters: {",
      "    timestampColumn: \"timestamp\"",
      "    timestampColumnFormat: \"yyyy/MM/dd/HH/mm/ss\"",
      "  }",
      "}");

  static final HdfsConfigWithSlidingWindow expHdfsSource4ConfigObj;
  static {
    String path = "dalids://sample_database.fact_data_table";
    TimeWindowParams timeWindowParams =
        new TimeWindowParams("timestamp", "yyyy/MM/dd/HH/mm/ss");
    SlidingWindowAggrConfig swaConfig = new SlidingWindowAggrConfig(false, timeWindowParams);
    expHdfsSource4ConfigObj = new HdfsConfigWithSlidingWindow("swaSource", path, swaConfig);
  }

  static final String hdfsSource5ConfigStrWithTimePartitionPattern = String.join("\n",
      "source: {",
      "  type: \"HDFS\"",
      "  location: { path: \"dalids://sample_database.fact_data_table\" }",
      "  timePartitionPattern: \"yyyy-MM-dd\"",
      "}");


  static final HdfsConfigWithRegularData expHdfsSource5ConfigObjWithTimePartitionPattern;
  static {
    String path = "dalids://sample_database.fact_data_table";
    expHdfsSource5ConfigObjWithTimePartitionPattern = new HdfsConfigWithRegularData("source", path, "yyyy-MM-dd",false);
  }

  static final String hdfsSource6ConfigStrWithLegacyTimeWindowParameters = String.join("\n",
      "swaSource: {",
      "  type: \"HDFS\"",
      "  location: { path: \"dalids://sample_database.fact_data_table\" }",
      "  isTimeSeries: true",
      "  timeWindowParameters: {",
      "    timestamp: \"timestamp\"",
      "    timestamp_format: \"yyyy/MM/dd/HH/mm/ss\"",
      "  }",
      "}");

  static final HdfsConfigWithSlidingWindow expHdfsSource6ConfigObjWithLegacyTimeWindowParameters;
  static {
    String path = "dalids://sample_database.fact_data_table";
    TimeWindowParams timeWindowParams =
        new TimeWindowParams("timestamp", "yyyy/MM/dd/HH/mm/ss");
    SlidingWindowAggrConfig swaConfig = new SlidingWindowAggrConfig(true, timeWindowParams);
    expHdfsSource6ConfigObjWithLegacyTimeWindowParameters = new HdfsConfigWithSlidingWindow("swaSource", path, swaConfig);
  }

  static final String invalidHdfsSourceconfigStrWithTimePartitionPatternAndIsTimeSeries = String.join("\n",
      "swaSource: {",
      "  type: \"HDFS\"",
      "  location: { path: \"dalids://sample_database.fact_data_table\" }",
      "  timePartitionPattern: \"yyyy-MM-dd\"",
      "  isTimeSeries: true",
      "  timeWindowParameters: {",
      "    timestamp: \"timestamp\"",
      "    timestamp_format: \"yyyy/MM/dd/HH/mm/ss\"",
      "  }",
      "}");

  static final String invalidHdfsSourceconfigStrWithHasTimeSnapshotAndIsTimeSeries = String.join("\n",
      "swaSource: {",
      "  type: \"HDFS\"",
      "  location: { path: \"dalids://sample_database.fact_data_table\" }",
      "  hasTimeSnapshot: true",
      "  isTimeSeries: true",
      "  timeWindowParameters: {",
      "    timestamp: \"timestamp\"",
      "    timestamp_format: \"yyyy/MM/dd/HH/mm/ss\"",
      "  }",
      "}");

  /*
   * Espresso
   */
  static final String espressoSource1ConfigStr = String.join("\n",
      "MemberPreferenceData: {",
      "  type: ESPRESSO",
      "  database: \"CareersPreferenceDB\"",
      "  table: \"MemberPreference\"",
      "  d2Uri: \"d2://ESPRESSO_MT2\"",
      "  keyExpr: \"key[0]\"",
      "}");

  public static final EspressoConfig expEspressoSource1ConfigObj = new EspressoConfig("MemberPreferenceData", "CareersPreferenceDB",
      "MemberPreference", "d2://ESPRESSO_MT2", "key[0]");

  /*
   * Venice sources
   */
  static final String veniceSource1ConfigStr = String.join("\n",
      "veniceTestSourceWithAvroKey {",
      "  type: VENICE",
      "  keyExpr : \"{\\\"x\\\" : (Integer)key[0], \\\"version\\\" : \\\"v2\\\"}\"",
      "  storeName: \"vtstore\"",
      "}");

  static final VeniceConfig expVeniceSource1ConfigObj;
  static {
    String storeName = "vtstore";
    String keyExpr = "{\"x\" : (Integer)key[0], \"version\" : \"v2\"}";
    expVeniceSource1ConfigObj = new VeniceConfig("veniceTestSourceWithAvroKey", storeName, keyExpr);
  }

  static final String veniceSource2ConfigStr = String.join("\n",
      "veniceTestSourceWithIntegerKey {",
      "  type: VENICE",
      "  keyExpr : \"(Integer)key[0]\"",
      "  storeName: \"vtstore2\"",
      "}");

  static final VeniceConfig expVeniceSource2ConfigObj;
  static {
    String storeName = "vtstore2";
    String keyExpr = "(Integer)key[0]";
    expVeniceSource2ConfigObj = new VeniceConfig("veniceTestSourceWithIntegerKey", storeName, keyExpr);
  }

  /*
   * Rest.Li sources
   */
  static final String restliSource1ConfigStr = String.join("\n",
      "JobsTargetingSegments: {",
      "  type: RESTLI",
      "  restResourceName: \"jobsTargetingSegments\"",
      "  restEntityType: \"jobPosting\"",
      "  pathSpec: \"targetingFacetsSet\"",
      "}");

  static final RestliConfig expRestliSource1ConfigObj;
  static {
    String resourceName = "jobsTargetingSegments";
    String keyExpr = "toUrn(\"jobPosting\", key[0])";
    PathSpec pathSpec = new PathSpec("targetingFacetsSet");
    expRestliSource1ConfigObj = new RestliConfig("JobsTargetingSegments", resourceName, keyExpr, null, pathSpec);
  }

  static final String restliSource2ConfigStr = String.join("\n",
      "MemberConnectionIntersection: {",
      "  type: RESTLI",
      "  restResourceName: setOperations",
      "  restEntityType: member",
      "  restReqParams: {",
      "    operator : INTERSECT",
      "    edgeSetSpecifications : {",
      "       json: {",
      "         firstEdgeType: MemberToMember",
      "         secondEdgeType: MemberToMember",
      "       }",
      "    }",
      "    second: {",
      "      mvel: \"key[1]\"",                  // key[0] is by default used as the request key
      "    }",
      "  }",
      "}");

  static final RestliConfig expRestliSource2ConfigObj;
  static {
    String resourceName = "setOperations";

    String keyExpr = "toUrn(\"member\", key[0])";

    Map<String, String> map = new HashMap<>();
    map.put("firstEdgeType", "MemberToMember");
    map.put("secondEdgeType", "MemberToMember");
    DataMap dataMap = new DataMap(map);

    String mvelExpr = "key[1]"; //MVEL.compileExpression("key[1]");

    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("operator", "INTERSECT");
    paramsMap.put("edgeSetSpecifications", dataMap);
    paramsMap.put("second", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    expRestliSource2ConfigObj = new RestliConfig("MemberConnectionIntersection", resourceName, keyExpr, paramsMap, null);
  }

  static final String restliSource3ConfigStr = String.join("\n",
      "MemberConnectionIntersection2: {",
      "  type: RESTLI",
      "  restResourceName: setOperations",
      "  restEntityType: member",
      "  restReqParams: {",
      "    operator : INTERSECT",
      "    edgeSetSpecifications : {",
      "      jsonArray: {",
      "        array: [",
      "          {firstEdgeType: MemberToMember, secondEdgeType : MemberToMember}",
      "        ]",
      "      }",
      "    }",
      "    second: {",
      "      mvel: \"key[1]\"",
      "    }",
      "  }",
      "}");

  static final RestliConfig expRestliSource3ConfigObj;
  static {
    String resourceName = "setOperations";

    String keyExpr = "toUrn(\"member\", key[0])";

    Map<String, String> map = new HashMap<>();
    map.put("firstEdgeType", "MemberToMember");
    map.put("secondEdgeType", "MemberToMember");
    DataMap dataMap = new DataMap(map);
    List<DataMap> list = new ArrayList<>();
    list.add(dataMap);
    DataList dataList = new DataList(list);

    String mvelExpr = "key[1]"; //MVEL.compileExpression("key[1]");

    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("operator", "INTERSECT");
    paramsMap.put("edgeSetSpecifications", dataList);
    paramsMap.put("second", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    expRestliSource3ConfigObj = new RestliConfig("MemberConnectionIntersection2", resourceName, keyExpr, paramsMap, null);
  }


  static final String restliSource4ConfigStr = String.join("\n",
      "Profile: {",
      "  type: RESTLI",
      "  restResouceName: \"profiles\"",
      "  keyExpr: \"toComplexResourceKey({\\\"id\\\": key[0]},{:})\"",
      "  restReqParams: {",
      "    viewerId: {mvel: \"key[0]\"}",
      "  }",
      "  pathSpec: \"positions\"",
      "}");

  static final RestliConfig expRestliSource4ConfigObj;
  static {
    String resourceName = "profiles";

    String keyExpr = "toComplexResourceKey({\"id\": key[0]},{:})";

    String mvelExpr = "key[0]"; //MVEL.compileExpression("key[0]")
    Map<String, Object> map = new HashMap<>();
    map.put("viewerId", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    PathSpec pathSpec = new PathSpec("positions");

    expRestliSource4ConfigObj = new RestliConfig("Profile", resourceName, keyExpr, map, pathSpec);
  }

  static final String restliSource5ConfigStr = String.join("\n",
      "MemberConnectionIntersection: {",
      "  type: RESTLI",
      "  restResourceName: setOperations",
      "  restEntityType: member",
      "  restReqParams: {",
      "    operator : INTERSECT",
      "    edgeSetSpecifications : {",
      "       json: \"{firstEdgeType: MemberToMember, secondEdgeType: MemberToMember}\"",
      "    }",
      "    second: {",
      "      mvel: \"key[1]\"",              // key[0] is by default used as the request key
      "    }",
      "  }",
      "}");

  static final RestliConfig expRestliSource5ConfigObj = expRestliSource2ConfigObj;

  static final String restliSource6ConfigStr = String.join("\n",
      "MemberConnectionIntersection: {",
      "  type: RESTLI",
      "  restResourceName: setOperations",
      "  restEntityType: member",
      "  restReqParams: {",
      "    operator : INTERSECT",
      "    edgeSetSpecifications : {",
      "       json: {",
      "       }",
      "    }",
      "    second: {",
      "      mvel: \"key[1]\"",                  // key[0] is by default used as the request key
      "    }",
      "  }",
      "}");

  static final RestliConfig expRestliSource6ConfigObj;
  static {
    String resourceName = "setOperations";

    String keyExpr = "toUrn(\"member\", key[0])";

    Map<String, String> map = new HashMap<>();
    DataMap dataMap = new DataMap(map);

    String mvelExpr = "key[1]"; //MVEL.compileExpression("key[1]");

    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("operator", "INTERSECT");
    paramsMap.put("edgeSetSpecifications", dataMap);
    paramsMap.put("second", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    expRestliSource6ConfigObj = new RestliConfig("MemberConnectionIntersection", resourceName, keyExpr, paramsMap, null);
  }

  static final String restliSource7ConfigStr = String.join("\n",
      "MemberConnectionIntersection2: {",
      "  type: RESTLI",
      "  restResourceName: setOperations",
      "  restEntityType: member",
      "  restReqParams: {",
      "    operator : INTERSECT",
      "    edgeSetSpecifications : {",
      "      jsonArray: {",
      "        array: [",
      "        ]",
      "      }",
      "    }",
      "    second: {",
      "      mvel: \"key[1]\"",
      "    }",
      "  }",
      "}");

  static final RestliConfig expRestliSource7ConfigObj;
  static {
    String resourceName = "setOperations";

    String keyExpr = "toUrn(\"member\", key[0])";

    List<DataMap> list = new ArrayList<>();
    DataList dataList = new DataList(list);

    String mvelExpr = "key[1]"; //MVEL.compileExpression("key[1]");

    Map<String, Object> paramsMap = new HashMap<>();
    paramsMap.put("operator", "INTERSECT");
    paramsMap.put("edgeSetSpecifications", dataList);
    paramsMap.put("second", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    expRestliSource7ConfigObj = new RestliConfig("MemberConnectionIntersection2", resourceName, keyExpr, paramsMap, null);
  }

  static final String restliSource8ConfigStr = String.join("\n",
      "Profile: {",
      "  type: RESTLI",
      "  restResouceName: \"profiles\"",
      "  finder: \"rule\"",
      "  restReqParams: {",
      "    ruleName: \"search/CurrentCompaniesOfConnections\"",
      "    ruleArguments: {mvel: \"[\\\"names\\\" : [\\\"member\\\", \\\"company\\\"], \\\"arguments\\\" : [[[\\\"value\\\" : key[0]], [:]]]]\"}",
      "  }",
      "  pathSpec: \"positions\"",
      "}");

  static final RestliConfig expRestliSource8ConfigObj;
  static {
    String resourceName = "profiles";
    String finder = "rule";
    String mvelExpr = "[\"names\" : [\"member\", \"company\"], \"arguments\" : [[[\"value\" : key[0]], [:]]]]";
    Map<String, Object> map = new HashMap<>();
    map.put("ruleName", "search/CurrentCompaniesOfConnections");
    map.put("ruleArguments", new DataMap(ImmutableMap.of(RestliConfig.MVEL_KEY, mvelExpr)));

    PathSpec pathSpec = new PathSpec("positions");

    expRestliSource8ConfigObj = new RestliConfig("Profile", resourceName, map, pathSpec, finder);
  }

  // Case where both keyExpr and finder are present.
  static final String restliSource9ConfigStr = String.join("\n",
      "Profile: {",
      "  type: RESTLI",
      "  restResourceName: \"profiles\"",
      "  finder: \"rule\"",
      "  keyExpr: \"toCompoundKey(\\\"member\\\", 123)\"",
      "}");

  static final RestliConfig expRestliSource9ConfigObj;
  static {
    String resourceName = "profiles";
    String finder = "rule";
    String mvelExpr = "toCompoundKey(\"member\", 123)";
    expRestliSource9ConfigObj = new RestliConfig("Profile", resourceName, mvelExpr, null, null, finder);
  }

  // Case where both keyExpr and finder are missing.
  static final String restliSource10ConfigStr = String.join("\n",
      "Profile: {",
      "  type: RESTLI",
      "  restResourceName: \"profiles\"",
      "}");

  /*
   * Kafka sources
   */
  static final String kafkaSource1ConfigStr = String.join("\n",
      "kafkaTestSource1: {",
      "  type: KAFKA",
      "  stream: \"kafka.testCluster.testTopic\"",
      "}");

  static final KafkaConfig expKafkaSource1ConfigObj =
      new KafkaConfig("kafkaTestSource1", "kafka.testCluster.testTopic", null);

  static final String kafkaSource2ConfigStr = String.join("\n",
      "kafkaTestSource2: {",
      "  type: KAFKA",
      "  stream: \"kafka.testCluster.testTopic\"",
      "  isTimeSeries: true",
      "  timeWindowParameters: {",
      "    timestamp: \"timestamp\"",
      "    timestamp_format: \"yyyy/MM/dd/HH/mm/ss\"",
      "  }",
      "}");

  static final KafkaConfig expKafkaSource2ConfigObj;
  static {
    String stream = "kafka.testCluster.testTopic";
    TimeWindowParams timeWindowParams =
    new TimeWindowParams("timestamp", "yyyy/MM/dd/HH/mm/ss");
    SlidingWindowAggrConfig swaConfig = new SlidingWindowAggrConfig(true, timeWindowParams);
    expKafkaSource2ConfigObj = new KafkaConfig("kafkaTestSource2", stream, swaConfig);
  }

  /*
   * RocksDB sources
   */
  static final String rocksDbSource1ConfigStr = String.join("\n",
      "rocksDBTestSource1: {",
      "  type: ROCKSDB",
      "  referenceSource: \"kafkaTestSource\"",
      "  extractFeatures: true",
      "  encoder: \"com.linkedin.frame.online.config.FoobarExtractor\"",
      "  decoder: \"com.linkedin.frame.online.config.FoobarExtractor\"",
      "  keyExpr: \"keyExprName\"",
      "}");

  static final RocksDbConfig expRocksDbSource1ConfigObj;
  static {
    String referenceSource = "kafkaTestSource";
    String encoder = "com.linkedin.frame.online.config.FoobarExtractor";
    String decoder = "com.linkedin.frame.online.config.FoobarExtractor";
    String keyExpr = "keyExprName";
    expRocksDbSource1ConfigObj = new RocksDbConfig("rocksDBTestSource1", referenceSource, true, encoder, decoder, keyExpr);
  }

  static final String rocksDbSource2ConfigStr = String.join("\n",
      "rocksDBTestSource2: {",
      "  type: ROCKSDB",
      "  referenceSource: \"kafkaTestSource\"",
      "  extractFeatures: true",
      "  encoder: \"com.linkedin.frame.online.config.FoobarExtractor\"",
      "  decoder: \"com.linkedin.frame.online.config.FoobarExtractor\"",
      "}");

  static final RocksDbConfig expRocksDbSource2ConfigObj;
  static {
    String referenceSource = "kafkaTestSource";
    String encoder = "com.linkedin.frame.online.config.FoobarExtractor";
    String decoder = "com.linkedin.frame.online.config.FoobarExtractor";
    expRocksDbSource2ConfigObj = new RocksDbConfig("rocksDBTestSource2", referenceSource, true, encoder, decoder, null);
  }
  /*
   * PassThrough sources
   */
  static final String passThroughSource1ConfigStr = String.join("\n",
      "passThroughTestSource: {",
      "  type: PASSTHROUGH",
      "  dataModel: \"com.linkedin.some.service.SomeEntity\"",
      "}");

  static final PassThroughConfig expPassThroughSource1ConfigObj =
      new PassThroughConfig("passThroughTestSource", "com.linkedin.some.service.SomeEntity");

  /*
   * Couchbase sources
   */
  static final String couchbaseSource1ConfigStr = String.join("\n",
      "couchbaseTestSource {",
      "  type: COUCHBASE",
      "  keyExpr : \"key[0]\"",
      "  bucketName: \"testBucket\"",
      "  bootstrapUris: [\"some-app.linkedin.com:8091\", \"other-app.linkedin.com:8091\"]",
      "  documentModel: \"com.linkedin.some.Document\"",
      "}");

  static final CouchbaseConfig expCouchbaseSource1ConfigObj;
  static {
    String bucketName = "testBucket";
    String keyExpr = "key[0]";
    String[] bootstrapUris = new String[] {"some-app.linkedin.com:8091", "other-app.linkedin.com:8091"};
    String documentModel = "com.linkedin.some.Document";
    expCouchbaseSource1ConfigObj = new CouchbaseConfig("couchbaseTestSource", bucketName, keyExpr, documentModel);
  }

  /*
   * Couchbase sources with special characters
   */
  static final String couchbaseSource1ConfigStrWithSpecialChars = String.join("\n",
      "\"couchbase:Test.Source\" {",
      "  type: COUCHBASE",
      "  keyExpr : \"key[0]\"",
      "  bucketName: \"testBucket\"",
      "  bootstrapUris: [\"some-app.linkedin.com:8091\", \"other-app.linkedin.com:8091\"]",
      "  documentModel: \"com.linkedin.some.Document\"",
      "}");
  static final CouchbaseConfig expCouchbaseSourceWithSpecialCharsConfigObj;
  static {
    String bucketName = "testBucket";
    String keyExpr = "key[0]";
    String[] bootstrapUris = new String[] {"some-app.linkedin.com:8091", "other-app.linkedin.com:8091"};
    String documentModel = "com.linkedin.some.Document";
    expCouchbaseSourceWithSpecialCharsConfigObj = new CouchbaseConfig("couchbase:Test.Source", bucketName, keyExpr, documentModel);
  }

  static final CouchbaseConfig expCouchbaseSource1ConfigObjWithSpecialChars;
  static {
    String bucketName = "testBucket";
    String keyExpr = "key[0]";
    String[] bootstrapUris = new String[]{"some-app.linkedin.com:8091", "other-app.linkedin.com:8091"};
    String documentModel = "com.linkedin.some.Document";
    expCouchbaseSource1ConfigObjWithSpecialChars = new CouchbaseConfig("couchbase:Test.Source", bucketName, keyExpr, documentModel);
  }

  /*
   * Pinot sources
   */
  static final String pinotSource1ConfigStr =
      String.join("\n", "pinotTestSource {",
          "  type: PINOT",
          "  resourceName : \"recentMemberActionsPinotQuery\"",
          "  queryTemplate : \"SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?)\"",
          "  queryArguments : [\"key[0]\"]",
          "  queryKeyColumns: [\"actorId\"]",
          "}");

  static final PinotConfig expPinotSource1ConfigObj;

  static {
    String resourceName = "recentMemberActionsPinotQuery";
    String queryTemplate = "SELECT verb, object, verbAttributes, timeStampSec FROM RecentMemberActions WHERE actorId IN (?)";
    String[] queryArguments = new String[]{"key[0]"};
    String[] queryKeyColumns = new String[]{"actorId"};

    expPinotSource1ConfigObj = new PinotConfig("pinotTestSource", resourceName, queryTemplate, queryArguments, queryKeyColumns);
  }

  static final String offlineSourcesConfigStr = String.join("\n",
      "sources: {",
      hdfsSource1ConfigStr,
      hdfsSource2ConfigStr,
      hdfsSource3ConfigStr,
      hdfsSource4ConfigStr,
      "}");

  static final SourcesConfig expOfflineSourcesConfigObj;
  static {
    Map<String, SourceConfig> sources = new HashMap<>();
    sources.put("member_derived_data", expHdfsSource1ConfigObj);
    sources.put("member_derived_data2", expHdfsSource2ConfigObj);
    sources.put("member_derived_data_dali", expHdfsSource3ConfigObj);
    sources.put("swaSource", expHdfsSource4ConfigObj);
    expOfflineSourcesConfigObj = new SourcesConfig(sources);
  }


  static final String onlineSourcesConfigStr = String.join("\n",
      "sources: {",
      espressoSource1ConfigStr,
      veniceSource1ConfigStr,
      veniceSource2ConfigStr,
      kafkaSource1ConfigStr,
      kafkaSource2ConfigStr,
      rocksDbSource1ConfigStr,
      rocksDbSource2ConfigStr,
      passThroughSource1ConfigStr,
      couchbaseSource1ConfigStr,
      pinotSource1ConfigStr,
      "}");

  static final SourcesConfig expOnlineSourcesConfigObj;
  static {
    Map<String, SourceConfig> sources = new HashMap<>();
    sources.put("MemberPreferenceData", expEspressoSource1ConfigObj);
    sources.put("veniceTestSourceWithAvroKey", expVeniceSource1ConfigObj);
    sources.put("veniceTestSourceWithIntegerKey", expVeniceSource2ConfigObj);
    sources.put("kafkaTestSource1", expKafkaSource1ConfigObj);
    sources.put("kafkaTestSource2", expKafkaSource2ConfigObj);
    sources.put("rocksDBTestSource1", expRocksDbSource1ConfigObj);
    sources.put("rocksDBTestSource2", expRocksDbSource2ConfigObj);
    sources.put("passThroughTestSource", expPassThroughSource1ConfigObj);
    sources.put("couchbaseTestSource", expCouchbaseSource1ConfigObj);
    sources.put("pinotTestSource", expPinotSource1ConfigObj);
    expOnlineSourcesConfigObj = new SourcesConfig(sources);
  }
}