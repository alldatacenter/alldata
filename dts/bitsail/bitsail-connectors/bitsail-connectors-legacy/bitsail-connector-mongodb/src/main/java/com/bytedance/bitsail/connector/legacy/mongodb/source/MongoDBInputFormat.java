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

package com.bytedance.bitsail.connector.legacy.mongodb.source;

import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.type.filemapping.MongoTypeInfoConverter;
import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnConfig;
import com.bytedance.bitsail.connector.legacy.mongodb.error.MongoDBPluginsErrorCode;
import com.bytedance.bitsail.connector.legacy.mongodb.option.MongoDBReaderOptions;
import com.bytedance.bitsail.connector.legacy.mongodb.source.split.MongoSplitter;
import com.bytedance.bitsail.connector.legacy.mongodb.source.split.MongoSplitterFactory;
import com.bytedance.bitsail.connector.legacy.mongodb.source.split.Range;
import com.bytedance.bitsail.connector.legacy.mongodb.source.split.TaskGroupInfo;
import com.bytedance.bitsail.connector.legacy.mongodb.util.MongoDBUtil;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.typeutils.NativeFlinkTypeInfoUtil;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.io.DefaultInputSplitAssigner;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.typeinfo.BasicTypeInfo;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.common.typeinfo.SqlTimeTypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ListTypeInfo;
import org.apache.flink.api.java.typeutils.MapTypeInfo;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.bson.BsonTimestamp;
import org.bson.Document;
import org.bson.types.Binary;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MongoDBInputFormat extends InputFormatPlugin<Row, InputSplit> implements ResultTypeQueryable<Row> {
  private static final long serialVersionUID = 1L;
  private static final Logger LOG = LoggerFactory.getLogger(MongoDBInputFormat.class);

  public static final int DEFAULT_PARALLELISM_NUM = 5;

  @Getter
  private int parallelismNum;

  private MongoConnConfig mongoConnConfig;
  private String splitKey;
  private int fetchSize;
  private int totalSplitNum;
  private String filter;

  MongoClient mongoclient;
  MongoCollection<Document> collection;
  MongoCursor<Document> cursor;

  TaskGroupInfo taskGroupInfo;

  private RowTypeInfo rowTypeInfo;
  private int currentSplitNum;
  private Map<Integer, Integer> jobIdOffsetInfo;

  public MongoDBInputFormat() {
  }

  @Override
  public RowTypeInfo getProducedType() {
    return rowTypeInfo;
  }

  @Override
  public TypeSystem getTypeSystem() {
    return TypeSystem.FLINK;
  }

  @Override
  public void openInputFormat() throws IOException {
    super.openInputFormat();
    mongoclient = MongoDBUtil.initMongoClientWithRetry(mongoConnConfig);
    MongoDatabase database = mongoclient.getDatabase(mongoConnConfig.getDbName());
    collection = database.getCollection(mongoConnConfig.getCollectionName());
    jobIdOffsetInfo = new ConcurrentHashMap<>();
  }

  @Override
  public void closeInputFormat() throws IOException {
    super.closeInputFormat();
    if (mongoclient != null) {
      mongoclient.close();
    }
  }

  @Override
  public InputSplit[] createSplits(int minNumSplits) {
    GenericInputSplit[] ret = new GenericInputSplit[taskGroupInfo.size()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = new GenericInputSplit(i, ret.length);
    }

    setTotalSplitsNum(totalSplitNum);
    LOG.info("MongoDB Input splits size: {}", totalSplitNum);
    return ret;
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics cachedStatistics) {
    return new BaseStatistics() {
      @Override
      public long getTotalInputSize() {
        return SIZE_UNKNOWN;
      }

      @Override
      public long getNumberOfRecords() {
        int rangeNum = 0;
        for (int i = 0; i < taskGroupInfo.size(); i++) {
          rangeNum += taskGroupInfo.get(i).size();
        }
        return (long) rangeNum * fetchSize;
      }

      @Override
      public float getAverageRecordWidth() {
        return AVG_RECORD_BYTES_UNKNOWN;
      }
    };
  }

  @Override
  public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
    return new DefaultInputSplitAssigner(inputSplits);
  }

  @Override
  public void configure(Configuration parameters) {
    super.configure(parameters);
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private Object parseBasicTypeInfo(Object value, TypeInformation typeInfo) {
    switch (typeInfo.toString()) {
      case "Void":
        return null;
      case "String":
        if (value instanceof Document) {
          return JSON.toJSONString(parseDocument((Document) value));
        } else if (value instanceof List) {
          return JSON.toJSONString(parseArray(value));
        }
        return parseBasicType(value).toString();
      case "Integer":
        return Integer.valueOf(value.toString());
      case "Long":
        return new BigDecimal(value.toString()).toBigInteger().longValue();
      case "Float":
        return Float.valueOf(value.toString());
      case "Double":
        return Double.valueOf(value.toString());
      case "Timestamp":
        if (value instanceof BsonTimestamp) {
          int result = ((BsonTimestamp) value).getTime();
          return new Timestamp(result * 1000L);
        } else if (value instanceof Date) {
          return new Timestamp(((Date) value).getTime());
        }
        return value;
      case "byte[]":
        return ((Binary) value).getData();
      default:
        return value;
    }
  }

  private List parseListTypeInfo(Object value, ListTypeInfo typeInfo) {
    List result = new ArrayList();
    TypeInformation elementTypeInfo = typeInfo.getElementTypeInfo();
    List list = (List) value;
    for (int i = 0; i < list.size(); i++) {
      Object element = list.get(i);
      Object v = parseObject(element, elementTypeInfo);
      result.add(v);
    }
    return result;
  }

  private Map parseMapTypeInfo(Object value, MapTypeInfo typeInfo) {
    Map result = new HashMap();
    TypeInformation keyTypeInfo = typeInfo.getKeyTypeInfo();
    TypeInformation valueTypeInfo = typeInfo.getValueTypeInfo();
    Map map = (Map) value;
    for (Object key : map.keySet()) {
      Object k = parseObject(key, keyTypeInfo);
      Object v = parseObject(map.get(key), valueTypeInfo);
      result.put(k, v);
    }
    return result;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private Object parseBasicType(Object value) {
    if (value instanceof ObjectId) {
      return ((ObjectId) value).toString();
    } else if (value instanceof BsonTimestamp) {
      int result = ((BsonTimestamp) value).getTime();
      return new Timestamp(result * 1000L);
    } else if (value instanceof Binary) {
      // org.bson.types.Binary to base64 encoded string.
      // Ref: https://stackoverflow.com/questions/9338989/what-does-the-0-mean-in-mongodbs-bindata0-e8menzzofymmd7wshdnrfjyek8m
      return new String(Base64.getEncoder().encode(((Binary) value).getData()));
    } else if (value instanceof Date) {
      return new Timestamp(((Date) value).getTime());
    } else {
      return value;
    }
  }

  private List parseArray(Object value) {
    List result = new ArrayList();
    List l = (List) value;
    for (int i = 0; i < l.size(); i++) {
      Object element = l.get(i);
      Object v;
      if (element instanceof Document) {
        v = parseDocument((Document) element);
      } else if (element instanceof List) {
        v = parseArray(element);
      } else {
        v = parseBasicType(element);
      }
      result.add(v);
    }
    return result;
  }

  private Map parseDocument(Document item) {
    Map<String, Object> map = new HashMap<>();
    for (String key : item.keySet()) {
      Object value = item.get(key);
      if (value instanceof List) {
        Object array = parseArray(value);
        map.put(key, array);
      } else if (value instanceof Document) {
        Object m = parseDocument((Document) value);
        map.put(key, m);
      } else {
        Object basic = parseBasicType(value);
        map.put(key, basic);
      }
    }
    return map;
  }

  private Object parseObject(Object item, TypeInformation typeInfo) {
    // BasicTypeInfo/byte[]/SqlTimeTypeInfo
    if (typeInfo instanceof BasicTypeInfo || typeInfo instanceof PrimitiveArrayTypeInfo || typeInfo instanceof SqlTimeTypeInfo) {
      Object basic = parseBasicTypeInfo(item, typeInfo);
      return basic;
    } else if (typeInfo instanceof ListTypeInfo) {
      Object array = parseListTypeInfo(item, (ListTypeInfo) typeInfo);
      return array;
    } else if (typeInfo instanceof MapTypeInfo) {
      Object map = parseMapTypeInfo(item, (MapTypeInfo) typeInfo);
      return map;
    } else {
      throw new RuntimeException("Unsupported TypeInformation: " + typeInfo);
    }
  }

  @Override
  public Row buildRow(Row row, String mandatoryEncoding) throws BitSailException {
    String[] fieldNames = rowTypeInfo.getFieldNames();
    Document item = cursor.next();
    for (int i = 0; i < row.getArity(); i++) {
      String fieldName = fieldNames[i];
      TypeInformation typeInfo = rowTypeInfo.getTypeAt(i);
      Object value = item.get(fieldName);
      if (value != null) {
        try {
          Object obj = parseObject(value, typeInfo);
          row.setField(i, obj);
        } catch (Exception e) {
          throw BitSailException.asBitSailException(CommonErrorCode.CONVERT_NOT_SUPPORT,
              String.format("field name: %s can not convert value: %s to type: %s.", fieldName, value, typeInfo));
        }
      } else {
        row.setField(i, null);
      }
    }
    return row;
  }

  private void initMongoConnConfig() {
    String hostsStr = "";
    String host = "";
    int port = 0;

    if (inputSliceConfig.fieldExists(MongoDBReaderOptions.HOSTS_STR)) {
      hostsStr = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.HOSTS_STR, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    } else {
      host = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.HOST, MongoDBPluginsErrorCode.REQUIRED_VALUE);
      port = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.PORT, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    }

    String dbName = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.DB_NAME, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    String collectionName = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.COLLECTION_NAME, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    String userName = inputSliceConfig.get(MongoDBReaderOptions.USER_NAME);
    String password = inputSliceConfig.get(MongoDBReaderOptions.PASSWORD);
    String authDbName = inputSliceConfig.getUnNecessaryOption(MongoDBReaderOptions.AUTH_DB_NAME, null);
    this.mongoConnConfig = MongoConnConfig
        .builder()
        .authDbName(authDbName)
        .hostsStr(hostsStr)
        .host(host)
        .port(port)
        .dbName(dbName)
        .collectionName(collectionName)
        .userName(userName)
        .password(password)
        .build();
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  @Override
  public void initPlugin() {
    initMongoConnConfig();

    List<ColumnInfo> columnInfos = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.COLUMNS, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    this.splitKey = inputSliceConfig.getNecessaryOption(MongoDBReaderOptions.SPLIT_PK, MongoDBPluginsErrorCode.REQUIRED_VALUE);
    this.filter = inputSliceConfig.getUnNecessaryOption(MongoDBReaderOptions.FILTER, null);

    // Unnecessary values
    this.fetchSize = inputSliceConfig.getUnNecessaryOption(MongoDBReaderOptions.READER_FETCH_SIZE, 100000);
    this.parallelismNum = inputSliceConfig.getUnNecessaryOption(MongoDBReaderOptions.READER_PARALLELISM_NUM, -1);
    if (this.parallelismNum < 0) {
      if (Strings.isNullOrEmpty(this.filter)) {
        this.parallelismNum = DEFAULT_PARALLELISM_NUM;
        LOG.info("No reader_parallelism_num, use default value: [{}]", DEFAULT_PARALLELISM_NUM);
      } else {
        this.parallelismNum = 1;
        LOG.info("The reader_parallelism_num will be set to [1] for filter query");
      }
    }

    if (!isIndexKey()) {
      LOG.warn("Warning: [" + this.splitKey + "] is not indexed key!");
    }
    LOG.info("Split key is: " + this.splitKey);

    MongoSplitterFactory factory = MongoSplitterFactory.builder()
        .connConfig(mongoConnConfig)
        .fetchSize(fetchSize)
        .splitKey(splitKey)
        .parallelism(parallelismNum)
        .build();
    MongoSplitter splitter = factory.getSplitter(inputSliceConfig);
    splitter.assign();
    this.taskGroupInfo = splitter.getTaskGroupInfo();
    this.totalSplitNum = splitter.getTotalSplitNum();

    this.rowTypeInfo = NativeFlinkTypeInfoUtil.getRowTypeInformation(columnInfos, new MongoTypeInfoConverter());

    LOG.info("Row Type Info: " + rowTypeInfo);
  }

  private boolean isIndexKey() {
    MongoClient mongoClient = MongoDBUtil.initMongoClientWithRetry(mongoConnConfig);
    MongoCollection<Document> collection = mongoClient.getDatabase(mongoConnConfig.getDbName()).getCollection(mongoConnConfig.getCollectionName());
    Set<String> indexKeys = MongoDBUtil.getCollectionIndexKey(collection);
    LOG.info("Index keys in collection [" + mongoConnConfig.getCollectionName() + "] are: " + indexKeys);
    return indexKeys.contains(splitKey);
  }

  @Override
  public boolean isSplitEnd() {

    if (getTaskRangeInfos().isEmpty()) {
      LOG.info("Task range list is empty. Will not read any data.");
      return true;
    }

    hasNext = cursor.hasNext();

    while (!hasNext && (getCurrentTaskId() < getTaskRangeInfos().size())) {
      fetchNextResultSet();
      hasNext = cursor.hasNext();
    }

    return !hasNext;
  }

  @Override
  public void open(InputSplit inputSplit) {
    currentSplitNum = inputSplit.getSplitNumber();
    jobIdOffsetInfo.putIfAbsent(currentSplitNum, 0);

    if (getTaskRangeInfos().isEmpty()) {
      LOG.info("Task range list is empty. Will not read any data.");
      return;
    }
    fetchNextResultSet();
  }

  private Range fetchNextResultSet() {
    Range range = getRangeInfo();

    if (cursor != null) {
      cursor.close();
    }

    Object lowerBound = range.getLowerBound();
    Object upperBound = range.getUpperBound();
    Document filter = new Document();
    if (lowerBound == null) {
      if (upperBound != null) {
        filter.append(splitKey, new Document("$lt", upperBound));
      }
    } else {
      if (upperBound == null) {
        filter.append(splitKey, new Document("$gte", lowerBound));
      } else {
        filter.append(splitKey, new Document("$gte", lowerBound).append("$lt", upperBound));
      }
    }

    if (StringUtils.isNotEmpty(this.filter)) {
      Document queryFilter = Document.parse(this.filter);
      filter = new Document("$and", Arrays.asList(filter, queryFilter));
    }

    cursor = collection.find(filter).iterator();
    return range;
  }

  @SuppressWarnings("checkstyle:MagicNumber")
  private synchronized Range getRangeInfo() {
    List<Range> currentTaskGroupRangeList = getTaskRangeInfos();

    int currentTaskId = getCurrentTaskId();
    jobIdOffsetInfo.put(currentSplitNum, currentTaskId + 1);
    Range range = currentTaskGroupRangeList.get(currentTaskId);
    incCompletedSplits(1);

    if (currentTaskId % 1000 == 0) {
      LOG.info("Start to process range info: " + range + "\t[" + currentTaskId + "/" + currentTaskGroupRangeList.size() + "]");
    }
    return range;
  }

  @Override
  public void completeSplits() {

  }

  private Integer getCurrentTaskId() {
    return jobIdOffsetInfo.get(currentSplitNum);
  }

  private List<Range> getTaskRangeInfos() {
    if (null == taskGroupInfo) {
      return Collections.emptyList();
    }
    final List<Range> result = taskGroupInfo.get(currentSplitNum);
    return result == null ? Collections.emptyList() : result;
  }

  @Override
  public void close() {
    LOG.info("Subtask {} start closing.", getRuntimeContext().getIndexOfThisSubtask());
    if (cursor != null) {
      cursor.close();
    }
    LOG.info("Subtask {} closed.", getRuntimeContext().getIndexOfThisSubtask());
  }

  @Override
  public String getType() {
    return "mongodb";
  }
}
