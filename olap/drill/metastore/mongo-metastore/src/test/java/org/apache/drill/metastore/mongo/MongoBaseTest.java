/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.metastore.mongo;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.components.tables.AbstractBasicTablesRequestsTest;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Category(MetastoreTest.class)
public class MongoBaseTest extends AbstractBasicTablesRequestsTest {
  private static final Logger logger = LoggerFactory.getLogger(MongoBaseTest.class);

  private static final String MONGO_IMAGE_NAME = "mongo:4.4.10";
  private static final int MONGO_PORT = 27017;
  private static final String CONFIG_SERVER_HOST = "m0";
  private static final String CONFIG_REPL_SET = "conf";
  private static final String SHARD_REPL_SET_0 = "shard0";
  private static final String SHARD_REPL_SET_1 = "shard1";
  private static final List<GenericContainer<?>> containers = Lists.newArrayList();
  protected static boolean isShardMode = Boolean.parseBoolean(
    System.getProperty("drill.mongo.tests.shardMode", "false"));

  @BeforeClass
  public static void init() throws IOException, InterruptedException {
    String connectionString;
    if (isShardMode) {
      connectionString = initCluster();
    } else {
      connectionString = initSingle();
    }
    Config config = DrillConfig.create()
      .withValue(MongoConfigConstants.CONNECTION,
        ConfigValueFactory.fromAnyRef(connectionString));
    innerInit(config, MongoMetastore.class);
  }

  private static String initSingle() {
    MongoDBContainer container = new MongoDBContainer(MONGO_IMAGE_NAME);
    container.start();
    containers.add(container);
    return String.format("mongodb://%s:%d",
      container.getContainerIpAddress(), container.getFirstMappedPort());
  }

  private static String initCluster() throws IOException, InterruptedException {
    Network network = Network.newNetwork();
    initConfigServer(network);
    initShardServers(network);
    String connectionString = initMongos(network);
    shardCollection();
    return connectionString;
  }

  private static void initConfigServer(Network network) throws IOException,
    InterruptedException {
    GenericContainer<?> configServer =
      newContainer(network, "configsvr", CONFIG_REPL_SET, CONFIG_SERVER_HOST);
    configServer.start();

    Container.ExecResult execResult = configServer.execInContainer("/bin/bash", "-c",
      String.format("echo 'rs.initiate({_id: \"%s\", configsvr: true, " +
        "members: [{ _id : 0, host : \"%s:%s\" }]})' | mongo --port %3$s", CONFIG_REPL_SET, CONFIG_SERVER_HOST, MONGO_PORT));
    logger.debug(execResult.toString());
    containers.add(configServer);
  }

  private static void initShardServers(Network network) throws IOException,
    InterruptedException {
    List<GenericContainer<?>> shards = Lists.newArrayList();

    shards.addAll(Stream.of("m1", "m2", "m3")
      .map(host -> newContainer(network, "shardsvr", SHARD_REPL_SET_0, host))
      .collect(Collectors.toList()));
    shards.addAll(Stream.of("m4", "m5", "m6")
      .map(host -> newContainer(network, "shardsvr", SHARD_REPL_SET_1, host))
      .collect(Collectors.toList()));
    shards.forEach(GenericContainer::start);

    Container.ExecResult execResult = shards.get(0).execInContainer("/bin/bash",
      "-c", String.format("mongo --port %s --eval 'printjson(rs.initiate(" +
          "{_id:\"%s\",members:[{_id:0,host:\"m1:%1$s\"},{_id:1,host:\"m2:%1$s\"}," +
          "{_id:2,host:\"m3:%1$s\"}]}))' --quiet",
      MONGO_PORT, SHARD_REPL_SET_0));
    logger.debug(execResult.toString());
    execResult = shards.get(0).execInContainer("/bin/bash", "-c",
      String.format("until mongo --port %s --eval \"printjson(rs.isMaster())\" " +
        "| grep ismaster | grep true > /dev/null 2>&1;do sleep 1;done", MONGO_PORT));
    logger.debug(execResult.toString());

    execResult = shards.get(3).execInContainer("/bin/bash", "-c",
      String.format("mongo --port %s --eval 'printjson(rs.initiate(" +
          "{_id:\"%s\",members:[{_id:0,host:\"m4:%1$s\"},{_id:1,host:\"m5:%1$s\"}," +
          "{_id:2,host:\"m6:%1$s\"}]}))' --quiet",
        MONGO_PORT, SHARD_REPL_SET_1));
    logger.debug(execResult.toString());
    execResult = shards.get(3).execInContainer("/bin/bash", "-c",
      String.format("until mongo --port %s --eval \"printjson(rs.isMaster())\" " +
        "| grep ismaster | grep true > /dev/null 2>&1;do sleep 1;done", MONGO_PORT));
    logger.debug(execResult.toString());
    containers.addAll(shards);
  }

  private static String initMongos(Network network) throws IOException,
    InterruptedException {
    String mongosHost = "m7";
    MongoDBContainer mongos = new MongoDBContainer(MONGO_IMAGE_NAME)
      .withNetwork(network)
      .withNetworkAliases(mongosHost)
      .withExposedPorts(MONGO_PORT)
      .withCommand(String.format("mongos --configdb %s/%s:%s --bind_ip " +
          "localhost,%s --port %3$s", CONFIG_REPL_SET, CONFIG_SERVER_HOST, MONGO_PORT,
        mongosHost));
    mongos.start();

    Container.ExecResult execResult = mongos.execInContainer("/bin/bash", "-c",
      String.format("echo 'sh.addShard(\"%s/m1,m2,m3\")' | mongo --port %s",
        SHARD_REPL_SET_0, MONGO_PORT));
    logger.debug(execResult.toString());
    execResult = mongos.execInContainer("/bin/bash", "-c",
      String.format("echo 'sh.addShard(\"%s/m4,m5,m6\")' | mongo --port %s",
        SHARD_REPL_SET_1, MONGO_PORT));
    logger.debug(execResult.toString());

    logger.debug("Execute list shards.");
    execResult = mongos.execInContainer("/bin/bash", "-c", "mongo --eval 'db" +
      ".adminCommand({ listShards: 1 })' --port " + MONGO_PORT);
    logger.debug(execResult.toString());
    containers.add(mongos);

    // the way how it work: client -> router(mongos) -> Shard1 ... ShardN
    return String.format("mongodb://%s:%s", mongos.getContainerIpAddress(), mongos.getMappedPort(MONGO_PORT));
  }

  private static void shardCollection() throws IOException, InterruptedException {
    // Enabled sharding at database level
    logger.debug("Enabled sharding for database: {}", MongoConfigConstants.DEFAULT_DATABASE);
    Container.ExecResult execResult = containers.get(containers.size() - 1)
      .execInContainer("/bin/bash", "-c",
        String.format("mongo --eval 'db.adminCommand({enableSharding:\"%s\"})'",
          MongoConfigConstants.DEFAULT_DATABASE));
    logger.debug(execResult.toString());

    // Shard the collection
    logger.debug("Shard the collection: {}.{}", MongoConfigConstants.DEFAULT_DATABASE,
      MongoConfigConstants.DEFAULT_TABLE_COLLECTION);
    execResult = containers.get(containers.size() - 1)
      .execInContainer("/bin/bash", "-c",
        String.format("echo 'sh.shardCollection(\"%s.%s\", {\"%s\" : \"hashed\"})' " +
            "| mongo ", MongoConfigConstants.DEFAULT_DATABASE,
          MongoConfigConstants.DEFAULT_TABLE_COLLECTION, MongoConfigConstants.ID));
    logger.debug(execResult.toString());
  }

  private static GenericContainer<?> newContainer(Network network, String type,
                                                  String replSet, String host) {
    return new GenericContainer<>(MONGO_IMAGE_NAME)
      .withNetwork(network)
      .withNetworkAliases(host)
      .withExposedPorts(MONGO_PORT)
      .withCommand(String.format("mongod --port %d --%s --replSet %s " +
        "--bind_ip localhost,%s", MONGO_PORT, type, replSet, host));
  }

  @AfterClass
  public static void tearDownCluster() {
    containers.forEach(GenericContainer::stop);
  }
}
