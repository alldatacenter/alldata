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

package com.bytedance.bitsail.connector.legacy.mongodb.util;

import com.bytedance.bitsail.connector.legacy.mongodb.common.MongoConnConfig;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Strings;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("checkstyle:MagicNumber")
public class MongoDBWriterUtil {
  public static Retryer<MongoClient> retryer = RetryerBuilder.<MongoClient>newBuilder()
      .retryIfResult(Objects::isNull)
      .retryIfException()
      .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
      .withStopStrategy(StopStrategies.stopAfterAttempt(3))
      .build();

  public static MongoClient buildMongoClientWithRetry(MongoConnConfig mongoConnConfig) {
    Callable<MongoClient> callable = () -> buildMongoClient(mongoConnConfig);
    try {
      return retryer.call(callable);
    } catch (RetryException | ExecutionException e) {
      throw new RuntimeException("Error while init mongo client. " + e.getMessage(), e);
    }
  }

  private static MongoClient buildMongoClient(MongoConnConfig mongoConnConfig) {
    if (mongoConnConfig.getClientMode() == MongoConnConfig.CLIENT_MODE.URL) {
      return getMongoClientByUrl(mongoConnConfig);
    } else {
      List<ServerAddress> addressList = getAddrListFromConfig(mongoConnConfig);
      switch (mongoConnConfig.getClientMode()) {
        case HOST_WITHOUT_CREDENTIAL:
          return new MongoClient(addressList, mongoConnConfig.getClientOption());
        case HOST_WITH_CREDENTIAL:
          return getMongoClientByCredential(addressList, mongoConnConfig);
        default:
          throw new RuntimeException("Mongo client mode is not support: " + mongoConnConfig.getClientMode());
      }
    }
  }

  private static MongoClient getMongoClientByUrl(MongoConnConfig mongoConnConfig) {
    MongoClientURI clientUri = new MongoClientURI(mongoConnConfig.getMongoUrl());
    mongoConnConfig.setDbName(clientUri.getDatabase());
    return new MongoClient(clientUri);
  }

  private static MongoClient getMongoClientByCredential(List<ServerAddress> addressList, MongoConnConfig mongoConnConfig) {
    String dbName = Strings.isNullOrEmpty(mongoConnConfig.getAuthDbName()) ? mongoConnConfig.getDbName() : mongoConnConfig.getAuthDbName();
    MongoCredential credential = MongoCredential.createCredential(
        mongoConnConfig.getUserName(),
        dbName,
        mongoConnConfig.getPassword().toCharArray());
    return new MongoClient(addressList, credential, mongoConnConfig.getClientOption());
  }

  private static List<ServerAddress> getAddrListFromConfig(MongoConnConfig mongoConnConfig) {
    List<ServerAddress> addressList;
    if (StringUtils.isNotEmpty(mongoConnConfig.getHostsStr())) {
      addressList = MongoDBUtil.getServerAddress(mongoConnConfig.getHostsStr());
    } else {
      addressList = Collections.singletonList(new ServerAddress(mongoConnConfig.getHost(), mongoConnConfig.getPort()));
    }
    return addressList;
  }
}
