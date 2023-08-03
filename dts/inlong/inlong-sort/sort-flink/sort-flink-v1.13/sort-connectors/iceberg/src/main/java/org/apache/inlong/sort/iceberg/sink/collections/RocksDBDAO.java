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

package org.apache.inlong.sort.iceberg.sink.collections;

import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.memory.DataInputDeserializer;
import org.apache.flink.core.memory.DataOutputSerializer;
import org.apache.flink.shaded.guava18.com.google.common.base.Preconditions;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.rocksdb.AbstractImmutableNativeReference;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.DBOptions;
import org.rocksdb.InfoLogLevel;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Statistics;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Data access objects for storing and retrieving objects in Rocks DB.
 */
public class RocksDBDAO<K, V> {

    private static final Logger LOG = LogManager.getLogger(RocksDBDAO.class);

    private transient ConcurrentHashMap<String, ColumnFamilyHandle> managedHandlesMap;
    private transient ConcurrentHashMap<String, ColumnFamilyDescriptor> managedDescriptorMap;
    private transient RocksDB rocksDB;
    private boolean closed = false;
    private final String rocksDBBasePath;
    private long totalBytesWritten;
    private final TypeSerializer<K> keySerializer;
    private final TypeSerializer<V> valueSerializer;
    private final DataInputDeserializer inputBuffer;
    private final DataOutputSerializer outputBuffer;

    public RocksDBDAO(
            String basePath,
            String rocksDBBasePath,
            TypeSerializer<K> keySerializer,
            TypeSerializer<V> valueSerializer) {
        this.rocksDBBasePath =
                String.format("%s/%s/%s", rocksDBBasePath, basePath.replace("/", "_"), UUID.randomUUID());
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.inputBuffer = new DataInputDeserializer();
        this.outputBuffer = new DataOutputSerializer(4096);
        init();
        totalBytesWritten = 0L;
    }

    /**
     * Create RocksDB if not initialized.
     */
    private RocksDB getRocksDB() {
        return rocksDB;
    }

    /**
     * Initialized Rocks DB instance.
     */
    private void init() {
        try {
            LOG.info("DELETING RocksDB persisted at " + rocksDBBasePath);
            FileIOUtils.deleteDirectory(new File(rocksDBBasePath));

            managedHandlesMap = new ConcurrentHashMap<>();
            managedDescriptorMap = new ConcurrentHashMap<>();

            // If already present, loads the existing column-family handles

            final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
                    .setWalDir(rocksDBBasePath).setStatsDumpPeriodSec(300).setStatistics(new Statistics());
            dbOptions.setLogger(new org.rocksdb.Logger(dbOptions) {

                @Override
                protected void log(InfoLogLevel infoLogLevel, String logMsg) {
                    LOG.info("From Rocks DB(" + rocksDBBasePath + ") : " + logMsg);
                }
            });
            final List<ColumnFamilyDescriptor> managedColumnFamilies = loadManagedColumnFamilies(dbOptions);
            final List<ColumnFamilyHandle> managedHandles = new ArrayList<>();
            FileIOUtils.mkdir(new File(rocksDBBasePath));
            rocksDB = RocksDB.open(dbOptions, rocksDBBasePath, managedColumnFamilies, managedHandles);

            Preconditions.checkArgument(managedHandles.size() == managedColumnFamilies.size(),
                    "Unexpected number of handles are returned");
            for (int index = 0; index < managedHandles.size(); index++) {
                ColumnFamilyHandle handle = managedHandles.get(index);
                ColumnFamilyDescriptor descriptor = managedColumnFamilies.get(index);
                String familyNameFromHandle = new String(handle.getName());
                String familyNameFromDescriptor = new String(descriptor.getName());

                Preconditions.checkArgument(familyNameFromDescriptor.equals(familyNameFromHandle),
                        "Family Handles not in order with descriptors");
                managedHandlesMap.put(familyNameFromHandle, handle);
                managedDescriptorMap.put(familyNameFromDescriptor, descriptor);
            }
        } catch (RocksDBException | IOException re) {
            LOG.error("Got exception opening Rocks DB instance ", re);
            throw new RuntimeException(re);
        }
    }

    /**
     * Helper to load managed column family descriptors.
     */
    private List<ColumnFamilyDescriptor> loadManagedColumnFamilies(DBOptions dbOptions) throws RocksDBException {
        final List<ColumnFamilyDescriptor> managedColumnFamilies = new ArrayList<>();
        final Options options = new Options(dbOptions, new ColumnFamilyOptions());
        List<byte[]> existing = RocksDB.listColumnFamilies(options, rocksDBBasePath);

        if (existing.isEmpty()) {
            LOG.info("No column family found. Loading default");
            managedColumnFamilies.add(getColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
        } else {
            LOG.info("Loading column families :" + existing.stream().map(String::new).collect(Collectors.toList()));
            managedColumnFamilies
                    .addAll(existing.stream().map(RocksDBDAO::getColumnFamilyDescriptor).collect(Collectors.toList()));
        }
        return managedColumnFamilies;
    }

    private static ColumnFamilyDescriptor getColumnFamilyDescriptor(byte[] columnFamilyName) {
        return new ColumnFamilyDescriptor(columnFamilyName, new ColumnFamilyOptions());
    }

    /**
     * Perform a batch write operation.
     */
    public void writeBatch(BatchHandler handler) {
        try (WriteBatch batch = new WriteBatch()) {
            handler.apply(batch);
            getRocksDB().write(new WriteOptions(), batch);
        } catch (RocksDBException re) {
            throw new RuntimeException(re);
        }
    }

    /**
     * Helper to add put operation in batch.
     *
     * @param batch Batch Handle
     * @param columnFamilyName Column Family
     * @param key Key
     * @param value Payload
     */
    public void putInBatch(WriteBatch batch, String columnFamilyName, String key, V value) {
        try {
            byte[] payload = serializeValue(value);
            batch.put(managedHandlesMap.get(columnFamilyName), key.getBytes(), payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to add put operation in batch.
     *
     * @param batch Batch Handle
     * @param columnFamilyName Column Family
     * @param key Key
     * @param value Payload
     */
    public void putInBatch(WriteBatch batch, String columnFamilyName, K key, V value) {
        try {
            byte[] keyBytes = serializeKey(key);
            byte[] payload = serializeValue(value);
            batch.put(managedHandlesMap.get(columnFamilyName), keyBytes, payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform single PUT on a column-family.
     *
     * @param columnFamilyName Column family name
     * @param key Key
     * @param value Payload
     */
    public void put(String columnFamilyName, String key, V value) {
        try {
            byte[] payload = serializeValue(value);
            getRocksDB().put(managedHandlesMap.get(columnFamilyName), key.getBytes(), payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform single PUT on a column-family.
     *
     * @param columnFamilyName Column family name
     * @param key Key
     * @param value Payload
     */
    public void put(String columnFamilyName, K key, V value) {
        try {
            byte[] payload = serializeValue(value);
            getRocksDB().put(managedHandlesMap.get(columnFamilyName), serializeKey(key), payload);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform a single Delete operation.
     *
     * @param columnFamilyName Column Family name
     * @param key Key to be deleted
     */
    public void delete(String columnFamilyName, String key) {
        try {
            getRocksDB().delete(managedHandlesMap.get(columnFamilyName), key.getBytes());
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform a single Delete operation.
     *
     * @param columnFamilyName Column Family name
     * @param key Key to be deleted
     */
    public void delete(String columnFamilyName, K key) {
        try {
            getRocksDB().delete(managedHandlesMap.get(columnFamilyName), serializeKey(key));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve a value for a given key in a column family.
     *
     * @param columnFamilyName Column Family Name
     * @param key Key to be retrieved
     */
    public V get(String columnFamilyName, String key) {
        Preconditions.checkArgument(!closed);
        try {
            byte[] val = getRocksDB().get(managedHandlesMap.get(columnFamilyName), key.getBytes());
            return val == null ? null : deserializeValue(val);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve a value for a given key in a column family.
     *
     * @param columnFamilyName Column Family Name
     * @param key Key to be retrieved
     */
    public V get(String columnFamilyName, K key) {
        Preconditions.checkArgument(!closed);
        try {
            byte[] val = getRocksDB().get(managedHandlesMap.get(columnFamilyName), serializeKey(key));
            return val == null ? null : deserializeValue(val);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Perform a prefix search and return stream of key-value pairs retrieved.
     * @note This stream must be closed after use, otherwise it will cause a memory leak
     *
     * @param columnFamilyName Column Family Name
     * @param prefix Prefix Key
     */
    public Stream<Tuple2<K, V>> prefixSearch(String columnFamilyName, byte[] prefix) {
        Preconditions.checkArgument(!closed);
        final RocksIterator it = getRocksDB().newIterator(managedHandlesMap.get(columnFamilyName));
        Iterator<Tuple2<K, V>> conditionalIt = new ConditionalIteratorWrapper<>(it, this, prefix);
        return StreamSupport
                .stream(Spliterators.spliteratorUnknownSize(conditionalIt, 0), false)
                .onClose(() -> it.close());
    }

    /**
     * Add a new column family to store.
     *
     * @param columnFamilyName Column family name
     */
    public void addColumnFamily(String columnFamilyName) {
        Preconditions.checkArgument(!closed);

        managedDescriptorMap.computeIfAbsent(columnFamilyName, colFamilyName -> {
            try {
                ColumnFamilyDescriptor descriptor = getColumnFamilyDescriptor(colFamilyName.getBytes());
                ColumnFamilyHandle handle = getRocksDB().createColumnFamily(descriptor);
                managedHandlesMap.put(colFamilyName, handle);
                return descriptor;
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Note : Does not delete from underlying DB. Just closes the handle.
     *
     * @param columnFamilyName Column Family Name
     */
    public void dropColumnFamily(String columnFamilyName) {
        Preconditions.checkArgument(!closed);

        managedDescriptorMap.computeIfPresent(columnFamilyName, (colFamilyName, descriptor) -> {
            ColumnFamilyHandle handle = managedHandlesMap.get(colFamilyName);
            try {
                getRocksDB().dropColumnFamily(handle);
                handle.close();
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
            managedHandlesMap.remove(columnFamilyName);
            return null;
        });
    }

    /**
     * Close the DAO object.
     */
    public synchronized void close() {
        if (!closed) {
            closed = true;
            managedHandlesMap.values().forEach(AbstractImmutableNativeReference::close);
            managedHandlesMap.clear();
            managedDescriptorMap.clear();
            getRocksDB().close();
            try {
                FileIOUtils.deleteDirectory(new File(rocksDBBasePath));
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }

    public long getTotalBytesWritten() {
        return totalBytesWritten;
    }

    private byte[] serializeKey(K key) throws IOException {
        keySerializer.serialize(key, outputBuffer);
        byte[] keyBytes = outputBuffer.getCopyOfBuffer();
        outputBuffer.clear();
        return keyBytes;
    }

    private byte[] serializeValue(V value) throws IOException {
        valueSerializer.serialize(value, outputBuffer);
        byte[] payload = outputBuffer.getCopyOfBuffer();
        outputBuffer.clear();

        totalBytesWritten += payload.length;
        return payload;
    }

    private K deserializeKey(byte[] buffer) throws IOException {
        inputBuffer.setBuffer(buffer);
        return keySerializer.deserialize(inputBuffer);
    }

    private V deserializeValue(byte[] buffer) throws IOException {
        inputBuffer.setBuffer(buffer);
        return valueSerializer.deserialize(inputBuffer);
    }

    String getRocksDBBasePath() {
        return rocksDBBasePath;
    }

    private static boolean prefixMatch(byte[] key, byte[] prefix) {
        int i = 0;
        while (i < prefix.length && i < key.length) {
            if (prefix[i] != key[i]) {
                return false;
            }
            i++;
        }
        return i == prefix.length;
    }

    /**
     * {@link Iterator} wrapper for RocksDb Iterator {@link RocksIterator}.
     */
    private static class IteratorWrapper<T, R> implements Iterator<Tuple2<T, R>> {

        protected final RocksIterator iterator;
        protected final RocksDBDAO<T, R> dao;

        public IteratorWrapper(final RocksIterator iterator, final RocksDBDAO<T, R> dao) {
            this.iterator = iterator;
            this.dao = dao;
            iterator.seekToFirst();
        }

        @Override
        public boolean hasNext() {
            return iterator.isValid();
        }

        @Override
        public Tuple2<T, R> next() {
            if (!hasNext()) {
                throw new IllegalStateException("next() called on rocksDB with no more valid entries");
            }
            T key = null;
            R val = null;
            try {
                key = dao.deserializeKey(iterator.key());
                val = dao.deserializeValue(iterator.value());
            } catch (IOException e) {
                throw new IllegalStateException("deserialize err", e);
            }
            iterator.next();
            return new Tuple2<>(key, val);
        }

        public void close() {
            iterator.close();
        }
    }

    private static class ConditionalIteratorWrapper<T, R> extends IteratorWrapper<T, R> {

        private byte[] keyPrefix;

        public ConditionalIteratorWrapper(final RocksIterator iterator, final RocksDBDAO<T, R> dao, byte[] keyPrefix) {
            super(iterator, dao);
            this.keyPrefix = keyPrefix;
            iterator.seek(keyPrefix);
        }

        @Override
        public boolean hasNext() {
            return iterator.isValid() && prefixMatch(iterator.key(), keyPrefix);
        }
    }

    /**
     * Functional interface for stacking operation to Write batch.
     */
    public interface BatchHandler {

        void apply(WriteBatch batch);
    }
}
