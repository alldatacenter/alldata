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
package org.apache.drill.exec.store.hbase.config;

import static org.apache.drill.exec.store.hbase.config.HBasePersistentStoreProvider.FAMILY;
import static org.apache.drill.exec.store.hbase.config.HBasePersistentStoreProvider.QUALIFIER;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.store.sys.BasePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreMode;
import org.apache.hadoop.hbase.client.Delete;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;

import org.apache.drill.shaded.guava.com.google.common.collect.Iterators;

public class HBasePersistentStore<V> extends BasePersistentStore<V> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HBasePersistentStore.class);

  private final PersistentStoreConfig<V> config;
  private final Table hbaseTable;
  private final String hbaseTableName;

  private final String tableName;
  private final byte[] tableNameStartKey;
  private final byte[] tableNameStopKey;

  public HBasePersistentStore(PersistentStoreConfig<V> config, Table table) {
    this.tableName = config.getName() + '\0';
    this.tableNameStartKey = Bytes.toBytes(tableName); // "tableName\x00"
    this.tableNameStopKey = this.tableNameStartKey.clone();
    this.tableNameStopKey[tableNameStartKey.length-1] = 1;
    this.config = config;
    this.hbaseTable = table;
    this.hbaseTableName = table.getName().getNameAsString();
  }

  @Override
  public PersistentStoreMode getMode() {
    return PersistentStoreMode.PERSISTENT;
  }

  @Override
  public boolean contains(String key) {
    try {
      Get get = new Get(row(key));
      get.addColumn(FAMILY, QUALIFIER);
      return hbaseTable.exists(get);
    } catch (IOException e) {
      throw UserException
          .dataReadError(e)
          .message("Caught error while checking row existence '%s' for table '%s'", key, hbaseTableName)
          .build(logger);
    }
  }

  @Override
  public V get(String key) {
    return get(key, FAMILY);
  }

  protected synchronized V get(String key, byte[] family) {
    try {
      Get get = new Get(row(key));
      get.addColumn(family, QUALIFIER);
      Result r = hbaseTable.get(get);
      if(r.isEmpty()){
        return null;
      }
      return value(r);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .message("Caught error while getting row '%s' from for table '%s'", key, hbaseTableName)
          .build(logger);
    }
  }

  @Override
  public void put(String key, V value) {
    put(key, FAMILY, value);
  }

  protected synchronized void put(String key, byte[] family, V value) {
    try {
      Put put = new Put(row(key));
      put.addColumn(family, QUALIFIER, bytes(value));
      hbaseTable.put(put);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .message("Caught error while putting row '%s' into table '%s'", key, hbaseTableName)
          .build(logger);
    }
  }

  @Override
  public synchronized boolean putIfAbsent(String key, V value) {
    try {
      Put put = new Put(row(key));
      put.addColumn(FAMILY, QUALIFIER, bytes(value));
      return hbaseTable.checkAndPut(put.getRow(), FAMILY, QUALIFIER, null /*absent*/, put);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .message("Caught error while putting row '%s' into table '%s'", key, hbaseTableName)
          .build(logger);
    }
  }

  @Override
  public synchronized void delete(String key) {
    delete(row(key));
  }

  @Override
  public Iterator<Entry<String, V>> getRange(int skip, int take) {
    final Iterator<Entry<String, V>> iter = new Iter(take);
    Iterators.advance(iter, skip);
    return Iterators.limit(iter, take);
  }

  private byte[] row(String key) {
    return Bytes.toBytes(this.tableName + key);
  }

  private byte[] bytes(V value) {
    try {
      return config.getSerializer().serialize(value);
    } catch (IOException e) {
      throw UserException.dataReadError(e).build(logger);
    }
  }

  private V value(Result result) {
    try {
      return config.getSerializer().deserialize(result.value());
    } catch (IOException e) {
      throw UserException.dataReadError(e).build(logger);
    }
  }

  private void delete(byte[] row) {
    try {
      Delete del = new Delete(row);
      hbaseTable.delete(del);
    } catch (IOException e) {
      throw UserException.dataReadError(e)
          .message("Caught error while deleting row '%s' from for table '%s'", Bytes.toStringBinary(row), hbaseTableName)
          .build(logger);
    }
  }

  private class Iter implements Iterator<Entry<String, V>> {
    private ResultScanner scanner;
    private Result current = null;
    private Result last = null;
    private boolean done = false;

    Iter(int take) {
      try {
        Scan scan = new Scan(tableNameStartKey, tableNameStopKey);
        scan.addColumn(FAMILY, QUALIFIER);
        scan.setCaching(Math.min(take, 100));
        scan.setBatch(take);  // set batch size
        scanner = hbaseTable.getScanner(scan);
      } catch (IOException e) {
        throw UserException.dataReadError(e)
            .message("Caught error while creating HBase scanner for table '%s'" + hbaseTableName)
            .build(logger);
      }
    }

    @Override
    public boolean hasNext()  {
      if (!done && current == null) {
        try {
          if ((current = scanner.next()) == null) {
            done = true;
          }
        } catch (IOException e) {
          throw UserException.dataReadError(e)
              .message("Caught error while fetching rows from for table '%s'", hbaseTableName)
              .build(logger);
        }
      }

      if (done && scanner != null) {
        scanner.close();
      }
      return (current != null);
    }

    @Override
    public Entry<String, V> next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      last = current;
      current = null;
      return new DeferredEntry(last);
    }

    @Override
    public void remove() {
      if (last == null) {
        throw new IllegalStateException("remove() called on HBase persistent store iterator, but there is no last row.");
      }
      delete(last.getRow());
    }

  }

  private class DeferredEntry implements Entry<String, V>{

    private Result result;

    public DeferredEntry(Result result) {
      super();
      this.result = result;
    }

    @Override
    public String getKey() {
      return Bytes.toString(result.getRow(), tableNameStartKey.length, result.getRow().length-tableNameStartKey.length);
    }

    @Override
    public V getValue() {
      return value(result);
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

  }

  @Override
  public void close() {
  }

}
