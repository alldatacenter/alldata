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
package org.apache.drill.exec.coord.zk;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.drill.shaded.guava.com.google.common.base.Function;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.collect.Iterables;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.drill.common.collections.ImmutableEntry;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;

/**
 * A namespace aware Zookeeper client.
 *
 * The implementation only operates under the given namespace and is safe to use.
 *
 * Note that instance of this class holds onto resources that must be released via {@code #close()}.
 */
public class ZookeeperClient implements AutoCloseable {
  private final CuratorFramework curator;
  private final String root;
  private final PathChildrenCache cache;
  private final CreateMode mode;

  public ZookeeperClient(final CuratorFramework curator, final String root, final CreateMode mode) {
    this.curator = Preconditions.checkNotNull(curator, "curator is required");
    Preconditions.checkArgument(!Strings.isNullOrEmpty(root), "root path is required");
    Preconditions.checkArgument(root.charAt(0) == '/', "root path must be absolute");
    this.root = root;
    this.mode = Preconditions.checkNotNull(mode, "mode is required");
    this.cache = new PathChildrenCache(curator, root, true);
  }

  /**
   * Starts the client. This call ensures the creation of the root path.
   *
   * @throws Exception  if cache fails to start or root path creation fails.
   * @see #close()
   */
  public void start() throws Exception {
    curator.newNamespaceAwareEnsurePath(root).ensure(curator.getZookeeperClient()); // ensure root is created
    getCache().start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE); //build cache at start up, to ensure we get correct results right away
  }

  public PathChildrenCache getCache() {
    return cache;
  }

  public String getRoot() {
    return root;
  }

  public CreateMode getMode() {
    return mode;
  }

  /**
   * Returns true if path exists in the cache, false otherwise.
   * Note that calls to this method are eventually consistent.
   *
   * @param path path to check
   * @return true if path exists, false otherwise
   */
  public boolean hasPath(final String path) {
    return hasPath(path, false, null);
  }

  /**
   * Returns true if path exists, false otherwise.
   * If consistent flag is set to true, check is done directly is made against Zookeeper directly,
   * else check is done against local cache.
   *
   * @param path path to check
   * @param consistent whether the check should be consistent
   * @return true if path exists, false otherwise
   */
  public boolean hasPath(final String path, final boolean consistent) {
    return hasPath(path, consistent, null);
  }

  /**
   * Checks if the given path exists.
   * If the flag consistent is set, the check is consistent as it is made against Zookeeper directly.
   * Otherwise, the check is eventually consistent.
   *
   * If consistency flag is set to true and version holder is not null, passes version holder to get data change version.
   * Data change version is retrieved from {@link Stat} object, it increases each time znode data change is performed.
   * Link to Zookeeper documentation - https://zookeeper.apache.org/doc/r3.2.2/zookeeperProgrammers.html#sc_zkDataModel_znodes
   *
   * @param path path to check
   * @param consistent whether the check should be consistent
   * @param version version holder
   * @return true if path exists, false otherwise
   */
  public boolean hasPath(final String path, final boolean consistent, final DataChangeVersion version) {
    Preconditions.checkNotNull(path, "path is required");

    final String target = PathUtils.join(root, path);
    try {
      if (consistent) {
        Stat stat = curator.checkExists().forPath(target);
        if (version != null && stat != null) {
          version.setVersion(stat.getVersion());
        }
        return stat != null;
      } else {
        return getCache().getCurrentData(target) != null;
      }
    } catch (final Exception e) {
      throw new DrillRuntimeException("error while checking path on zookeeper", e);
    }
  }

  /**
   * Returns a value corresponding to the given path if path exists in the cache, null otherwise.
   *
   * Note that calls to this method are eventually consistent.
   *
   * @param path  target path
   */
  public byte[] get(final String path) {
    return get(path, false);
  }

  /**
   * Returns the value corresponding to the given key, null otherwise.
   *
   * If the flag consistent is set, the check is consistent as it is made against Zookeeper directly. Otherwise,
   * the check is eventually consistent.
   *
   * @param path  target path
   * @param consistent consistency flag
   */
  public byte[] get(final String path, final boolean consistent) {
    return get(path, consistent, null);
  }

  /**
   * Returns the value corresponding to the given key, null otherwise.
   *
   * The check is consistent as it is made against Zookeeper directly.
   *
   * Passes version holder to get data change version.
   *
   * @param path  target path
   * @param version version holder
   */
  public byte[] get(final String path, final DataChangeVersion version) {
    return get(path, true, version);
  }

  /**
   * Returns the value corresponding to the given key, null otherwise.
   *
   * If the flag consistent is set, the check is consistent as it is made against Zookeeper directly. Otherwise,
   * the check is eventually consistent.
   *
   * If consistency flag is set to true and version holder is not null, passes version holder to get data change version.
   * Data change version is retrieved from {@link Stat} object, it increases each time znode data change is performed.
   * Link to Zookeeper documentation - https://zookeeper.apache.org/doc/r3.2.2/zookeeperProgrammers.html#sc_zkDataModel_znodes
   *
   * @param path  target path
   * @param consistent consistency check
   * @param version version holder
   */
  public byte[] get(final String path, final boolean consistent, final DataChangeVersion version) {
    Preconditions.checkNotNull(path, "path is required");

    final String target = PathUtils.join(root, path);
    if (consistent) {
      try {
        if (version != null) {
          Stat stat = new Stat();
          final byte[] bytes = curator.getData().storingStatIn(stat).forPath(target);
          version.setVersion(stat.getVersion());
          return bytes;
        }
        return curator.getData().forPath(target);
      } catch (final Exception ex) {
        throw new DrillRuntimeException(String.format("error retrieving value for [%s]", path), ex);
      }
    } else {
      final ChildData data = getCache().getCurrentData(target);
      if (data != null) {
        return data.getData();
      }
    }
    return null;
  }

  /**
   * Creates the given path without placing any data in.
   *
   * @param path  target path
   */
  public void create(final String path) {
    Preconditions.checkNotNull(path, "path is required");

    final String target = PathUtils.join(root, path);
    try {
      curator.create().withMode(mode).forPath(target);
      getCache().rebuildNode(target);
    } catch (final Exception e) {
      throw new DrillRuntimeException("unable to put ", e);
    }
  }

  /**
   * Puts the given byte sequence into the given path.
   *
   * If path does not exists, this call creates it.
   *
   * @param path  target path
   * @param data  data to store
   */
  public void put(final String path, final byte[] data) {
    put(path, data, null);
  }

  /**
   * Puts the given byte sequence into the given path.
   *
   * If path does not exists, this call creates it.
   *
   * If version holder is not null and path already exists, passes given version for comparison.
   * Zookeeper maintains stat structure that holds version number which increases each time znode data change is performed.
   * If we pass version that doesn't match the actual version of the data,
   * the update will fail {@link org.apache.zookeeper.KeeperException.BadVersionException}.
   * We catch such exception and re-throw it as {@link VersionMismatchException}.
   * Link to documentation - https://zookeeper.apache.org/doc/r3.2.2/zookeeperProgrammers.html#sc_zkDataModel_znodes
   *
   * @param path  target path
   * @param data  data to store
   * @param version version holder
   */
  public void put(final String path, final byte[] data, DataChangeVersion version) {
    Preconditions.checkNotNull(path, "path is required");
    Preconditions.checkNotNull(data, "data is required");

    final String target = PathUtils.join(root, path);
    try {
      // we make a consistent read to ensure this call won't fail upon consecutive calls on the same path
      // before cache is updated
      boolean hasNode = hasPath(path, true);
      if (!hasNode) {
        try {
          curator.create().withMode(mode).forPath(target, data);
        } catch (NodeExistsException e) {
          // Handle race conditions since Drill is distributed and other
          // drillbits may have just created the node. This assumes that we do want to
          // override the new node. Makes sense here, because if the node had existed,
          // we'd have updated it.
          hasNode = true;
        }
      }
      if (hasNode) {
        if (version != null) {
          try {
            curator.setData().withVersion(version.getVersion()).forPath(target, data);
          } catch (final KeeperException.BadVersionException e) {
            throw new VersionMismatchException("Unable to put data. Version mismatch is detected.", version.getVersion(), e);
          }
        } else {
          curator.setData().forPath(target, data);
        }
      }
      getCache().rebuildNode(target);
    } catch (final VersionMismatchException e) {
      throw e;
    } catch (final Exception e) {
      throw new DrillRuntimeException("unable to put ", e);
    }
  }

  /**
   * Puts the given byte sequence into the given path if path is does not exist.
   *
   * @param path  target path
   * @param data  data to store
   * @return null if path was created, else data stored for the given path
   */
  public byte[] putIfAbsent(final String path, final byte[] data) {
    Preconditions.checkNotNull(path, "path is required");
    Preconditions.checkNotNull(data, "data is required");

    final String target = PathUtils.join(root, path);
    try {
      try {
        curator.create().withMode(mode).forPath(target, data);
        getCache().rebuildNode(target);
        return null;
      } catch (NodeExistsException e) {
        // do nothing
      }
      return curator.getData().forPath(target);
    } catch (final Exception e) {
      throw new DrillRuntimeException("unable to put ", e);
    }
  }

  /**
   * Deletes the given node residing at the given path
   *
   * @param path  target path to delete
   */
  public void delete(final String path) {
    Preconditions.checkNotNull(path, "path is required");

    final String target = PathUtils.join(root, path);
    try {
      curator.delete().forPath(target);
      getCache().rebuildNode(target);
    } catch (final Exception e) {
      throw new DrillRuntimeException(String.format("unable to delete node at %s", target), e);
    }
  }

  /**
   * Returns an iterator of (key, value) pairs residing under {@link #getRoot() root} path.
   */
  public Iterator<Map.Entry<String, byte[]>> entries() {
    final String prefix = PathUtils.join(root, "/");
    return Iterables.transform(getCache().getCurrentData(), new Function<ChildData, Map.Entry<String, byte[]>>() {
      @Nullable
      @Override
      public Map.Entry<String, byte[]> apply(final ChildData data) {
        // normalize key name removing the root prefix. resultant key must be a relative path, not beginning with a '/'.
        final String key = data.getPath().replace(prefix, "");
        return new ImmutableEntry<>(key, data.getData());
      }
    }).iterator();
  }

  @Override
  public void close() throws Exception {
    getCache().close();
  }

}
