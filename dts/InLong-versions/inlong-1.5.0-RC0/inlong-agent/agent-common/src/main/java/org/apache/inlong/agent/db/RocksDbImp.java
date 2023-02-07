/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.db;

import com.google.gson.Gson;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.common.db.CommandEntity;
import org.rocksdb.AbstractImmutableNativeReference;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * DB implement based on the Rocks DB.
 */
public class RocksDbImp implements Db {

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDbImp.class);
    private static final Gson GSON = new Gson();

    private final AgentConfiguration conf;
    private final RocksDB db;
    private final String commandFamilyName = "command";
    private final String defaultFamilyName = "default";
    private ConcurrentHashMap<String, ColumnFamilyHandle> columnHandlesMap;
    private ConcurrentHashMap<String, ColumnFamilyDescriptor> columnDescriptorMap;
    private String storePath;

    public RocksDbImp() {
        // init rocks db
        this.conf = AgentConfiguration.getAgentConf();
        this.db = initEnv();
        // add a command column family
        addColumnFamily(commandFamilyName);
    }

    private static ColumnFamilyDescriptor getColumnFamilyDescriptor(byte[] columnFamilyName) {
        return new ColumnFamilyDescriptor(columnFamilyName, new ColumnFamilyOptions());
    }

    private RocksDB initEnv() {
        String configPath = conf.get(AgentConstants.AGENT_ROCKS_DB_PATH, AgentConstants.DEFAULT_AGENT_ROCKS_DB_PATH);
        String parentPath = conf.get(AgentConstants.AGENT_HOME, AgentConstants.DEFAULT_AGENT_HOME);
        File finalPath = new File(parentPath, configPath);
        storePath = finalPath.getAbsolutePath();
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        try {
            boolean result = finalPath.mkdirs();
            LOGGER.info("create directory {}, result is {}", finalPath, result);

            columnHandlesMap = new ConcurrentHashMap<>();
            columnDescriptorMap = new ConcurrentHashMap<>();

            final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
                    .setWalDir(finalPath.getAbsolutePath()).setStatistics(new Statistics());

            final List<ColumnFamilyDescriptor> managedColumnFamilies = loadManagedColumnFamilies(dbOptions);
            final List<ColumnFamilyHandle> managedHandles = new ArrayList<>();

            RocksDB rocksDB = RocksDB.open(dbOptions,
                    finalPath.getAbsolutePath(), managedColumnFamilies, managedHandles);

            for (int index = 0; index < managedHandles.size(); index++) {
                ColumnFamilyHandle handle = managedHandles.get(index);
                ColumnFamilyDescriptor descriptor = managedColumnFamilies.get(index);
                String familyNameFromHandle = new String(handle.getName());
                String familyNameFromDescriptor = new String(descriptor.getName());

                columnHandlesMap.put(familyNameFromHandle, handle);
                columnDescriptorMap.put(familyNameFromDescriptor, descriptor);
            }
            return rocksDB;
        } catch (Exception ex) {
            // db is vital.
            LOGGER.error("init rocksdb error, please check", ex);
            throw new RuntimeException(ex);
        }
    }

    private List<ColumnFamilyDescriptor> loadManagedColumnFamilies(DBOptions dbOptions) throws RocksDBException {
        final List<ColumnFamilyDescriptor> managedColumnFamilies = new ArrayList<>();
        final Options options = new Options(dbOptions, new ColumnFamilyOptions());
        List<byte[]> existing = RocksDB.listColumnFamilies(options, storePath);

        if (existing.isEmpty()) {
            LOGGER.info("no previous column family found, use default");
            managedColumnFamilies.add(getColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
        } else {
            LOGGER.info("loading column families :" + existing.stream().map(String::new).collect(Collectors.toList()));
            managedColumnFamilies.addAll(
                    existing.stream().map(RocksDbImp::getColumnFamilyDescriptor).collect(Collectors.toList()));
        }
        return managedColumnFamilies;
    }

    /**
     * add columnFamilyName
     */
    public void addColumnFamily(String columnFamilyName) {
        columnDescriptorMap.computeIfAbsent(columnFamilyName, colFamilyName -> {
            try {
                ColumnFamilyDescriptor descriptor = getColumnFamilyDescriptor(colFamilyName.getBytes());
                ColumnFamilyHandle handle = db.createColumnFamily(descriptor);
                columnHandlesMap.put(colFamilyName, handle);
                return descriptor;
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public KeyValueEntity get(String key) {
        requireNonNull(key);
        try {
            byte[] bytes = db.get(columnHandlesMap.get(defaultFamilyName), key.getBytes());
            return bytes == null ? null : GSON.fromJson(new String(bytes), KeyValueEntity.class);
        } catch (Exception e) {
            throw new RuntimeException("get key value entity error", e);
        }
    }

    @Override
    public CommandEntity getCommand(String commandId) {
        try {
            byte[] bytes = db.get(columnHandlesMap.get(commandFamilyName), commandId.getBytes());
            return bytes == null ? null : GSON.fromJson(new String(bytes), CommandEntity.class);
        } catch (Exception e) {
            throw new RuntimeException("get command value error", e);
        }
    }

    @Override
    public CommandEntity putCommand(CommandEntity entity) {
        requireNonNull(entity);
        try {
            db.put(columnHandlesMap.get(commandFamilyName), entity.getId().getBytes(), GSON.toJson(entity).getBytes());
        } catch (Exception e) {
            throw new RuntimeException("put value to rocks db error", e);
        }
        return entity;
    }

    @Override
    public void set(KeyValueEntity entity) {
        requireNonNull(entity);
        put(entity);
    }

    @Override
    public KeyValueEntity put(KeyValueEntity entity) {
        requireNonNull(entity);
        try {
            db.put(columnHandlesMap.get(defaultFamilyName), entity.getKey().getBytes(), GSON.toJson(entity).getBytes());
        } catch (Exception e) {
            throw new RuntimeException("put value to rocks db error", e);
        }
        return entity;
    }

    @Override
    public KeyValueEntity remove(String key) {
        requireNonNull(key);
        KeyValueEntity keyValueEntity = get(key);
        if (keyValueEntity == null) {
            LOGGER.warn("no key {} exist in rocksdb", key);
            return null;
        }
        try {
            db.delete(columnHandlesMap.get(defaultFamilyName), key.getBytes());
            return keyValueEntity;
        } catch (Exception e) {
            throw new RuntimeException("remove value from rocks db error", e);
        }
    }

    @Override
    public List<KeyValueEntity> searchWithKeyPrefix(StateSearchKey searchKey, String keyPrefix) {
        List<KeyValueEntity> results = new LinkedList<>();
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (keyValue.getStateSearchKey().equals(searchKey) && keyValue.getKey().startsWith(keyPrefix)) {
                    results.add(keyValue);
                }
                it.next();
            }
        }
        return results;
    }

    @Override
    public List<KeyValueEntity> search(StateSearchKey searchKey) {
        List<KeyValueEntity> results = new LinkedList<>();
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (keyValue.getStateSearchKey().equals(searchKey)) {
                    results.add(keyValue);
                }
                it.next();
            }
        }
        return results;
    }

    @Override
    public List<KeyValueEntity> search(List<StateSearchKey> searchKeys) {
        List<KeyValueEntity> results = new LinkedList<>();
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (Objects.nonNull(keyValue) && searchKeys.contains(keyValue.getStateSearchKey())) {
                    results.add(keyValue);
                }
                it.next();
            }
        }
        return results;
    }

    @Override
    public List<CommandEntity> searchCommands(boolean isAcked) {
        List<CommandEntity> results = new LinkedList<>();
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(commandFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                CommandEntity commandEntity = GSON.fromJson(new String(it.value()), CommandEntity.class);
                if (commandEntity.isAcked() == isAcked) {
                    results.add(commandEntity);
                }
                it.next();
            }
        }
        return results;
    }

    @Override
    public KeyValueEntity searchOne(StateSearchKey searchKey) {
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (keyValue.getStateSearchKey().equals(searchKey)) {
                    return keyValue;
                }
                it.next();
            }
        }
        return null;
    }

    @Override
    public KeyValueEntity searchOne(String fileName) {
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (keyValue.getFileName().equals(fileName)) {
                    return keyValue;
                }
                it.next();
            }
        }
        return null;
    }

    @Override
    public List<KeyValueEntity> findAll(String prefix) {
        List<KeyValueEntity> results = new LinkedList<>();
        try (final RocksIterator it = db.newIterator(columnHandlesMap.get(defaultFamilyName))) {
            it.seekToFirst();
            while (it.isValid()) {
                KeyValueEntity keyValue = GSON.fromJson(new String(it.value()), KeyValueEntity.class);
                if (keyValue.getKey().startsWith(prefix)) {
                    results.add(keyValue);
                }
                it.next();
            }
        }
        return results;
    }

    @Override
    public void close() throws IOException {
        db.close();
        columnHandlesMap.values().forEach(AbstractImmutableNativeReference::close);
        columnHandlesMap.clear();
        columnDescriptorMap.clear();
    }

}
