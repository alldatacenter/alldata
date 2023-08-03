/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.utils.map;

import com.netease.arctic.ArcticIOException;
import com.netease.arctic.utils.LocalFileUtil;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.commons.lang.Validate;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RocksDBBackend {
  private static final Logger LOG = LoggerFactory.getLogger(RocksDBBackend.class);
  private static final String BACKEND_BASE_DIR = System.getProperty("java.io.tmpdir");
  private static final ThreadLocal<RocksDBBackend> instance =
          new ThreadLocal<>();

  public static RocksDBBackend getOrCreateInstance() {
    RocksDBBackend backend = instance.get();
    if (backend == null) {
      backend = create(BACKEND_BASE_DIR);
      instance.set(backend);
    }
    if (backend.closed) {
      backend = create(BACKEND_BASE_DIR);
      instance.set(backend);
    }
    return backend;
  }

  public static RocksDBBackend getOrCreateInstance(@Nullable String backendBaseDir) {
    if (backendBaseDir == null) {
      return getOrCreateInstance();
    }
    RocksDBBackend backend = instance.get();
    if (backend == null) {
      backend = create(backendBaseDir);
      instance.set(backend);
    }
    if (backend.closed) {
      backend = create(backendBaseDir);
      instance.set(backend);
    }
    return backend;
  }

  private Map<String, ColumnFamilyHandle> handlesMap = new HashMap<>();
  private Map<String, ColumnFamilyDescriptor> descriptorMap = new HashMap<>();
  private RocksDB rocksDB;
  private boolean closed = false;
  private final String rocksDBBasePath;
  private long totalBytesWritten;

  private static RocksDBBackend create(@Nullable String backendBaseDir) {
    return new RocksDBBackend(backendBaseDir);
  }

  private RocksDBBackend(@Nullable String backendBaseDir) {
    this.rocksDBBasePath = backendBaseDir == null ? UUID.randomUUID().toString() :
        String.format("%s/%s", backendBaseDir, UUID.randomUUID());
    totalBytesWritten = 0L;
    setup();
  }

  /**
   * Initialized Rocks DB instance.
   */
  private void setup() {
    try {
      LOG.info("DELETING RocksDB instance persisted at " + rocksDBBasePath);
      LocalFileUtil.deleteDirectory(new File(rocksDBBasePath));

      final DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)
              .setWalDir(rocksDBBasePath).setStatsDumpPeriodSec(300).setStatistics(new Statistics());
      dbOptions.setLogger(new org.rocksdb.Logger(dbOptions) {
        @Override
        protected void log(InfoLogLevel infoLogLevel, String logMsg) {
          LOG.info("From Rocks DB : " + logMsg);
        }
      });
      final List<ColumnFamilyDescriptor> managedColumnFamilies = loadManagedColumnFamilies(dbOptions);
      final List<ColumnFamilyHandle> managedHandles = new ArrayList<>();
      LocalFileUtil.mkdir(new File(rocksDBBasePath));
      rocksDB = RocksDB.open(dbOptions, rocksDBBasePath, managedColumnFamilies, managedHandles);

      Validate.isTrue(managedHandles.size() == managedColumnFamilies.size(),
              "Unexpected number of handles are returned");
      for (int index = 0; index < managedHandles.size(); index++) {
        ColumnFamilyHandle handle = managedHandles.get(index);
        ColumnFamilyDescriptor descriptor = managedColumnFamilies.get(index);
        String familyNameFromHandle = new String(handle.getName());
        String familyNameFromDescriptor = new String(descriptor.getName());

        Validate.isTrue(familyNameFromDescriptor.equals(familyNameFromHandle),
                "Family Handles not in order with descriptors");
        handlesMap.put(familyNameFromHandle, handle);
        descriptorMap.put(familyNameFromDescriptor, descriptor);
      }
      addShutDownHook();
    } catch (RocksDBException | IOException re) {
      LOG.error("Got exception opening Rocks DB instance ", re);
      if (rocksDB != null) {
        close();
      }
      throw new ArcticIOException(re);
    }
  }

  private void addShutDownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));
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
              .addAll(existing.stream().map(this::getColumnFamilyDescriptor).collect(Collectors.toList()));
    }
    return managedColumnFamilies;
  }

  private ColumnFamilyDescriptor getColumnFamilyDescriptor(byte[] columnFamilyName) {
    return new ColumnFamilyDescriptor(columnFamilyName, new ColumnFamilyOptions());
  }

  /**
   * Perform single PUT on a column-family.
   *
   * @param columnFamilyName Column family name
   * @param key Key
   * @param value Payload
   */
  @VisibleForTesting
  public <K extends Serializable, T extends Serializable> void put(String columnFamilyName, K key, T value) {
    try {
      Validate.isTrue(key != null && value != null,
              "values or keys in rocksdb can not be null!");
      byte[] payload = serializePayload(value);
      rocksDB.put(handlesMap.get(columnFamilyName), SerializationUtil.kryoSerialize(key), payload);
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Perform single PUT on a column-family.
   *
   * @param columnFamilyName Column family name
   * @param key Key
   * @param value Payload
   */
  public void put(String columnFamilyName, byte[] key, byte[] value) {
    try {
      Validate.isTrue(key != null && value != null,
          "values or keys in rocksdb can not be null!");
      ColumnFamilyHandle cfHandler = handlesMap.get(columnFamilyName);
      Validate.isTrue(cfHandler != null, "column family " +
          columnFamilyName + " does not exists in rocksdb");
      rocksDB.put(cfHandler, key, payload(value));
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Perform a single Delete operation.
   *
   * @param columnFamilyName Column Family name
   * @param key Key to be deleted
   */
  @VisibleForTesting
  public <K extends Serializable> void delete(String columnFamilyName, K key) {
    try {
      Validate.isTrue(key != null, "keys in rocksdb can not be null!");
      rocksDB.delete(handlesMap.get(columnFamilyName), SerializationUtil.kryoSerialize(key));
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Perform a single Delete operation.
   *
   * @param columnFamilyName Column Family name
   * @param key Key to be deleted
   */
  public void delete(String columnFamilyName, byte[] key) {
    try {
      Validate.isTrue(key != null, "keys in rocksdb can not be null!");
      rocksDB.delete(handlesMap.get(columnFamilyName), key);
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Retrieve a value for a given key in a column family.
   *
   * @param columnFamilyName Column Family Name
   * @param key Key to be retrieved
   */
  @VisibleForTesting
  public <K extends Serializable, T extends Serializable> T get(String columnFamilyName, K key) {
    Validate.isTrue(!closed);
    try {
      Validate.isTrue(key != null, "keys in rocksdb can not be null!");
      byte[] val = rocksDB.get(handlesMap.get(columnFamilyName), SerializationUtil.kryoSerialize(key));
      return val == null ? null : SerializationUtil.kryoDeserialize(val);
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Retrieve a value for a given key in a column family.
   *
   * @param columnFamilyName Column Family Name
   * @param key Key to be retrieved
   */
  public byte[] get(String columnFamilyName, byte[] key) {
    Validate.isTrue(!closed);
    try {
      Validate.isTrue(key != null, "keys in rocksdb can not be null!");
      byte[] val = rocksDB.get(handlesMap.get(columnFamilyName), key);
      return val;
    } catch (Exception e) {
      throw new ArcticIOException(e);
    }
  }

  /**
   * Return Iterator of key-value pairs from RocksIterator.
   *
   * @param columnFamilyName Column Family Name
   */
  @VisibleForTesting
  public <T extends Serializable> Iterator<T> valuesForTest(String columnFamilyName) {
    return new ValueIteratorForTest<>(rocksDB.newIterator(handlesMap.get(columnFamilyName)));
  }

  /**
   * Return Iterator of key-value pairs from RocksIterator.
   *
   * @param columnFamilyName Column Family Name
   */
  public  Iterator<byte[]> values(String columnFamilyName) {
    return new ValueIterator(rocksDB.newIterator(handlesMap.get(columnFamilyName)));
  }

  /**
   * Add a new column family to store.
   *
   * @param columnFamilyName Column family name
   */
  public void addColumnFamily(String columnFamilyName) {
    Validate.isTrue(!closed);

    descriptorMap.computeIfAbsent(columnFamilyName, colFamilyName -> {
      try {
        ColumnFamilyDescriptor descriptor = getColumnFamilyDescriptor(colFamilyName.getBytes());
        ColumnFamilyHandle handle = rocksDB.createColumnFamily(descriptor);
        handlesMap.put(colFamilyName, handle);
        return descriptor;
      } catch (RocksDBException e) {
        throw new ArcticIOException(e);
      }
    });
  }

  /**
   * Note : Does not delete from underlying DB. Just closes the handle.
   *
   * @param columnFamilyName Column Family Name
   */
  public void dropColumnFamily(String columnFamilyName) {
    Validate.isTrue(!closed);

    descriptorMap.computeIfPresent(columnFamilyName, (colFamilyName, descriptor) -> {
      ColumnFamilyHandle handle = handlesMap.get(colFamilyName);
      try {
        rocksDB.dropColumnFamily(handle);
        handle.close();
      } catch (RocksDBException e) {
        throw new ArcticIOException(e);
      }
      handlesMap.remove(columnFamilyName);
      return null;
    });
  }

  public List<ColumnFamilyDescriptor> listColumnFamilies() {
    return new ArrayList<>(descriptorMap.values());
  }

  /**
   * Close the DAO object.
   */
  public void close() {
    if (!closed) {
      closed = true;
      handlesMap.values().forEach(AbstractImmutableNativeReference::close);
      handlesMap.clear();
      descriptorMap.clear();
      rocksDB.close();
      try {
        LocalFileUtil.deleteDirectory(new File(rocksDBBasePath));
      } catch (IOException e) {
        throw new ArcticIOException(e.getMessage(), e);
      }
    }
  }

  public String getRocksDBBasePath() {
    return rocksDBBasePath;
  }

  public long getTotalBytesWritten() {
    return totalBytesWritten;
  }

  private byte[] serializePayload(Object value) throws IOException {
    byte[] payload = SerializationUtil.kryoSerialize(value);
    totalBytesWritten += payload.length;
    return payload;
  }

  private byte[] payload(byte[] value) {
    totalBytesWritten += value.length;
    return value;
  }

  /**
   * {@link Iterator} wrapper for RocksDb Iterator {@link RocksIterator}.
   */
  private static class ValueIteratorForTest<R> implements Iterator<R> {

    private final RocksIterator iterator;

    public ValueIteratorForTest(final RocksIterator iterator) {
      this.iterator = iterator;
      iterator.seekToFirst();
    }

    @Override
    public boolean hasNext() {
      return iterator.isValid();
    }

    @Override
    public R next() {
      if (!hasNext()) {
        throw new IllegalStateException("next() called on rocksDB with no more valid entries");
      }
      R val = SerializationUtil.kryoDeserialize(iterator.value());
      iterator.next();
      return val;
    }
  }

  /**
   * {@link Iterator} wrapper for RocksDb Iterator {@link RocksIterator}.
   */
  private static class ValueIterator implements Iterator<byte[]> {

    private final RocksIterator iterator;

    public ValueIterator(final RocksIterator iterator) {
      this.iterator = iterator;
      iterator.seekToFirst();
    }

    @Override
    public boolean hasNext() {
      return iterator.isValid();
    }

    @Override
    public byte[] next() {
      if (!hasNext()) {
        throw new IllegalStateException("next() called on rocksDB with no more valid entries");
      }
      byte[] val = iterator.value();
      iterator.next();
      return val;
    }
  }
}
