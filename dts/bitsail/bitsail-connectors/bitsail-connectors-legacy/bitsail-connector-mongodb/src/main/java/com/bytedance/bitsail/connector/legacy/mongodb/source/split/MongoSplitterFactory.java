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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnConfig;
import com.bytedance.bitsail.connector.legacy.mongodb.option.MongoDBReaderOptions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import static com.bytedance.bitsail.connector.legacy.mongodb.source.split.MongoSplitter.SplitterMode.PAGINATING;
import static com.bytedance.bitsail.connector.legacy.mongodb.source.split.MongoSplitter.SplitterMode.PARALLELISM;

/**
 * Created 2021/6/1
 *
 * @author ke.hao
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MongoSplitterFactory {

  private int fetchSize;
  private int parallelism;
  private String splitKey;
  private MongoConnConfig connConfig;

  public MongoSplitter getSplitter(BitSailConfiguration configuration) {
    String splitMode = StringUtils.lowerCase(configuration.get(MongoDBReaderOptions.SPLIT_MODE));

    switch (splitMode) {
      case PAGINATING:
        return new MongoPaginatingSplitter(connConfig, splitKey, fetchSize, parallelism);
      case PARALLELISM:
        return new MongoParallelismSplitter(connConfig, parallelism, splitKey);
      default:
    }

    throw new UnsupportedOperationException(String.format("Unsupported split mode %s on mongo.", splitMode));
  }

}
