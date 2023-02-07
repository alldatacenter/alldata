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
package org.apache.drill.exec.expr.fn.registry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.AutoCloseables;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.store.TransientStore;
import org.apache.drill.exec.coord.store.TransientStoreConfig;
import org.apache.drill.exec.coord.store.TransientStoreListener;
import org.apache.drill.exec.exception.StoreException;
import org.apache.drill.exec.exception.VersionMismatchException;
import org.apache.drill.exec.proto.SchemaUserBitShared;
import org.apache.drill.exec.proto.UserBitShared.Registry;
import org.apache.drill.exec.store.sys.PersistentStoreConfig;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.store.sys.VersionedPersistentStore;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Is responsible for remote function registry management.
 * Creates all remote registry areas at startup and validates them,
 * during init establishes connections with three udf related stores.
 * Provides tools to work with three udf related stores, gives access to remote registry areas.
 * <p/>
 * There are three udf stores:
 *
 * <li><b>REGISTRY</b> - persistent store, stores remote function registry {@link Registry} under udf path
 * which contains information about all dynamically registered jars and their function signatures.
 * If connection is created for the first time, puts empty remote registry.</li>
 *
 * <li><b>UNREGISTRATION</b> - transient store, stores information under udf/unregister path.
 * udf/unregister path is persistent by itself but any child created will be transient.
 * Whenever user submits request to unregister jar, child path with jar name is created under this store.
 * This store also holds unregistration listener, which notifies all drill bits when child path is created,
 * so they can start local unregistration process.</li>
 *
 * <li><b>JARS</b> - transient store, stores information under udf/jars path.
 * udf/jars path is persistent by itself but any child created will be transient.
 * Servers as lock, not allowing to perform any action on the same time.
 * There two types of actions: {@link Action#REGISTRATION} and {@link Action#UNREGISTRATION}.
 * Before starting any action, users tries to create child path with jar name under this store
 * and if such path already exists, receives action being performed on that very jar.
 * When user finishes its action, he deletes child path with jar name.</li>
 * <p/>
 * There are three udf areas:
 *
 * <li><b>STAGING</b> - area where user copies binary and source jars before starting registration process.</li>
 * <li><b>REGISTRY</b> - area where registered jars are stored.</li>
 * <li><b>TMP</b> - area where source and binary jars are backed up in unique folder during registration process.</li>
 */
public class RemoteFunctionRegistry implements AutoCloseable {

  private static final String REGISTRY_PATH = "registry";
  private static final Logger logger = LoggerFactory.getLogger(RemoteFunctionRegistry.class);
  private static final ObjectMapper mapper = new ObjectMapper().enable(INDENT_OUTPUT);

  private final TransientStoreListener unregistrationListener;
  private int retryAttempts;
  private FileSystem fs;
  private Path registryArea;
  private Path stagingArea;
  private Path tmpArea;

  private VersionedPersistentStore<Registry> registry;
  private TransientStore<String> unregistration;
  private TransientStore<String> jars;

  public RemoteFunctionRegistry(TransientStoreListener unregistrationListener) {
    this.unregistrationListener = unregistrationListener;
  }

  public void init(DrillConfig config, PersistentStoreProvider storeProvider, ClusterCoordinator coordinator) {
    prepareStores(storeProvider, coordinator);
    prepareAreas(config);
    this.retryAttempts = config.getInt(ExecConstants.UDF_RETRY_ATTEMPTS);
  }

  /**
   * Returns current remote function registry version.
   * If remote function registry is not found or unreachable, logs error and returns -1.
   *
   * @return remote function registry version if any, -1 otherwise
   */
  public int getRegistryVersion() {
    DataChangeVersion version = new DataChangeVersion();
    boolean contains = false;
    try {
      contains = registry.contains(REGISTRY_PATH, version);
    } catch (Exception e) {
      logger.error("Problem during trying to access remote function registry [{}]", REGISTRY_PATH, e);
    }
    if (contains) {
      return version.getVersion();
    } else {
      logger.error("Remote function registry [{}] is unreachable", REGISTRY_PATH);
      return DataChangeVersion.NOT_AVAILABLE;
    }
  }

  /**
   * Report whether a remote registry exists. During some unit tests,
   * no remote registry exists, so the other methods should not be called.
   * @return true if a remote registry exists, false if this a local-only
   * instance and no such registry exists
   */
  public boolean hasRegistry() { return registry != null; }

  public Registry getRegistry(DataChangeVersion version) {
    return registry.get(REGISTRY_PATH, version);
  }

  public void updateRegistry(Registry registryContent, DataChangeVersion version) throws VersionMismatchException {
    registry.put(REGISTRY_PATH, registryContent, version);
  }

  public void submitForUnregistration(String jar) {
    unregistration.putIfAbsent(jar, jar);
  }

  public void finishUnregistration(String jar) {
    unregistration.remove(jar);
  }

  public String addToJars(String jar, Action action) {
    return jars.putIfAbsent(jar, action.toString());
  }

  public void removeFromJars(String jar) {
    jars.remove(jar);
  }

  public int getRetryAttempts() {
    return retryAttempts;
  }

  public FileSystem getFs() {
    return fs;
  }

  public Path getRegistryArea() {
    return registryArea;
  }

  public Path getStagingArea() {
    return stagingArea;
  }

  public Path getTmpArea() {
    return tmpArea;
  }

  /**
   * Connects to three stores: REGISTRY, UNREGISTRATION, JARS.
   * Puts in REGISTRY store with default instance of remote function registry if store is initiated for the first time.
   * Registers unregistration listener in UNREGISTRATION store.
   */
  private void prepareStores(PersistentStoreProvider storeProvider, ClusterCoordinator coordinator) {
    try {
      PersistentStoreConfig<Registry> registrationConfig = PersistentStoreConfig
          .newProtoBuilder(SchemaUserBitShared.Registry.WRITE, SchemaUserBitShared.Registry.MERGE)
          .name("udf")
          .persist()
          .build();
      registry = storeProvider.getOrCreateVersionedStore(registrationConfig);
      logger.trace("Remote function registry type: {}.", registry.getClass());
      registry.putIfAbsent(REGISTRY_PATH, Registry.getDefaultInstance());
    } catch (StoreException e) {
      throw new DrillRuntimeException("Failure while loading remote registry.", e);
    }

    TransientStoreConfig<String> unregistrationConfig = TransientStoreConfig.
        newJacksonBuilder(mapper, String.class).name("udf/unregister").build();
    unregistration = coordinator.getOrCreateTransientStore(unregistrationConfig);
    unregistration.addListener(unregistrationListener);

    TransientStoreConfig<String> jarsConfig = TransientStoreConfig.
        newJacksonBuilder(mapper, String.class).name("udf/jars").build();
    jars = coordinator.getOrCreateTransientStore(jarsConfig);
  }

  /**
   * Creates if absent and validates three udf areas: STAGING, REGISTRY and TMP.
   * Generated udf ares root from {@link ExecConstants#UDF_DIRECTORY_ROOT},
   * if not set, uses user home directory instead.
   */
  private void prepareAreas(DrillConfig config) {
    logger.info("Preparing three remote udf areas: staging, registry and tmp.");
    Configuration conf = new Configuration();
    if (config.hasPath(ExecConstants.UDF_DIRECTORY_FS)) {
      conf.set(FileSystem.FS_DEFAULT_NAME_KEY, config.getString(ExecConstants.UDF_DIRECTORY_FS));
    }

    try {
      this.fs = FileSystem.get(conf);
    } catch (IOException e) {
      throw DrillRuntimeException.create(e,
          "Error during file system %s setup", conf.get(FileSystem.FS_DEFAULT_NAME_KEY));
    }

    String root = fs.getHomeDirectory().toUri().getPath();
    if (config.hasPath(ExecConstants.UDF_DIRECTORY_ROOT)) {
      root = config.getString(ExecConstants.UDF_DIRECTORY_ROOT);
    }

    this.registryArea = createArea(fs, root, config.getString(ExecConstants.UDF_DIRECTORY_REGISTRY));
    this.stagingArea = createArea(fs, root, config.getString(ExecConstants.UDF_DIRECTORY_STAGING));
    this.tmpArea = createArea(fs, root, config.getString(ExecConstants.UDF_DIRECTORY_TMP));
  }

  /**
   * Concatenates udf are with root directory.
   * Creates udf area, if area does not exist.
   * Checks if area exists and is directory, if it is writable for current user,
   * throws {@link DrillRuntimeException} otherwise.
   *
   * @param fs file system where area should be created or checked
   * @param root root directory
   * @param directory directory path
   * @return path to area
   */
  private Path createArea(FileSystem fs, String root, String directory) {
    Path path = new Path(new File(root, directory).toURI().getPath());
    String fullPath = path.toUri().getPath();
    try {
      fs.mkdirs(path);
      Preconditions.checkState(fs.exists(path), "Area [%s] must exist", fullPath);
      FileStatus fileStatus = fs.getFileStatus(path);
      Preconditions.checkState(fileStatus.isDirectory(), "Area [%s] must be a directory", fullPath);
      FsPermission permission = fileStatus.getPermission();
      // The process user has write rights on directory if:
      // 1. process user is owner of the directory and has write rights
      // 2. process user is in group that has write rights
      // 3. any user has write rights
      Preconditions.checkState(
          (ImpersonationUtil.getProcessUserName()
              .equals(fileStatus.getOwner())
              && permission.getUserAction().implies(FsAction.WRITE)) ||
          (Sets.newHashSet(ImpersonationUtil.getProcessUserGroupNames())
              .contains(fileStatus.getGroup())
              && permission.getGroupAction().implies(FsAction.WRITE)) ||
          permission.getOtherAction().implies(FsAction.WRITE),
          "Area [%s] must be writable and executable for application user", fullPath);
    } catch (Exception e) {
      if (e instanceof DrillRuntimeException) {
        throw (DrillRuntimeException) e;
      }
      throw DrillRuntimeException.create(e,
          "Error during udf area creation [%s] on file system [%s]", fullPath, fs.getUri());
    }
    logger.info("Created remote udf area [{}] on file system [{}]", fullPath, fs.getUri());
    return path;
  }

  @Override
  public void close() {
    try {
      AutoCloseables.close(
          fs,
          registry,
          unregistration,
          jars);
    } catch (Exception e) {
      logger.warn("Failure on close()", e);
    }
  }

  public enum Action {
    REGISTRATION,
    UNREGISTRATION
  }
}
