/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.mongodb.sink;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.MongoTypeInfoConverter;
import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnConfig;
import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnOptions;
import com.bytedance.bitsail.connector.legacy.mongodb.constant.MongoDBConstants;
import com.bytedance.bitsail.connector.legacy.mongodb.error.MongoDBPluginsErrorCode;
import com.bytedance.bitsail.connector.legacy.mongodb.option.MongoDBWriterOptions;
import com.bytedance.bitsail.connector.legacy.mongodb.util.MongoDBWriterUtil;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.types.Row;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MongoDBOutputFormat extends OutputFormatPlugin<Row> implements ResultTypeQueryable<Row> {
  private static final Logger LOG = LoggerFactory.getLogger(MongoDBOutputFormat.class);
  private static final long serialVersionUID = 1L;

  private RowTypeInfo rowTypeInfo;
  private List<ColumnInfo> columnInfos;
  private List<String> columnNames;

  private int batchSize;
  private int batchCount;
  private String partitionKey;
  private String partitionValue;
  private String partitionPatternFormat;
  private MongoValueConverter mongoValueConvert;

  private MongoConnConfig mongoConnConfig;
  private transient MongoClient mongoClient;
  private transient MongoDatabase mongoDatabase;
  private transient MongoCollection<Document> mongoCollection;
  private List<Document> docsBuffer;
  private List<String> uniqueKeyList;

  @Override
  public void initPlugin() throws Exception {
    initMongoConnConfig();
    this.batchSize = outputSliceConfig.get(MongoDBWriterOptions.BATCH_SIZE);

    String dateFormat = outputSliceConfig.get(MongoDBWriterOptions.DATE_FORMAT_PATTERN);
    String timeZone = outputSliceConfig.get(MongoDBWriterOptions.DATE_TIME_ZONE);
    Map<String, Object> convertOptions = new HashMap<>();
    if (!Strings.isNullOrEmpty(dateFormat)) {
      SimpleDateFormat format = new SimpleDateFormat(dateFormat);
      convertOptions.put(MongoValueConverter.CUSTOM_DATE_FORMAT_KEY, format);
      convertOptions.put(MongoValueConverter.TIME_ZONE, timeZone);
      LOG.info("MongoValueConvert Options: " + JSON.toJSONString(convertOptions));
    }
    this.mongoValueConvert = new MongoValueConverter(convertOptions);

    this.columnInfos = outputSliceConfig.getNecessaryOption(
        MongoDBWriterOptions.COLUMNS, CommonErrorCode.CONFIG_ERROR);
    this.columnNames = columnInfos.stream().map(ColumnInfo::getName).collect(Collectors.toList());
    installMongoClient();
    handlePreSql();
    if (this.mongoConnConfig.getWriteMode() == MongoConnConfig.WRITE_MODE.INSERT) {
      this.partitionKey = this.outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.PARTITION_KEY, MongoDBPluginsErrorCode.REQUIRED_VALUE);
      this.partitionValue = this.outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.PARTITION_VALUE, MongoDBPluginsErrorCode.REQUIRED_VALUE);
      this.partitionPatternFormat = this.outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.PARTITION_PATTERN_FORMAT, MongoDBPluginsErrorCode.REQUIRED_VALUE);

      handleInsertModePartition();
    } else {
      String uniqueKeysStr = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.UNIQUE_KEY, MongoDBPluginsErrorCode.REQUIRED_VALUE);
      this.uniqueKeyList = Arrays.asList(StringUtils.split(uniqueKeysStr, ","));
    }
    this.rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columnInfos, new MongoTypeInfoConverter());
    LOG.info("Output Row Type Info: " + rowTypeInfo);
  }

  @Override
  public void open(int taskNumber, int numTasks) throws IOException {
    super.open(taskNumber, numTasks);
    installMongoClient();
    this.batchCount = 0;
    docsBuffer = new ArrayList<Document>(batchSize);
  }

  @Override
  public String getType() {
    return "mongodb";
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  @Override
  public int getMaxParallelism() {
    return MongoDBConstants.MAX_PARALLELISM_OUTPUT_MONGODB;
  }

  @Override
  public void writeRecordInternal(Row record) throws Exception {
    Document doc = new Document();
    int index = 0;
    for (index = 0; index < record.getArity(); index++) {
      Object value = mongoValueConvert.convert(index, record.getField(index), this.columnInfos);
      doc.append(this.columnNames.get(index), value);
    }
    if (this.mongoConnConfig.getWriteMode() == MongoConnConfig.WRITE_MODE.INSERT) {
      addPartitionValue(doc, index);
    }
    docsBuffer.add(doc);
    this.batchCount++;
    if (this.batchCount == this.batchSize && !this.docsBuffer.isEmpty()) {
      flushDocBuffer();
    }
  }

  public void flushDocBuffer() {
    if (this.mongoConnConfig.getWriteMode() == MongoConnConfig.WRITE_MODE.INSERT) {
      this.mongoCollection.insertMany(this.docsBuffer);
    } else {
      List<ReplaceOneModel<Document>> replaceModelList = new ArrayList<>();
      for (Document doc : this.docsBuffer) {
        BasicDBObject query = new BasicDBObject();
        for (String uniqueKey : uniqueKeyList) {
          query.put(uniqueKey, doc.get(uniqueKey));
        }
        ReplaceOneModel<Document> replaceOneModel = new ReplaceOneModel<>(query, doc, new UpdateOptions().upsert(true));
        replaceModelList.add(replaceOneModel);
      }
      this.mongoCollection.bulkWrite(replaceModelList, new BulkWriteOptions().ordered(false));
    }
    this.batchCount = 0;
    this.docsBuffer.clear();
  }

  private void handleInsertModePartition() {
    clearPartitionRecords();
    addPartitionColumn();
  }

  private void clearPartitionRecords() {
    BasicDBObject query = new BasicDBObject();
    if (this.partitionPatternFormat.equals("yyyyMMdd")) {
      query.put(this.partitionKey, Long.valueOf(this.partitionValue));
    } else {
      query.put(this.partitionKey, this.partitionValue);
    }
    this.mongoCollection.deleteMany(query);
  }

  private void addPartitionColumn() {
    this.columnNames.add(this.partitionKey);
    if (this.partitionPatternFormat.equals("yyyyMMdd")) {
      this.columnInfos.add(new ColumnInfo(this.partitionKey, "long"));
    } else {
      this.columnInfos.add(new ColumnInfo(this.partitionKey, "string"));
    }
  }

  private void addPartitionValue(Document doc, int index) {
    if (this.partitionPatternFormat.equals("yyyyMMdd")) {
      doc.append(this.columnNames.get(index), Long.valueOf(this.partitionValue));
    } else {
      doc.append(this.columnNames.get(index), this.partitionValue);
    }
  }

  /**
   * Supported pre-sql operations:<br/>
   * 1. drop: Drop all data from target collection.<br/>
   * example: {"type":"drop"}<br/>
   * 2. remove: Drop data that matches from target collection.<br/>
   * example: {"type":"remove", "item":[{"name":"key1", "value":"test"}]}<br/>
   * Support 'drop' and 'remove' operations.
   */
  public void handlePreSql() {
    String preSql = outputSliceConfig.get(MongoDBWriterOptions.PRE_SQL);
    if (Strings.isNullOrEmpty(preSql)) {
      return;
    }

    BitSailConfiguration preSqlConf = BitSailConfiguration.from(preSql);
    String type = preSqlConf.getString("type");
    if (Strings.isNullOrEmpty(type)) {
      return;
    }

    if (type.equals("drop")) {
      this.mongoCollection.drop();
    } else if (type.equals("remove")) {
      String json = preSqlConf.getString("json");
      BasicDBObject query;
      if (Strings.isNullOrEmpty(json)) {
        query = new BasicDBObject();
        List<Object> items = preSqlConf.getList("item", Object.class);
        for (Object con : items) {
          BitSailConfiguration tempConf = BitSailConfiguration.from(con.toString());
          if (Strings.isNullOrEmpty(tempConf.getString("condition"))) {
            query.put(tempConf.getString("name"), tempConf.get("value"));
          } else {
            query.put(tempConf.getString("name"),
                new BasicDBObject(tempConf.getString("condition"), tempConf.get("value")));
          }
        }
      } else {
        query = (BasicDBObject) JSON.parse(json);
      }
      this.mongoCollection.deleteMany(query);
    }
  }

  @Override
  public void tryCleanupOnError() throws Exception {
  }

  @Override
  public void close() throws IOException {
    if (this.batchCount > 0) {
      flushDocBuffer();
    }

    if (null != this.mongoClient) {
      this.mongoClient.close();
    }

    super.close();
  }

  @Override
  public TypeInformation<Row> getProducedType() {
    return rowTypeInfo;
  }

  @SuppressWarnings("checkstyle:FallThrough")
  private void initMongoConnConfig() {
    MongoConnConfig.CLIENT_MODE clientMode = MongoConnConfig.CLIENT_MODE.valueOf(outputSliceConfig.get(MongoDBWriterOptions.CLIENT_MODE).toUpperCase());
    LOG.info("Current mongo client mode: " + clientMode);
    String hostsStr = "";
    String host = "";
    String userName = "";
    String password = "";
    int port = 0;
    String mongoUrl = "";
    switch (clientMode) {
      case URL:
        mongoUrl = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.MONGO_URL, MongoDBPluginsErrorCode.REQUIRED_VALUE);
        break;
      case HOST_WITH_CREDENTIAL:
        userName = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.USER_NAME, MongoDBPluginsErrorCode.REQUIRED_VALUE);
        password = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.PASSWORD, MongoDBPluginsErrorCode.REQUIRED_VALUE);
      case HOST_WITHOUT_CREDENTIAL:
        if (outputSliceConfig.fieldExists(MongoDBWriterOptions.MONGO_HOSTS_STR)) {
          hostsStr = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.MONGO_HOSTS_STR, MongoDBPluginsErrorCode.REQUIRED_VALUE);
        } else {
          host = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.MONGO_HOST, MongoDBPluginsErrorCode.REQUIRED_VALUE);
          port = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.MONGO_PORT, MongoDBPluginsErrorCode.REQUIRED_VALUE);
        }
        break;
      default:
    }
    MongoConnConfig.WRITE_MODE writeMode = MongoConnConfig.WRITE_MODE.valueOf(outputSliceConfig.get(MongoDBWriterOptions.WRITE_MODE).toUpperCase());
    String dbName = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.DB_NAME, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    String authDbName = outputSliceConfig.get(MongoDBWriterOptions.AUTH_DB_NAME);
    String collectionName = outputSliceConfig.getNecessaryOption(MongoDBWriterOptions.COLLECTION_NAME, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    MongoConnOptions options = getOptions();
    MongoConnConfig.MongoConnConfigBuilder builder = MongoConnConfig.builder();
    builder.clientMode(clientMode);
    builder.mongoUrl(mongoUrl);
    builder.hostsStr(hostsStr);
    builder.host(host);
    builder.port(port);
    builder.dbName(dbName);
    builder.authDbName(authDbName);
    builder.collectionName(collectionName);
    builder.userName(userName);
    builder.password(password);
    builder.options(options);
    builder.writeMode(writeMode);
    this.mongoConnConfig = builder.build();
  }

  private void installMongoClient() {
    if (null == this.mongoClient) {
      this.mongoClient = MongoDBWriterUtil.buildMongoClientWithRetry(this.mongoConnConfig);
    }

    if (null == this.mongoDatabase) {
      this.mongoDatabase = mongoClient.getDatabase(this.mongoConnConfig.getDbName());
    }

    if (null == this.mongoCollection) {
      this.mongoCollection = this.mongoDatabase.getCollection(this.mongoConnConfig.getCollectionName());
    }
  }

  private MongoConnOptions getOptions() {
    MongoConnOptions.MongoConnOptionsBuilder build = MongoConnOptions.builder();
    build.connectionsPerHost(outputSliceConfig.get(MongoDBWriterOptions.MAX_CONNECTION_PER_HOST));
    build.threadsAllowedToBlockForConnectionMultiplier(outputSliceConfig.get(MongoDBWriterOptions.THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER));
    build.connectTimeout(outputSliceConfig.get(MongoDBWriterOptions.CONNECT_TIMEOUT_MS));
    build.maxWaitTime(outputSliceConfig.get(MongoDBWriterOptions.MAX_WAIT_TIME_MS));
    build.socketTimeout(outputSliceConfig.get(MongoDBWriterOptions.SOCKET_TIMEOUT_MS));
    build.writeConcern(outputSliceConfig.get(MongoDBWriterOptions.WRITE_CONCERN));
    return build.build();
  }
}
