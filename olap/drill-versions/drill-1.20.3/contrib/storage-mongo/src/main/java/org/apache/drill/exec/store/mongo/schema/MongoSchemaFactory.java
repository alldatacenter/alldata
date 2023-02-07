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
package org.apache.drill.exec.store.mongo.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.mongo.MongoScanSpec;
import org.apache.drill.exec.store.mongo.MongoStoragePlugin;
import org.apache.drill.exec.store.mongo.MongoStoragePluginConfig;
import org.apache.drill.exec.store.plan.rel.PluginDrillTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import com.mongodb.MongoException;
import com.mongodb.client.MongoDatabase;

public class MongoSchemaFactory extends AbstractSchemaFactory {

  private static final Logger logger = LoggerFactory.getLogger(MongoSchemaFactory.class);

  private static final String DATABASES = "databases";

  private final LoadingCache<String, List<String>> databases;
  private final LoadingCache<String, List<String>> tableNameLoader;
  private final Map<String, String> schemaNameMap;
  private final MongoStoragePlugin plugin;

  public MongoSchemaFactory(MongoStoragePlugin plugin, String schemaName) {
    super(schemaName);
    this.plugin = plugin;
    this.schemaNameMap = new HashMap<>();

    databases = CacheBuilder //
        .newBuilder() //
        .expireAfterAccess(1, TimeUnit.MINUTES) //
        .build(new DatabaseLoader());

    tableNameLoader = CacheBuilder //
        .newBuilder() //
        .expireAfterAccess(1, TimeUnit.MINUTES) //
        .build(new TableNameLoader());
  }

  private class DatabaseLoader extends CacheLoader<String, List<String>> {

    @Override
    public List<String> load(String key) {
      if (!DATABASES.equals(key)) {
        throw new UnsupportedOperationException();
      }
      try {
        List<String> dbNames = new ArrayList<>();
        plugin.getClient().listDatabaseNames().forEach(name -> {
          // 1. Schemas in drill are case insensitive and stored in lower case.
          dbNames.add(name.toLowerCase());
          /*
           * 2. Support database name with capital letters.
           * case 1: "show tables from mongo.HELLO", Should using the lower case name
           *  to resolve the schema lookup in `CalciteSchema`.
           * case 2: "select * from mongo.HEllO.myTable", Must be using origin name
           *  to create `MongoScanSpec` and initial connection in `MongoRecordReader`.
           */
          schemaNameMap.put(name.toLowerCase(), name);
        });
        return dbNames;
      } catch (MongoException me) {
        logger.warn("Failure while loading databases in Mongo. {}",
            me.getMessage());
        return Collections.emptyList();
      } catch (Exception e) {
        throw new DrillRuntimeException(e.getMessage(), e);
      }
    }

  }

  private class TableNameLoader extends CacheLoader<String, List<String>> {

    @Override
    public List<String> load(String dbName) {
      try {
        MongoDatabase db = plugin.getClient().getDatabase(schemaNameMap.get(dbName));
        List<String> collectionNames = new ArrayList<>();
        db.listCollectionNames().into(collectionNames);
        return collectionNames;
      } catch (MongoException me) {
        logger.warn("Failure while getting collection names from '{}'. {}",
            dbName, me.getMessage());
        return Collections.emptyList();
      } catch (Exception e) {
        throw new DrillRuntimeException(e.getMessage(), e);
      }
    }
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) {
    MongoSchema schema = new MongoSchema(getName());
    SchemaPlus hPlus = parent.add(getName(), schema);
    schema.setHolder(hPlus);
  }

  class MongoSchema extends AbstractSchema {

    private final Map<String, MongoDatabaseSchema> schemaMap = Maps.newHashMap();

    public MongoSchema(String name) {
      super(Collections.emptyList(), name);
    }

    @Override
    public AbstractSchema getSubSchema(String name) {
      List<String> tables;
      try {
        if (! schemaMap.containsKey(name)) {
          tables = tableNameLoader.get(name);
          schemaMap.put(name, new MongoDatabaseSchema(tables, this, name));
        }

        return schemaMap.get(name);

        //return new MongoDatabaseSchema(tables, this, name);
      } catch (ExecutionException e) {
        logger.warn("Failure while attempting to access MongoDataBase '{}'.",
            name, e.getCause());
        return null;
      }

    }

    void setHolder(SchemaPlus plusOfThis) {
      for (String s : getSubSchemaNames()) {
        plusOfThis.add(s, getSubSchema(s));
      }
    }

    @Override
    public boolean showInInformationSchema() {
      return false;
    }

    @Override
    public Set<String> getSubSchemaNames() {
      try {
        List<String> dbs = databases.get(DATABASES);
        return Sets.newHashSet(dbs);
      } catch (ExecutionException e) {
        logger.warn("Failure while getting Mongo database list.", e);
        return Collections.emptySet();
      }
    }

    List<String> getTableNames(String dbName) {
      try {
        return tableNameLoader.get(dbName);
      } catch (ExecutionException e) {
        logger.warn("Failure while loading table names for database '{}'.",
            dbName, e.getCause());
        return Collections.emptyList();
      }
    }

    DrillTable getDrillTable(String dbName, String collectionName) {
      MongoScanSpec mongoScanSpec = new MongoScanSpec(schemaNameMap.get(dbName), collectionName);
      return new PluginDrillTable(plugin, getName(), null, mongoScanSpec, plugin.convention());
    }

    @Override
    public String getTypeName() {
      return MongoStoragePluginConfig.NAME;
    }
  }
}
