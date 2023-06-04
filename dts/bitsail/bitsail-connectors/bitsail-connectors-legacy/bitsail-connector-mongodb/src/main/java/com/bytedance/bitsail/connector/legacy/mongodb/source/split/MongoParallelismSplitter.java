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

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.mongodb.MongoCommandException;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created 2021/5/31
 *
 * @author ke.hao
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MongoParallelismSplitter extends MongoSplitter {
  private static final Logger LOG = LoggerFactory.getLogger(MongoParallelismSplitter.class);

  private static final int MONGO_UNAUTHORIZED_ERR_CODE = 13;
  private static final int MONGO_ILLEGALOP_ERR_CODE = 20;

  public MongoParallelismSplitter(MongoConnConfig mongoConnConfig,
                                  int parallelism,
                                  String splitKey) {
    super(mongoConnConfig, parallelism, splitKey);
  }

  private static int getAvgObjSize(Document collStats) {
    int avgObjSize = 1;
    Object avgObjSizeObj = collStats.get("avgObjSize");
    if (avgObjSizeObj instanceof Integer) {
      avgObjSize = ((Integer) avgObjSizeObj).intValue();
    } else if (avgObjSizeObj instanceof Double) {
      avgObjSize = ((Double) avgObjSizeObj).intValue();
    }
    return avgObjSize;
  }

  @Override
  public List<Range> splitRangeBySplitKey() {
    List<Range> rangeList = Lists.newArrayList();
    if (taskGroupNum == 1) {
      LOG.info("Task parallelism equals 1, split will skip!");
      return Lists.newArrayList(new Range(null, null));
    }

    Stopwatch started = Stopwatch.createStarted();
    try {
      Document collStats = database.runCommand(new Document("collStats", mongoConnConfig.getCollectionName()));
      Object count = collStats.get("count");
      long docCount = 0;
      if (count instanceof Double) {
        docCount = ((Double) count).longValue();
      } else if (count instanceof Integer) {
        docCount = (Integer) count;
      }

      LOG.info("Doc count: " + docCount);
      if (docCount == 0) {
        return rangeList;
      }

      int splitsCount = taskGroupNum - 1;
      boolean splitVector = supportSplitVector();
      List<Object> splitPoints = null;
      if (splitVector) {
        splitPoints = splitVector(collStats, splitsCount, docCount);
      } else {
        splitPoints = splitSplitKey(splitsCount, docCount);
      }

      rangeList =  buildRangeListBySplitPoints(splitPoints);
      return rangeList;
    } finally {
      LOG.info("Split range time cost: {}.", started.elapsed(TimeUnit.SECONDS));
    }
  }

  private boolean supportSplitVector() {
    try {
      database.runCommand(new Document("splitVector", mongoConnConfig.getDbName() + "." + mongoConnConfig.getCollectionName())
          .append("keyPattern", new Document(splitKey, 1))
          .append("force", true));
    } catch (MongoCommandException e) {
      if (e.getErrorCode() == MONGO_UNAUTHORIZED_ERR_CODE ||
          e.getErrorCode() == MONGO_ILLEGALOP_ERR_CODE) {
        return false;
      }
    }
    return true;
  }

  private List<Object> splitVector(Document collStats,
                                   int splitsCount,
                                   long docCount) {
    LOG.info("Range will be split by command : splitVector.");
    List<Object> splitPoints = Lists.newArrayList();
    boolean forceMedianSplit = false;
    int avgObjSize = getAvgObjSize(collStats);
    long maxChunkSize = (docCount / splitsCount - 1) * 2 * avgObjSize / (1024 * 1024);
    if (maxChunkSize < 1) {
      forceMedianSplit = true;
    }
    Document result = null;
    if (!forceMedianSplit) {
      result = database.runCommand(new Document("splitVector",
        mongoConnConfig.getDbName() + "." + mongoConnConfig.getCollectionName())
        .append("keyPattern", new Document(splitKey, 1))
        .append("maxChunkSize", maxChunkSize)
        .append("maxSplitPoints", taskGroupNum - 1));
    } else {
      result = database.runCommand(new Document("splitVector",
        mongoConnConfig.getDbName() + "." + mongoConnConfig.getCollectionName())
        .append("keyPattern", new Document(splitKey, 1))
        .append("force", true));
    }
    ArrayList<Document> splitKeys = result.get("splitKeys", ArrayList.class);

    for (int i = 0; i < splitKeys.size(); i++) {
      Document splitKeyDoc = splitKeys.get(i);
      Object id = splitKeyDoc.get(splitKey);
      splitPoints.add(id);
    }
    return splitPoints;
  }

  public List<Object> splitSplitKey(int splitPointCount,
                                     long docCount) {
    LOG.info("Range will be split by splitKey.");
    List<Object> splitPoints = Lists.newArrayList();
    int chunkDocCount = (int) docCount / taskGroupNum;
    if (chunkDocCount == 0) {
      LOG.info("docCount is smaller than parallelism: " + docCount);
      chunkDocCount = (int) docCount - 1;
      splitPointCount = 1;
    }

    int skipCount = chunkDocCount;
    return getSplitPointsByRangeNumAndStep(splitPointCount, skipCount);
  }
}
