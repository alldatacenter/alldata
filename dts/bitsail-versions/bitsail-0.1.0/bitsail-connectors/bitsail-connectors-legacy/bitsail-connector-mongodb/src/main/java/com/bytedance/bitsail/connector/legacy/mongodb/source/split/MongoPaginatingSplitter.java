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

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MongoPaginatingSplitter extends MongoSplitter implements Serializable {

  private int fetchSize;

  public MongoPaginatingSplitter(MongoConnConfig mongoConnConfig, String splitKey, int fetchSize, int taskGroupNum) {
    super(mongoConnConfig, taskGroupNum, splitKey);
    this.fetchSize = fetchSize;
  }

  @Override
  public List<Range> splitRangeBySplitKey() {
    List<Range> rangeList = new ArrayList<>();
    long count = collection.count();
    if (count == 0) {
      return rangeList;
    }
    int splitPointsCount = 0;
    if (count >= this.fetchSize) {
      splitPointsCount = this.fetchSize <= 1 ? (int) (count - 1) : (int) (count / this.fetchSize - 1);
    }

    List<Object> splitPoints = getSplitPointsByRangeNumAndStep(splitPointsCount, this.fetchSize);
    rangeList = buildRangeListBySplitPoints(splitPoints);
    return rangeList;
  }
}

