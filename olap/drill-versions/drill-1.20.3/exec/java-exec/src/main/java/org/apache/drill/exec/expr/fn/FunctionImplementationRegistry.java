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
package org.apache.drill.exec.expr.fn;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.expression.fn.FunctionReplacementUtils;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.RunTimeScan;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.store.TransientStoreEvent;
import org.apache.drill.exec.coord.store.TransientStoreListener;
import org.apache.drill.exec.exception.FunctionValidationException;
import org.apache.drill.exec.exception.JarValidationException;
import org.apache.drill.exec.expr.fn.registry.LocalFunctionRegistry;
import org.apache.drill.exec.expr.fn.registry.FunctionHolder;
import org.apache.drill.exec.expr.fn.registry.JarScan;
import org.apache.drill.exec.expr.fn.registry.RemoteFunctionRegistry;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.proto.UserBitShared.Jar;
import org.apache.drill.exec.resolver.FunctionResolver;
import org.apache.drill.exec.resolver.FunctionResolverFactory;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionSet;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.exec.store.sys.store.DataChangeVersion;
import org.apache.drill.exec.util.JarUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry for functions. Notably, in addition to Drill its functions (in
 * {@link LocalFunctionRegistry}), other PluggableFunctionRegistry (e.g.,
 * {@link org.apache.drill.exec.expr.fn.HiveFunctionRegistry}) is also
 * registered in this class
 */
public class FunctionImplementationRegistry implements FunctionLookupContext, AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(FunctionImplementationRegistry.class);

  private final LocalFunctionRegistry localFunctionRegistry;
  private final RemoteFunctionRegistry remoteFunctionRegistry;
  private final Path localUdfDir;
  private boolean deleteTmpDir;
  private File tmpDir;
  private final List<PluggableFunctionRegistry> pluggableFuncRegistries = new ArrayList<>();
  private OptionSet optionManager;
  private final boolean useDynamicUdfs;

  @VisibleForTesting
  public FunctionImplementationRegistry(DrillConfig config){
    this(config, ClassPathScanner.fromPrescan(config));
  }

  public FunctionImplementationRegistry(DrillConfig config, ScanResult classpathScan) {
    this(config, classpathScan, null);
  }

  public FunctionImplementationRegistry(DrillConfig config, ScanResult classpathScan, OptionManager optionManager) {
    Stopwatch w = Stopwatch.createStarted();

    logger.debug("Generating function registry.");
    this.optionManager = optionManager;

    // Unit tests fail if dynamic UDFs are turned on AND the test happens
    // to access an undefined function. Since we want a reasonable failure
    // rather than a crash, we provide a boot-time option, set only by
    // tests, to disable DUDF lookup.

    useDynamicUdfs = !config.getBoolean(ExecConstants.UDF_DISABLE_DYNAMIC);
    localFunctionRegistry = new LocalFunctionRegistry(classpathScan);

    Set<Class<? extends PluggableFunctionRegistry>> registryClasses =
        classpathScan.getImplementations(PluggableFunctionRegistry.class);

    for (Class<? extends PluggableFunctionRegistry> clazz : registryClasses) {
      for (Constructor<?> c : clazz.getConstructors()) {
        Class<?>[] params = c.getParameterTypes();
        if (params.length != 1 || params[0] != DrillConfig.class) {
          logger.warn("Skipping PluggableFunctionRegistry constructor {} for class {} since it doesn't implement a " +
              "[constructor(DrillConfig)]", c, clazz);
          continue;
        }

        try {
          PluggableFunctionRegistry registry = (PluggableFunctionRegistry)c.newInstance(config);
          pluggableFuncRegistries.add(registry);
        } catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          logger.warn("Unable to instantiate PluggableFunctionRegistry class '{}'. Skipping it.", clazz, e);
        }

        break;
      }
    }
    logger.info("Function registry loaded.  {} functions loaded in {} ms.", localFunctionRegistry.size(), w.elapsed(TimeUnit.MILLISECONDS));
    this.remoteFunctionRegistry = new RemoteFunctionRegistry(new UnregistrationListener());
    this.localUdfDir = getLocalUdfDir(config);
  }

  public FunctionImplementationRegistry(DrillConfig config, ScanResult classpathScan, OptionSet optionManager) {
    this(config, classpathScan);
    this.optionManager = optionManager;
  }

  /**
   * Register functions in given operator table.
   * @param operatorTable operator table
   */
  public void register(DrillOperatorTable operatorTable) {
    // Register Drill functions first and move to pluggable function registries.
    localFunctionRegistry.register(operatorTable);

    for(PluggableFunctionRegistry registry : pluggableFuncRegistries) {
      registry.register(operatorTable);
    }
  }

  /**
   * First attempts to find the Drill function implementation that matches the name, arg types and return type.
   * If exact function implementation was not found,
   * syncs local function registry with remote function registry if needed
   * and tries to find function implementation one more time
   * but this time using given <code>functionResolver</code>.
   *
   * @param functionResolver function resolver
   * @param functionCall function call
   * @return best matching function holder
   */
  @Override
  public DrillFuncHolder findDrillFunction(FunctionResolver functionResolver, FunctionCall functionCall) {
    AtomicInteger version = new AtomicInteger();
    String newFunctionName = functionReplacement(functionCall);

    // Dynamic UDFS: First try with exact match. If not found, we may need to
    // update the registry, so sync with remote.

    if (useDynamicUdfs) {
      List<DrillFuncHolder> functions = localFunctionRegistry.getMethods(newFunctionName, version);
      FunctionResolver exactResolver = FunctionResolverFactory.getExactResolver(functionCall);
      DrillFuncHolder holder = exactResolver.getBestMatch(functions, functionCall);
      if (holder != null) {
        return holder;
      }
      syncWithRemoteRegistry(version.get());
    }

    // Whether Dynamic UDFs or not: look in the registry for
    // an inexact match.

    List<DrillFuncHolder> functions = localFunctionRegistry.getMethods(newFunctionName, version);
    return functionResolver.getBestMatch(functions, functionCall);
  }

  /**
   * Checks if this function replacement is needed.
   *
   * @param functionCall function call
   * @return new function name is replacement took place, otherwise original function name
   */
  private String functionReplacement(FunctionCall functionCall) {
    String funcName = functionCall.getName();
    if (functionCall.argCount() == 0) {
      return funcName;
    }
    boolean castEmptyStringToNull = optionManager != null &&
                  optionManager.getOption(ExecConstants.CAST_EMPTY_STRING_TO_NULL_OPTION);
    if (!castEmptyStringToNull) {
      return funcName;
    }
    MajorType majorType =  functionCall.arg(0).getMajorType();
    DataMode dataMode = majorType.getMode();
    MinorType minorType = majorType.getMinorType();
    if (FunctionReplacementUtils.isReplacementNeeded(funcName, minorType)) {
      funcName = FunctionReplacementUtils.getReplacingFunction(funcName, dataMode, minorType);
    }

    return funcName;
  }

  /**
   * Finds the Drill function implementation that matches the name, arg types and return type.
   *
   * @param name function name
   * @param argTypes input parameters types
   * @param returnType function return type
   * @return exactly matching function holder
   */
  public DrillFuncHolder findExactMatchingDrillFunction(String name, List<MajorType> argTypes, MajorType returnType) {
    return findExactMatchingDrillFunction(name, argTypes, returnType, useDynamicUdfs);
  }

  /**
   * Finds the Drill function implementation that matches the name, arg types and return type.
   * If exact function implementation was not found,
   * checks if local function registry is in sync with remote function registry.
   * If not syncs them and tries to find exact function implementation one more time
   * but with retry flag set to false.
   *
   * @param name function name
   * @param argTypes input parameters types
   * @param returnType function return type
   * @param retry retry on failure flag
   * @return exactly matching function holder
   */
  private DrillFuncHolder findExactMatchingDrillFunction(String name,
                                                         List<MajorType> argTypes,
                                                         MajorType returnType,
                                                         boolean retry) {
    AtomicInteger version = new AtomicInteger();
    for (DrillFuncHolder h : localFunctionRegistry.getMethods(name, version)) {
      if (h.matches(returnType, argTypes)) {
        return h;
      }
    }
    if (retry && syncWithRemoteRegistry(version.get())) {
      return findExactMatchingDrillFunction(name, argTypes, returnType, false);
    }
    return null;
  }

  /**
   * Find function implementation for given <code>functionCall</code> in non-Drill function registries such as Hive UDF
   * registry.
   *
   * Note: Order of searching is same as order of {@link org.apache.drill.exec.expr.fn.PluggableFunctionRegistry}
   * implementations found on classpath.
   *
   * @param functionCall function call
   * @return drill function holder
   */
  @Override
  public AbstractFuncHolder findNonDrillFunction(FunctionCall functionCall) {
    for(PluggableFunctionRegistry registry : pluggableFuncRegistries) {
      AbstractFuncHolder h = registry.getFunction(functionCall);
      if (h != null) {
        return h;
      }
    }

    return null;
  }

  // Method to find if the output type of a drill function if of complex type
  public boolean isFunctionComplexOutput(String name) {
    List<DrillFuncHolder> methods = localFunctionRegistry.getMethods(name);
    for (DrillFuncHolder holder : methods) {
      if (holder.getReturnValue().isComplexWriter()) {
        return true;
      }
    }
    return false;
  }

  public LocalFunctionRegistry getLocalFunctionRegistry() {
    return localFunctionRegistry;
  }

  public RemoteFunctionRegistry getRemoteFunctionRegistry() {
    return remoteFunctionRegistry;
  }

  /**
   * Using given local path to jar creates unique class loader for this jar.
   * Class loader is closed to release opened connection to jar when validation is finished.
   * Scan jar content to receive list of all scanned classes
   * and starts validation process against local function registry.
   * Checks if received list of validated function is not empty.
   *
   * @param path local path to jar we need to validate
   * @return list of validated function signatures
   */
  public List<String> validate(Path path) throws IOException {
    URL url = path.toUri().toURL();
    URL[] urls = {url};
    try (URLClassLoader classLoader = new URLClassLoader(urls)) {
      ScanResult jarScanResult = scan(classLoader, path, urls);
      List<String> functions = localFunctionRegistry.validate(path.getName(), jarScanResult);
      if (functions.isEmpty()) {
        throw new FunctionValidationException(String.format("Jar %s does not contain functions", path.getName()));
      }
      return functions;
    }
  }

  /**
   * Purpose of this method is to synchronize remote and local function registries if needed
   * and to inform if function registry was changed after given version.
   * <p/>
   * To make synchronization as much light-weigh as possible, first only versions of both registries are checked
   * without any locking. If synchronization is needed, enters synchronized block to prevent others loading the same jars.
   * The need of synchronization is checked again (double-check lock) before comparing jars.
   * If any missing jars are found, they are downloaded to local udf area, each is wrapped into {@link JarScan}.
   * Once jar download is finished, all missing jars are registered in one batch.
   * In case if any errors during jars download / registration, these errors are logged.
   * <p/>
   * During registration local function registry is updated with remote function registry version it is synced with.
   * When at least one jar of the missing jars failed to download / register,
   * local function registry version are not updated but jars that where successfully downloaded / registered
   * are added to local function registry.
   * <p/>
   * If synchronization between remote and local function registry was not needed,
   * checks if given registry version matches latest sync version
   * to inform if function registry was changed after given version.
   *
   * @param version remote function registry local function registry was based on
   * @return true if remote and local function registries were synchronized after given version
   */
  public boolean syncWithRemoteRegistry(int version) {
    // Do the version check only if a remote registry exists. It does
    // not exist for some JMockit-based unit tests.
    if (isRegistrySyncNeeded()) {
      synchronized (this) {
        int localRegistryVersion = localFunctionRegistry.getVersion();
        if (isRegistrySyncNeeded(remoteFunctionRegistry.getRegistryVersion(), localRegistryVersion))  {
          DataChangeVersion remoteVersion = new DataChangeVersion();
          List<String> missingJars = getMissingJars(this.remoteFunctionRegistry, localFunctionRegistry, remoteVersion);
          List<JarScan> jars = new ArrayList<>();
          if (!missingJars.isEmpty()) {
            logger.info("Starting dynamic UDFs lazy-init process.\n" +
                "The following jars are going to be downloaded and registered locally: " + missingJars);
            for (String jarName : missingJars) {
              Path binary = null;
              Path source = null;
              URLClassLoader classLoader = null;
              try {
                binary = copyJarToLocal(jarName, this.remoteFunctionRegistry);
                source = copyJarToLocal(JarUtil.getSourceName(jarName), this.remoteFunctionRegistry);
                URL[] urls = {binary.toUri().toURL(), source.toUri().toURL()};
                classLoader = new URLClassLoader(urls);
                ScanResult scanResult = scan(classLoader, binary, urls);
                localFunctionRegistry.validate(jarName, scanResult);
                jars.add(new JarScan(jarName, scanResult, classLoader));
              } catch (Exception e) {
                deleteQuietlyLocalJar(binary);
                deleteQuietlyLocalJar(source);
                if (classLoader != null) {
                  try {
                    classLoader.close();
                  } catch (Exception ex) {
                    logger.warn("Problem during closing class loader for {}", jarName, e);
                  }
                }
                logger.error("Problem during remote functions load from {}", jarName, e);
              }
            }
          }
          int latestRegistryVersion = jars.size() != missingJars.size() ?
              localRegistryVersion : remoteVersion.getVersion();
          localFunctionRegistry.register(jars, latestRegistryVersion);
          return true;
        }
      }
    }

    return version != localFunctionRegistry.getVersion();
  }

  /**
   * Checks if remote and local registries should be synchronized.
   * Before comparing versions, checks if remote function registry is actually exists.
   *
   * @return true is local registry should be refreshed, false otherwise
   */
  private boolean isRegistrySyncNeeded() {
    logger.trace("Has remote function registry: {}", remoteFunctionRegistry.hasRegistry());
    return remoteFunctionRegistry.hasRegistry() &&
           isRegistrySyncNeeded(remoteFunctionRegistry.getRegistryVersion(), localFunctionRegistry.getVersion());
  }

  /**
   * Checks if local function registry should be synchronized with remote function registry.
   *
   * <ul>If remote function registry version is {@link DataChangeVersion#UNDEFINED},
   * it means that remote function registry does not support versioning
   * thus we need to synchronize both registries.</ul>
   * <ul>If remote function registry version is {@link DataChangeVersion#NOT_AVAILABLE},
   * it means that remote function registry is unreachable
   * or is not configured thus we skip synchronization and return false.</ul>
   * <ul>For all other cases synchronization is needed if remote
   * and local function registries versions do not match.</ul>
   *
   * @param remoteVersion remote function registry version
   * @param localVersion local function registry version
   * @return true is local registry should be refreshed, false otherwise
   */
  private boolean isRegistrySyncNeeded(int remoteVersion, int localVersion) {
    logger.trace("Compare remote [{}] and local [{}] registry versions.", remoteVersion, localVersion);
    return remoteVersion == DataChangeVersion.UNDEFINED ||
        (remoteVersion != DataChangeVersion.NOT_AVAILABLE && remoteVersion != localVersion);
  }

  /**
  * First finds path to marker file url, otherwise throws {@link JarValidationException}.
  * Then scans jar classes according to list indicated in marker files.
  * Additional logic is added to close {@link URL} after {@link ConfigFactory#parseURL(URL)}.
  * This is extremely important for Windows users where system doesn't allow to delete file if it's being used.
  *
  * @param classLoader unique class loader for jar
  * @param path local path to jar
  * @param urls urls associated with the jar (ex: binary and source)
  * @return scan result of packages, classes, annotations found in jar
  */
  private ScanResult scan(ClassLoader classLoader, Path path, URL[] urls) throws IOException {
    Enumeration<URL> markerFileEnumeration = classLoader.getResources(
        ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME);
    while (markerFileEnumeration.hasMoreElements()) {
      URL markerFile = markerFileEnumeration.nextElement();
      if (markerFile.getPath().contains(path.toUri().getPath())) {
        URLConnection markerFileConnection = null;
        try {
          markerFileConnection = markerFile.openConnection();
          DrillConfig drillConfig = DrillConfig.create(ConfigFactory.parseURL(markerFile));
          return RunTimeScan.dynamicPackageScan(drillConfig, Sets.newHashSet(urls));
        } finally {
          if (markerFileConnection instanceof JarURLConnection) {
            ((JarURLConnection) markerFileConnection).getJarFile().close();
          }
        }
      }
    }
    throw new JarValidationException(String.format("Marker file %s is missing in %s",
        ConfigConstants.DRILL_JAR_MARKER_FILE_RESOURCE_PATHNAME, path.getName()));
  }

  /**
   * Return list of jars that are missing in local function registry
   * but present in remote function registry.
   * Also updates version holder with remote function registry version.
   *
   * @param remoteFunctionRegistry remote function registry
   * @param localFunctionRegistry local function registry
   * @param version holder for remote function registry version
   * @return list of missing jars
   */
  private List<String> getMissingJars(RemoteFunctionRegistry remoteFunctionRegistry,
                                      LocalFunctionRegistry localFunctionRegistry,
                                      DataChangeVersion version) {
    List<Jar> remoteJars = remoteFunctionRegistry.getRegistry(version).getJarList();
    List<String> localJars = localFunctionRegistry.getAllJarNames();
    List<String> missingJars = new ArrayList<>();
    for (Jar jar : remoteJars) {
      if (!localJars.contains(jar.getName())) {
        missingJars.add(jar.getName());
      }
    }
    return missingJars;
  }

  /**
   * Retrieve all functions, mapped by source jars (after syncing)
   * @return Map of source jars and their functionHolders
   */
  public Map<String, List<FunctionHolder>> getAllJarsWithFunctionsHolders() {
    if (useDynamicUdfs) {
      syncWithRemoteRegistry(localFunctionRegistry.getVersion());
    }
    return localFunctionRegistry.getAllJarsWithFunctionsHolders();
  }

  /**
   * Creates local udf directory, if it doesn't exist.
   * Checks if local udf directory is a directory and if current application has write rights on it.
   * Attempts to clean up local udf directory in case jars were left after previous drillbit run.
   *
   * @param config drill config
   * @return path to local udf directory
   */
  private Path getLocalUdfDir(DrillConfig config) {
    tmpDir = getTmpDir(config);
    File udfDir = new File(tmpDir, config.getString(ExecConstants.UDF_DIRECTORY_LOCAL));
    String udfPath = udfDir.getPath();
    if (udfDir.mkdirs()) {
      logger.debug("Local udf directory [{}] was created", udfPath);
    }
    Preconditions.checkState(udfDir.exists(), "Local udf directory [%s] must exist", udfPath);
    Preconditions.checkState(udfDir.isDirectory(), "Local udf directory [%s] must be a directory", udfPath);
    Preconditions.checkState(udfDir.canWrite(), "Local udf directory [%s] must be writable for application user", udfPath);
    try {
      FileUtils.cleanDirectory(udfDir);
    } catch (IOException e) {
      throw new DrillRuntimeException("Error during local udf directory clean up", e);
    }
    logger.info("Created and validated local udf directory [{}]", udfPath);
    return new Path(udfDir.toURI());
  }

  /**
   * First tries to get drill temporary directory value from from config ${drill.tmp-dir},
   * then checks environmental variable $DRILL_TMP_DIR.
   * If value is still missing, generates directory using {@link DrillFileUtils#createTempDir()}.
   * If temporary directory was generated, sets {@link #deleteTmpDir} to true
   * to delete directory on drillbit exit.
   *
   * @param config drill config
   * @return drill temporary directory path
   */
  private File getTmpDir(DrillConfig config) {
    String drillTempDir;
    if (config.hasPath(ExecConstants.DRILL_TMP_DIR)) {
      drillTempDir = config.getString(ExecConstants.DRILL_TMP_DIR);
    } else {
      drillTempDir = System.getenv("DRILL_TMP_DIR");
    }

    if (drillTempDir == null) {
      deleteTmpDir = true;
      return DrillFileUtils.createTempDir();
    }

    return new File(drillTempDir);
  }

  /**
   * Copies jar from remote udf area to local udf area.
   *
   * @param jarName jar name to be copied
   * @param remoteFunctionRegistry remote function registry
   * @return local path to jar that was copied
   * @throws IOException in case of problems during jar coping process
   */
  private Path copyJarToLocal(String jarName, RemoteFunctionRegistry remoteFunctionRegistry) throws IOException {
    Path registryArea = remoteFunctionRegistry.getRegistryArea();
    FileSystem fs = remoteFunctionRegistry.getFs();
    Path remoteJar = new Path(registryArea, jarName);
    Path localJar = new Path(localUdfDir, jarName);
    try {
      fs.copyToLocalFile(remoteJar, localJar);
    } catch (IOException e) {
      String message = String.format("Error during jar [%s] coping from [%s] to [%s]",
          jarName, registryArea.toUri().getPath(), localUdfDir.toUri().getPath());
      throw new IOException(message, e);
    }
    return localJar;
  }

  /**
   * Deletes quietly local jar but first checks if path to jar is not null.
   *
   * @param jar path to jar
   */
  private void deleteQuietlyLocalJar(Path jar) {
    if (jar != null) {
      FileUtils.deleteQuietly(new File(jar.toUri().getPath()));
    }
  }

  /**
   * If {@link #deleteTmpDir} is set to true, deletes generated temporary directory.
   * Otherwise cleans up {@link #localUdfDir}.
   */
  @Override
  public void close() {
    localFunctionRegistry.close();
    if (deleteTmpDir) {
      FileUtils.deleteQuietly(tmpDir);
    } else {
      try {
        File localDir = new File(localUdfDir.toUri().getPath());
        if (localDir.exists()) {
          FileUtils.cleanDirectory(localDir);
        }
      } catch (IOException e) {
        logger.warn("Problems during local udf directory clean up", e);
      }
    }
  }

  /**
   * Fires when jar name is submitted for unregistration.
   * Will unregister all functions associated with the jar name
   * and delete binary and source associated with the jar from local udf directory
   */
  private class UnregistrationListener implements TransientStoreListener {

    @Override
    public void onChange(TransientStoreEvent<?> event) {
      String jarName = (String) event.getValue();
      localFunctionRegistry.unregister(jarName);
      String localDir = localUdfDir.toUri().getPath();
      FileUtils.deleteQuietly(new File(localDir, jarName));
      FileUtils.deleteQuietly(new File(localDir, JarUtil.getSourceName(jarName)));
    }
  }
}
