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
package org.apache.drill.exec.store.mongo;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.apache.drill.categories.MongoStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(Suite.class)
@Suite.SuiteClasses({
  TestMongoFilterPushDown.class,
  TestMongoProjectPushDown.class,
  TestMongoQueries.class,
  TestMongoLimitPushDown.class,
  TestMongoChunkAssignment.class,
  TestMongoStoragePluginUsesCredentialsStore.class,
  TestMongoDrillIssue.class
})

@Category({SlowTest.class, MongoStorageTest.class})
public class MongoTestSuite extends BaseTest implements MongoTestConstants {

  private static final Logger logger = LoggerFactory.getLogger(MongoTestSuite.class);
  protected static MongoClient mongoClient;

  private static final boolean distMode = Boolean.parseBoolean(System.getProperty("drill.mongo.tests.shardMode", "false"));
  private static volatile String connectionURL = null;
  private static final AtomicInteger initCount = new AtomicInteger(0);

  private static ContainerManager containerManager;

  public static String getConnectionURL() {
    return connectionURL;
  }

  private abstract static class ContainerManager {
    protected static List<GenericContainer<?>> mongoContainers;

    public abstract String setup() throws Exception;

    public void cleanup() {
      mongoContainers.forEach(GenericContainer::stop);
    }

    public GenericContainer<?> getMasterContainer() {
      return mongoContainers.iterator().next();
    }
  }

  private static GenericContainer<?> newContainer(Network network, String host) {
    GenericContainer<?> container = new GenericContainer<>("mongo:4.4.10")
        .withNetwork(network)
        .withNetworkAliases(host)
        .withExposedPorts(MONGOS_PORT)
        .withCommand(String.format("mongod --port %d --shardsvr --replSet rs0 --bind_ip localhost,%s", MONGOS_PORT, host));
    return container;
  }

  private static class DistributedMode extends ContainerManager {

    @Override
    public String setup() throws Exception {
      Network network = Network.newNetwork();

      Stream.of("m1", "m2", "m3")
          .map(host -> newContainer(network, host))
          .collect(Collectors.toList());

      String configServerHost = "m4";
      GenericContainer<?> configServer = new GenericContainer<>("mongo:4.4.10")
          .withNetwork(network)
          .withNetworkAliases(configServerHost)
          .withExposedPorts(MONGOS_PORT)
          .withCommand(String.format("mongod --configsvr --port %s --replSet rs0conf --bind_ip localhost,%s", MONGOS_PORT, configServerHost));

      configServer.start();

      Container.ExecResult execResult = configServer.execInContainer("/bin/bash", "-c",
          String.format("echo 'rs.initiate({_id: \"rs0conf\",configsvr: true, members: [{ _id : 0, host : \"%s:%2$s\" }]})' | mongo --port %2$s", configServerHost, MONGOS_PORT));

      logger.info(execResult.toString());

      String mongosHost = "m5";
      GenericContainer<?> mongos = new GenericContainer<>("mongo:4.4.10")
          .withNetwork(network)
          .withNetworkAliases(mongosHost)
          .withExposedPorts(MONGOS_PORT)
          .withCommand(String.format("mongos --configdb rs0conf/%1$s:%2$s --bind_ip localhost,%3$s --port %2$s", configServerHost, MONGOS_PORT, mongosHost));

      mongos.start();

      mongoContainers.forEach(GenericContainer::start);

      GenericContainer<?> master = getMasterContainer();

      execResult = master.execInContainer("/bin/bash", "-c",
          String.format("mongo --port %1$s --eval 'printjson(rs.initiate({_id:\"rs0\"," +
              "members:[{_id:0,host:\"m1:%1$s\"},{_id:1,host:\"m2:%1$s\"},{_id:2,host:\"m3:%1$s\"}]}))' --quiet", MONGOS_PORT));
      logger.info(execResult.toString());

      execResult = master.execInContainer("/bin/bash", "-c",
          String.format("until mongo --port %s --eval \"printjson(rs.isMaster())\" | grep ismaster | grep true > /dev/null 2>&1;do sleep 1;done", MONGOS_PORT));
      logger.info(execResult.toString());

      execResult = mongos.execInContainer("/bin/bash", "-c", "echo 'sh.addShard(\"rs0/m1\")' | mongo --port " + MONGOS_PORT);
      logger.info(execResult.toString());

      String replicaSetUrl = String.format("mongodb://%s:%s", mongos.getContainerIpAddress(), mongos.getMappedPort(MONGOS_PORT));

      mongoClient = MongoClients.create(replicaSetUrl);

      logger.info("Execute list shards.");
      execResult = master.execInContainer("/bin/bash", "-c", "mongo --eval 'db.adminCommand({ listShards: 1 })' --port " + MONGOS_PORT);
      logger.info(execResult.toString());

      // Enabled sharding at database level
      logger.info("Enabled sharding at database level");
      execResult = mongos.execInContainer("/bin/bash", "-c", String.format("mongo --eval 'db.adminCommand( {\n" +
          "   enableSharding: \"%s\"\n" +
          "} )'", EMPLOYEE_DB));
      logger.info(execResult.toString());

      // Create index in sharded collection
      logger.info("Create index in sharded collection");
      MongoDatabase db = mongoClient.getDatabase(EMPLOYEE_DB);
      db.getCollection(EMPINFO_COLLECTION).createIndex(Indexes.ascending("employee_id"));

      // Shard the collection
      logger.info("Shard the collection: {}.{}", EMPLOYEE_DB, EMPINFO_COLLECTION);
      execResult = mongos.execInContainer("/bin/bash", "-c", String.format(
          "echo 'sh.shardCollection(\"%s.%s\", {\"employee_id\" : 1})' | mongo ", EMPLOYEE_DB, EMPINFO_COLLECTION));
      logger.info(execResult.toString());
      createMongoUser();
      createDbAndCollections(DONUTS_DB, DONUTS_COLLECTION, "id");
      createDbAndCollections(EMPLOYEE_DB, EMPTY_COLLECTION, "field_2");
      createDbAndCollections(DATATYPE_DB, DATATYPE_COLLECTION, "_id");

      // the way how it work: client -> router(mongos) -> Shard1 ... ShardN
      return String.format("mongodb://%s:%s", LOCALHOST, mongos.getMappedPort(MONGOS_PORT));
    }
  }

  public static class SingleMode extends ContainerManager {

    @Override
    public String setup() throws IOException {
      mongoContainers = Collections.singletonList(new GenericContainer<>("mongo:4.4.10")
          .withNetwork(Network.SHARED)
          .withNetworkAliases("M1")
          .withExposedPorts(MONGOS_PORT)
          .withCommand("--replSet rs0 --bind_ip localhost,M1"));

      mongoContainers.forEach(GenericContainer::start);
      GenericContainer<?> master = getMasterContainer();

      try {
        master.execInContainer("/bin/bash", "-c",
            "mongo --eval 'printjson(rs.initiate({_id:\"rs0\","
                + "members:[{_id:0,host:\"M1:27017\"}]}))' "
                + "--quiet");
        master.execInContainer("/bin/bash", "-c",
            "until mongo --eval \"printjson(rs.isMaster())\" | grep ismaster | grep true > /dev/null 2>&1;"
                + "do sleep 1;done");
      } catch (Exception e) {
        throw new IllegalStateException("Failed to initiate rs.", e);
      }

      String connectionString = String.format("mongodb://%s:%d", master.getContainerIpAddress(), master.getFirstMappedPort());
      mongoClient = MongoClients.create(connectionString);

      createMongoUser();
      createDbAndCollections(EMPLOYEE_DB, EMPINFO_COLLECTION, "employee_id");
      createDbAndCollections(EMPLOYEE_DB, SCHEMA_CHANGE_COLLECTION, "field_2");
      createDbAndCollections(EMPLOYEE_DB, EMPTY_COLLECTION, "field_2");
      createDbAndCollections(DATATYPE_DB, DATATYPE_COLLECTION, "_id");

      return connectionString;
    }
  }

  @BeforeClass
  public static void initMongo() throws Exception {
    synchronized (MongoTestSuite.class) {
      if (initCount.get() == 0) {
        if (distMode) {
          logger.info("Executing tests in distributed mode");
          containerManager = new DistributedMode();
        } else {
          logger.info("Executing tests in single mode");
          containerManager = new SingleMode();
        }
        connectionURL = containerManager.setup();
        // ToDo DRILL-7269: fix the way how data are imported for the sharded mongo cluster
        containerManager.getMasterContainer().copyFileToContainer(Transferable.of(Files.asCharSource(new File(Resources.getResource(EMP_DATA).toURI()), Charsets.UTF_8).read().getBytes()), EMP_DATA);
        containerManager.getMasterContainer().copyFileToContainer(Transferable.of(Files.asCharSource(new File(Resources.getResource(SCHEMA_CHANGE_DATA).toURI()), Charsets.UTF_8).read().getBytes()), SCHEMA_CHANGE_DATA);
        containerManager.getMasterContainer().copyFileToContainer(Transferable.of(Files.asCharSource(new File(Resources.getResource(DONUTS_DATA).toURI()), Charsets.UTF_8).read().getBytes()), DONUTS_DATA);
        containerManager.getMasterContainer().copyFileToContainer(Transferable.of(Files.asCharSource(new File(Resources.getResource(DATATYPE_DATA).toURI()), Charsets.UTF_8).read().getBytes()), DATATYPE_DATA);
        TestTableGenerator.importData(containerManager.getMasterContainer(), EMPLOYEE_DB, EMPINFO_COLLECTION, EMP_DATA);
        TestTableGenerator.importData(containerManager.getMasterContainer(), EMPLOYEE_DB, SCHEMA_CHANGE_COLLECTION, SCHEMA_CHANGE_DATA);
        TestTableGenerator.importData(containerManager.getMasterContainer(), DONUTS_DB, DONUTS_COLLECTION, DONUTS_DATA);
        TestTableGenerator.importData(containerManager.getMasterContainer(), DATATYPE_DB, DATATYPE_COLLECTION, DATATYPE_DATA);
        TestTableGenerator.importData(containerManager.getMasterContainer(), ISSUE7820_DB, ISSUE7820_COLLECTION, EMP_DATA);
      }
      initCount.incrementAndGet();
    }
  }

  private static void createDbAndCollections(String dbName,
      String collectionName, String indexFieldName) {
    MongoDatabase db = mongoClient.getDatabase(dbName);
    MongoCollection<Document> mongoCollection = db.getCollection(collectionName);
    if (mongoCollection == null) {
      db.createCollection(collectionName);
      mongoCollection = db.getCollection(collectionName);
    }

    if (indexFieldName.equals("_id")) {
      // Mongo 3.4 and later already makes an index for a field named _id
      return;
    }

    IndexOptions indexOptions = new IndexOptions().unique(true).background(false).name(indexFieldName);
    Bson keys = Indexes.ascending(indexFieldName);
    mongoCollection.createIndex(keys, indexOptions);
  }

  private static void createMongoUser() throws IOException {
    Configuration configuration = new Configuration();
    String storeName = "mongo";
    char[] usernameChars = configuration.getPassword(DrillMongoConstants.STORE_CONFIG_PREFIX + storeName + DrillMongoConstants.USERNAME_CONFIG_SUFFIX);
    char[] passwordChars = configuration.getPassword(DrillMongoConstants.STORE_CONFIG_PREFIX + storeName + DrillMongoConstants.PASSWORD_CONFIG_SUFFIX);
    if (usernameChars != null && passwordChars != null) {
      String username = URLEncoder.encode(new String(usernameChars), "UTF-8");
      String password = URLEncoder.encode(new String(passwordChars), "UTF-8");

      BasicDBObject createUserCommand = new BasicDBObject("createUser", username)
          .append("pwd", password)
          .append("roles",
              Collections.singletonList(
                  new BasicDBObject("role", "readWrite")
                      .append("db", AUTHENTICATION_DB)));

      MongoDatabase db = mongoClient.getDatabase(AUTHENTICATION_DB);
      db.runCommand(createUserCommand);
    }
  }

  @AfterClass
  public static void tearDownCluster() {
    synchronized (MongoTestSuite.class) {
      if (initCount.decrementAndGet() == 0) {
        try {
          if (mongoClient != null) {
            mongoClient.getDatabase(EMPLOYEE_DB).drop();
            mongoClient.getDatabase(DATATYPE_DB).drop();
            mongoClient.getDatabase(DONUTS_DB).drop();
          }
        } finally {
          if (mongoClient != null) {
            mongoClient.close();
          }
          containerManager.cleanup();
        }
      }
    }
  }
}
