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

package com.bytedance.bitsail.connector.legacy.mongodb.source.split;

import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnConfig;
import com.bytedance.bitsail.connector.legacy.mongodb.util.MongoDBUtil;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import lombok.Getter;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public abstract class MongoSplitter {

  private TaskGroupInfo taskGroupInfo;
  public MongoConnConfig mongoConnConfig;

  /**
   * The max parallelism num
   */
  public int taskGroupNum;
  public transient MongoClient mongoClient;
  public MongoCollection<Document> collection;
  public MongoDatabase database;
  public String splitKey;

  @Getter
  private int totalSplitNum = -1;

  public MongoSplitter(MongoConnConfig mongoConnConfig, int taskGroupNum, String splitKey) {
    this.taskGroupNum = taskGroupNum;
    this.mongoConnConfig = mongoConnConfig;
    this.splitKey = splitKey;
    mongoClient = MongoDBUtil.initMongoClientWithRetry(mongoConnConfig);
    database = mongoClient.getDatabase(this.mongoConnConfig.getDbName());
    collection = database.getCollection(this.mongoConnConfig.getCollectionName());
    taskGroupInfo = new TaskGroupInfo(taskGroupNum);
  }

  public abstract List<Range> splitRangeBySplitKey();

  public void assign() {
    List<Range> rangeList = splitRangeBySplitKey();
    for (int i = 0; i < rangeList.size(); i++) {
      Range range = rangeList.get(i);
      int index = i % taskGroupNum;
      taskGroupInfo.get(index).add(range);
    }
    totalSplitNum = rangeList.size();
  }

  /**
   * Compute split points according to region number and region size.
   */
  public List<Object> getSplitPointsByRangeNumAndStep(int rangeNum, int step) {
    List<Object> splitPoints = new ArrayList<>(rangeNum);
    int num = 0;
    Document result;
    Object obj = null;
    if (rangeNum == 0) {
      result = collection.find().sort(new BasicDBObject(splitKey, 1)).limit(1).first();
      obj = result.get(splitKey);
      splitPoints.add(obj);
    } else {
      while (num < rangeNum) {
        if (num == 0) {
          result = collection.find().sort(new BasicDBObject(splitKey, 1)).limit(1).skip(step).first();
        } else {
          result = collection.find(Filters.gte(splitKey, obj)).sort(new BasicDBObject(splitKey, 1)).limit(1).skip(step).first();
        }
        num++;
        obj = result.get(splitKey);
        splitPoints.add(obj);
      }
    }

    return splitPoints;
  }

  /**
   * Construct range list according to split points.
   */
  public List<Range> buildRangeListBySplitPoints(List<Object> splitPoints) {
    List<Range> rangeList = Lists.newArrayList();
    Object lastValue = null;
    for (Object splitPoint : splitPoints) {
      Range range = new Range(lastValue, splitPoint);
      lastValue = splitPoint;
      rangeList.add(range);
    }
    Range range = new Range(lastValue, null);
    rangeList.add(range);
    return rangeList;
  }

  public TaskGroupInfo getTaskGroupInfo() {
    return taskGroupInfo;
  }

  public interface SplitterMode {
    /**
     * Default split strategy: records number / fetchSize
     */
    String PAGINATING = "paginating";

    /**
     * records number / parallelism
     */
    String PARALLELISM = "parallelism";
  }
}

