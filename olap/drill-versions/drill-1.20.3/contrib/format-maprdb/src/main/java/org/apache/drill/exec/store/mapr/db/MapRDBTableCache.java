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
package org.apache.drill.exec.store.mapr.db;

import com.mapr.db.Table;
import com.mapr.db.impl.MapRDBImpl;
import com.mapr.db.index.IndexDesc;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.TimeUnit;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalListener;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapRDBTableCache {

  private static final Logger logger = LoggerFactory.getLogger(MapRDBTableCache.class);

  public static final String FORMAT_MAPRDB_JSON_TABLE_CACHE_ENABLED = "format-maprdb.json.tableCache.enabled";

  public static final String FORMAT_MAPRDB_JSON_TABLE_CACHE_SIZE = "format-maprdb.json.tableCache.size";

  public static final String FORMAT_MAPRDB_JSON_TABLE_CACHE_TIMEOUT = "format-maprdb.json.tableCache.expireTimeInMinutes";

  private static final int MIN_TABLE_CACHE_SIZE = 1;

  private static final int MIN_TABLE_CACHE_ENTRY_TIMEOUT = 10;

  LoadingCache<MapRDBTableCache.Key, Table> tableCache;

  private final boolean tableCachingEnabled;

  public MapRDBTableCache(DrillConfig config) {
    tableCachingEnabled = config.getBoolean(FORMAT_MAPRDB_JSON_TABLE_CACHE_ENABLED);
    if (tableCachingEnabled) {
      final int tableCacheSize = Math.max((int) (config.getDouble(FORMAT_MAPRDB_JSON_TABLE_CACHE_SIZE)), MIN_TABLE_CACHE_SIZE);
      final int tableCacheExpiryTime = Math.max((int) (config.getDouble(FORMAT_MAPRDB_JSON_TABLE_CACHE_TIMEOUT)), MIN_TABLE_CACHE_ENTRY_TIMEOUT);

      RemovalListener<MapRDBTableCache.Key, Table> removalListener = new RemovalListener<MapRDBTableCache.Key, Table>() {
        public void onRemoval(RemovalNotification<MapRDBTableCache.Key, Table> removal) {
          Table table = removal.getValue();
          MapRDBTableCache.Key key = removal.getKey();
          logger.debug("time {} closing the tablePath {} tableHandle {} index {} userName {}",
              System.nanoTime(),
              key.path == null ? "null" : key.path, table == null ? "null" : table,
              key.indexDesc == null ? "null" : key.indexDesc.getIndexName(),
              key.ugi.getUserName() == null ? "null" : key.ugi.getUserName());
          table.close(); // close the table
        }
      };

      // Common table cache for primary and index tables. Key is Pair<tablePath, indexDesc>
      // For primary table, indexDesc is null.
      tableCache = CacheBuilder.newBuilder().
          expireAfterAccess(tableCacheExpiryTime, TimeUnit.MINUTES).
          maximumSize(tableCacheSize).
          removalListener(removalListener).build(new CacheLoader<MapRDBTableCache.Key, Table>() {

        @Override
        public Table load(final MapRDBTableCache.Key key) throws Exception {
          // getTable is already calling tableCache.get in correct user UGI context, so should be fine here.
          // key.Left is Path. key.Right is indexDesc.
          Table table = (key.indexDesc == null ? MapRDBImpl.getTable(key.path) : MapRDBImpl.getIndexTable(key.indexDesc));
          logger.debug("time {} opened the table for tablePath {} tableHandle {} index {} userName {}",
              System.nanoTime(),
              key.path == null ? "null" : key.path,
              table == null ? "null" : table,
              key.indexDesc == null ? "null" : key.indexDesc.getIndexName(),
              key.ugi.getUserName() == null ? "null" : key.ugi.getUserName());
          return table;
        }
      });

      logger.debug("table cache created with size {} and expiryTimeInMin {} ", tableCacheSize, tableCacheExpiryTime);
    }
  }


  /**
   * getTable given primary table path and indexDesc.
   * returns Table for corresponding index table if indexDesc is not null.
   * returns Table for primary table if indexDesc is null.
   *
   * @param tablePath primary table path
   * @param indexDesc index table descriptor
   */
  public Table getTable(final Path tablePath, final IndexDesc indexDesc, final String userName) throws DrillRuntimeException {

    final Table dbTableHandle;
    final UserGroupInformation proxyUserUgi = ImpersonationUtil.createProxyUgi(userName);

    try {
      dbTableHandle = proxyUserUgi.doAs(new PrivilegedExceptionAction<Table>() {
        public Table run() throws Exception {

          if (logger.isTraceEnabled()) {
            logger.trace("Getting MaprDB Table handle for proxy user: " + UserGroupInformation.getCurrentUser());
          }

          if (tableCachingEnabled) {
            Table table = tableCache.get(new MapRDBTableCache.Key(tablePath, indexDesc));
            logger.trace("time {} get the tablePath {} tableHandle {} index {} userName {} currentUser {}",
                System.nanoTime(), tablePath == null ? "null" : tablePath,
                table == null ? "null" : table,
                indexDesc == null ? "null" : indexDesc.getIndexName(),
                userName == null ? "null" : userName,
                UserGroupInformation.getCurrentUser() == null ? "null" : UserGroupInformation.getCurrentUser());
            return table;
          } else {
            return indexDesc == null ? MapRDBImpl.getTable(tablePath) : MapRDBImpl.getIndexTable(indexDesc);
          }
        }
      });
    } catch (Exception e) {
      throw new DrillRuntimeException("Error getting table: " + tablePath.toString() + (indexDesc == null ? "" : (", " +
          "IndexDesc: " + indexDesc.toString())), e);
    }

    return dbTableHandle;
  }

  /**
   * getTable given primary table name.
   * returns Table for primary table with given name.
   *
   * @param tableName primary table path
   */
  public Table getTable(String tableName, String userName) {
    return getTable(new Path(tableName), null, userName);
  }

  /**
   * getTable given primary table path.
   * returns Table for primary table with given path.
   *
   * @param tablePath primary table path
   */
  public Table getTable(Path tablePath, String userName) {
    return getTable(tablePath, null, userName);
  }

  /**
   * getTable given primary table name and indexDesc.
   * returns Table for corresponding index table if indexDesc is not null.
   * returns Table for primary table if indexDesc is null.
   *
   * @param tableName primary table name
   * @param indexDesc index table Descriptor
   */
  public Table getTable(String tableName, IndexDesc indexDesc, String userName) {
    return getTable(new Path(tableName), indexDesc, userName);
  }

  /**
   * closeTable
   *
   * @param table table to be closed.
   */
  public void closeTable(Table table) {
    if (!tableCachingEnabled && table != null) {
      table.close();
    }
  }

  /**
   * Key for {@link MapRDBTableCache} to store table path, {@link IndexDesc} and UGI.
   */
  static class Key {
    final Path path;

    final IndexDesc indexDesc;

    final UserGroupInformation ugi;

    Key(Path path, IndexDesc indexDesc) throws IOException {
      this.path = path;
      this.indexDesc = indexDesc;
      this.ugi = UserGroupInformation.getCurrentUser();
    }

    public int hashCode() {

      final int IdxDescHashCode = (indexDesc == null) ? 0 : indexDesc.getIndexFid().hashCode();
      return (path.hashCode() + IdxDescHashCode + ugi.hashCode());
    }

    static boolean isEqual(Object a, Object b) {
      return a == b || a != null && a.equals(b);
    }

    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      } else if (obj != null && obj instanceof MapRDBTableCache.Key) {
        MapRDBTableCache.Key that = (MapRDBTableCache.Key) obj;
        return isEqual(this.path, that.path)
            && isEqual(this.indexDesc, that.indexDesc)
            && isEqual(this.ugi, that.ugi);
      } else {
        return false;
      }
    }

    public String toString() {
      return "(Path: " + this.path.toString() +
          ", UGI: " + this.ugi.toString() +
          ", IndexDesc: " + (this.indexDesc == null ? "" : this.indexDesc.toString()) + ")";
    }
  }
}
