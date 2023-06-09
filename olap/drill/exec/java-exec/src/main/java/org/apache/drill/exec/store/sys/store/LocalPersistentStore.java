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
package org.apache.drill.exec.store.sys.store;

import org.apache.commons.io.IOUtils;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.store.sys.BasePersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreMode;
import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.drill.exec.ExecConstants.DRILL_SYS_FILE_SUFFIX;

/**
 * Local persistent store stores its data on the given file system.
 * Data is stored in the files with key name as a base and
 * {@link org.apache.drill.exec.ExecConstants#DRILL_SYS_FILE_SUFFIX} suffix.
 *
 * @param <V> store data type
 */
public class LocalPersistentStore<V> extends BasePersistentStore<V> {

  private static final Logger logger = LoggerFactory.getLogger(LocalPersistentStore.class);

  private static final PathFilter SYS_FILE_SUFFIX_FILTER = path -> path.getName().endsWith(DRILL_SYS_FILE_SUFFIX);

  private final Path basePath;

  private final PersistentStoreConfig<V> config;

  private final DrillFileSystem fs;

  public LocalPersistentStore(DrillFileSystem fs, Path base, PersistentStoreConfig<V> config) {
    this.basePath = new Path(base, config.getName());
    this.config = config;
    this.fs = fs;
    try {
      fs.mkdirs(basePath);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Failure setting local persistent store path [%s]: %s",
        basePath, e.getMessage());
    }
  }

  @Override
  public PersistentStoreMode getMode() {
    return PersistentStoreMode.PERSISTENT;
  }

  public static Path getLogDir() {
    String drillLogDir = System.getenv("DRILL_LOG_DIR");
    if (drillLogDir == null) {
      drillLogDir = System.getProperty("drill.log.dir");
    }
    if (drillLogDir == null) {
      drillLogDir = "/var/log/drill";
    }
    return new Path(new File(drillLogDir).getAbsoluteFile().toURI());
  }

  public static DrillFileSystem getFileSystem(DrillConfig config, Path root) throws IOException {
    Path blobRoot = root == null ? getLogDir() : root;
    Configuration fsConf = new Configuration();
    if (blobRoot.toUri().getScheme() != null) {
      fsConf.set(FileSystem.FS_DEFAULT_NAME_KEY, blobRoot.toUri().toString());
    }


    DrillFileSystem fs = new DrillFileSystem(fsConf);
    fs.mkdirs(blobRoot);
    return fs;
  }

  @Override
  public Iterator<Map.Entry<String, V>> getRange(int skip, int take) {
    List<FileStatus> fileStatuses;
    try {
      fileStatuses = DrillFileSystemUtil.listFiles(fs, basePath, false, SYS_FILE_SUFFIX_FILTER);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable to retrieve store data: %s", e.getMessage());
    }

    if (fileStatuses.isEmpty()) {
      return Collections.emptyIterator();
    }

    return fileStatuses.stream()
      .map(this::extractKeyName)
      .sorted()
      .skip(skip)
      .limit(take)
      .collect(Collectors.toMap(
        Function.identity(),
        this::get,
        (o, n) -> n,
        LinkedHashMap::new))
      .entrySet()
      .iterator();
  }

  @Override
  public boolean contains(String key) {
    Path path = makePath(key, false);
    return exists(path);
  }

  @Override
  public V get(String key) {
    Path path = makePath(key, false);
    if (!exists(path)) {
      return null;
    }

    try (InputStream is = fs.open(path)) {
      byte[] bytes = IOUtils.toByteArray(is);
      return deserialize(path, bytes);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable to retrieve store data for the path [%s]: %s",
        path, e.getMessage());
    }
  }

  @Override
  public void put(String key, V value) {
    Path path = makePath(key, true);
    put(path, value);
  }

  @Override
  public boolean putIfAbsent(String key, V value) {
    Path path = makePath(key, true);
    if (exists(path)) {
      return false;
    }

    put(path, value);
    return true;
  }

  @Override
  public void delete(String key) {
    Path path = makePath(key, true);
    try {
      fs.delete(path, false);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable to delete store data for the path [%s]: %s",
        path, e.getMessage());
    }
  }

  @Override
  public void close() {
  }

  /**
   * Checks if given key name is valid. Since store data is persisted on the file system,
   * key name must not be null or contain any special characters.
   *
   * @param key key name
   * @return true if key name is valid, false otherwise
   */
  private boolean isValidKey(String key) {
    return key != null
      && !key.isEmpty()
      && !key.contains(":")
      && !key.contains("..")
      && !key.contains("/");
  }

  /**
   * Constructs path based on given path name.
   * If given key is invalid, will fail only if {@code failOnInvalidKey} is passed as true,
   * otherwise will return null value.
   *
   * @param key key name
   * @param failOnInvalidKey flag indicating if exception should be on the invalid key
   * @return constructed path relevant to the current store configuration
   */
  private Path makePath(String key, boolean failOnInvalidKey) {
    if (isValidKey(key)) {
      try {
        return new Path(basePath, key + DRILL_SYS_FILE_SUFFIX);
      } catch (IllegalArgumentException e) {
        return handleInvalidKey(key, e, failOnInvalidKey);
      }
    } else {
      return handleInvalidKey(key, null, failOnInvalidKey);
    }
  }

  private Path handleInvalidKey(String key, Throwable throwable, boolean failOnInvalidKey) {
    if (failOnInvalidKey) {
      throw DrillRuntimeException.create(throwable, "Illegal storage key name: %s", key);
    } else {
      logger.debug("Illegal storage key name: {}", key, throwable);
      return null;
    }
  }

  private boolean exists(Path path) {
    try {
      return path != null && fs.exists(path);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable to check store file [%s] existence: %s",
        path, e.getMessage());
    }
  }

  private byte[] serialize(Path path, V value) {
    try {
      return config.getSerializer().serialize(value);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable serialize value for the store key [%s]: %s",
        path, e.getMessage());
    }
  }

  private V deserialize(Path path, byte[] bytes) {
    try {
      return config.getSerializer().deserialize(bytes);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable deserialize value for the path [%s]: %s",
        path, e.getMessage());
    }
  }

  private void put(Path path, V value) {
    try (OutputStream os = fs.create(path)) {
      IOUtils.write(serialize(path, value), os);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e, "Unable to store data for the path [%s]: %s",
        path, e.getMessage());
    }
  }

  /**
   * Extracts key name from file status.
   * Key name is base of the file name where key data is stored.
   * {@link org.apache.drill.exec.ExecConstants#DRILL_SYS_FILE_SUFFIX}
   * should be removed from the file name to obtain key name.
   *
   * @param fileStatus file status
   * @return key name
   */
  private String extractKeyName(FileStatus fileStatus) {
    String name = fileStatus.getPath().getName();
    return name.substring(0, name.length() - DRILL_SYS_FILE_SUFFIX.length());
  }
}
