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
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("checkstyle:MagicNumber")
@Slf4j
public class MongoDBUtil {
  public static Retryer<MongoClient> retryer = RetryerBuilder.<MongoClient>newBuilder()
      .retryIfResult(Objects::isNull)
      .retryIfException()
      .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
      .withStopStrategy(StopStrategies.stopAfterAttempt(3))
      .build();

  private static final String HOST_SPLIT_REGEX = ",\\s*";

  private static final Pattern HOST_PORT_PATTERN = Pattern.compile("(?<host>.*):(?<port>\\d+)*");

  private static final Integer DEFAULT_PORT = 27017;

  public static MongoClient initMongoClientWithRetry(MongoConnConfig mongoConnConfig) {
    Callable<MongoClient> callable = () -> {
      List<ServerAddress> addressList;
      if (StringUtils.isNotEmpty(mongoConnConfig.getHostsStr())) {
        addressList = getServerAddress(mongoConnConfig.getHostsStr());
      } else {
        addressList = Collections.singletonList(new ServerAddress(mongoConnConfig.getHost(), mongoConnConfig.getPort()));
      }
      String database = StringUtils.isEmpty(mongoConnConfig.getAuthDbName()) ?
          mongoConnConfig.getDbName()
          : mongoConnConfig.getAuthDbName();
      return initMongoClient(addressList, mongoConnConfig.getUserName(), mongoConnConfig.getPassword(), database);
    };

    try {
      return retryer.call(callable);
    } catch (RetryException | ExecutionException e) {
      throw new RuntimeException("Error while init mongo client. " + e.getMessage(), e);
    }
  }

  /**
   * parse server address from hostPorts string
   */
  public static List<ServerAddress> getServerAddress(String hostPorts) {
    List<ServerAddress> addresses = new ArrayList<>();

    for (String hostPort : hostPorts.split(HOST_SPLIT_REGEX)) {
      if (hostPort.length() == 0) {
        continue;
      }

      Matcher matcher = HOST_PORT_PATTERN.matcher(hostPort);
      if (matcher.find()) {
        String host = matcher.group("host");
        String portStr = matcher.group("port");
        int port = portStr == null ? DEFAULT_PORT : Integer.parseInt(portStr);

        ServerAddress serverAddress = new ServerAddress(host, port);
        addresses.add(serverAddress);
      }
    }

    return addresses;
  }

  public static MongoClient initMongoClient(List<ServerAddress> addressList, String userName, String password, String dbName) {
    MongoClientSettings.Builder settingBuilder = MongoClientSettings.builder()
        .applyToClusterSettings(builder -> builder.hosts(addressList));

    if (StringUtils.isNotEmpty(userName) && StringUtils.isNotEmpty(password)) {
      MongoCredential credential = MongoCredential.createCredential(userName, dbName, password.toCharArray());
      settingBuilder.credential(credential);
    }
    return MongoClients.create(settingBuilder.build());
  }

  public static MongoClient initNoCredentialMongoClient(String host, int port) {
    return MongoClients.create(
        MongoClientSettings.builder()
            .applyToClusterSettings(builder ->
                builder.hosts(Collections.singletonList(new ServerAddress(host, port))))
            .build()
    );
  }

  public static Set<String> getCollectionIndexKey(MongoCollection<Document> collection) {
    ListIndexesIterable<Document> currentIndexes = collection.listIndexes();
    MongoCursor<Document> cursor = currentIndexes.iterator();
    Set<String> indexNames = new HashSet<>();
    while (cursor.hasNext()) {
      Document next = cursor.next();
      Document key = (Document) next.get("key");
      String name = (String) key.keySet().toArray()[0];
      indexNames.add(name);
    }
    return indexNames;
  }
}


