/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.base.packages;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.JsonSerializer;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Function;

@Slf4j
public class PackageManager {

  private static PackageManager instance;

  /**
   * Mapping for the plugin with plugin's classloader
   */
  private final Map<String, URLClassLoader> pluginClassLoader = new ConcurrentHashMap<>();
  private final Set<String> loadedPluginLibs = new ConcurrentSkipListSet<>();

  private final ExecutionEnviron environ;
  private final PluginManager pluginManager;
  private final boolean printClassLoaderUrls;
  private final boolean enableDynamicLoader;

  private PackageManager(PackageManagerParam param) {
    environ = param.getEnviron();
    enableDynamicLoader = param.getEnableDynamicLoader();
    printClassLoaderUrls = param.getPrintClassLoaderUrls();

    String path;
    if (StringUtils.isNotEmpty(param.getRootPath())) {
      path = param.getRootPath();
    } else {
      try {
        path = Paths.get(this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
      } catch (Exception e) {
        throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, e);
      }
    }

    pluginManager = PluginManager.builder()
        .dynamicLoad(param.enableDynamicLoader)
        .dryRun(param.getDryRun())
        .path(Paths.get(path))
        .pluginLibDir(param.getPluginLibDir())
        .pluginConfDir(param.getPluginConfDir())
        .build();
  }

  public static PackageManager getInstance(ExecutionEnviron environ, BitSailConfiguration conf) {
    boolean dryRun = conf.get(CommonOptions.DRY_RUN);
    boolean printClassLoaderUrls = conf.get(CommonOptions.PRINT_LOADED_URLS);
    boolean dynamicLoadEnabled = conf.get(CommonOptions.ENABLE_DYNAMIC_LOADER);
    String pluginLibDir = conf.get(CommonOptions.JOB_PLUGIN_LIB_PATH);
    String pluginConfDir = conf.get(CommonOptions.JOB_PLUGIN_CONF_PATH);
    String rootPath = conf.get(CommonOptions.JOB_PLUGIN_ROOT_PATH);

    PackageManagerParam param = PackageManagerParam
        .builder()
        .environ(environ)
        .dryRun(dryRun)
        .printClassLoaderUrls(printClassLoaderUrls)
        .pluginLibDir(pluginLibDir)
        .pluginConfDir(pluginConfDir)
        .rootPath(rootPath)
        .enableDynamicLoader(dynamicLoadEnabled).build();
    return getInstance(param);
  }

  public static PackageManager getInstance(PackageManagerParam param) {
    if (instance == null) {
      synchronized (PackageManager.class) {
        if (instance == null) {
          instance = new PackageManager(param);
        }
      }
    }
    return instance;
  }

  public <R> R loadDynamicLibrary(String name, Function<ClassLoader, R> function) {
    ClassLoader classLoader = retrieveClassLoader(name);
    return callbackAndReset(function, classLoader);
  }

  private <R> R callbackAndReset(Function<ClassLoader, R> function, ClassLoader newClassLoader) {
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(newClassLoader);
    try {
      return function.apply(newClassLoader);
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  private void printClassLoaderUrls(ClassLoader cl) {
    log.info("classloader:" + cl);
    while (cl instanceof URLClassLoader) {
      log.info("current classloader:" + cl);
      for (URL url : ((URLClassLoader) cl).getURLs()) {
        log.info("load url:" + cl + ": " + url);
      }
      cl = cl.getParent();
    }
  }

  @SneakyThrows
  // classLoader for dynamic library
  private URLClassLoader retrieveClassLoader(String pluginClassName) {
    URLClassLoader classLoader = (URLClassLoader) Thread.currentThread().getContextClassLoader();
    if (!this.enableDynamicLoader) {
      return classLoader;
    }
    List<URL> dynamicLib = pluginManager.getPluginLibs(pluginClassName);
    log.info("Get dynamic lib " + JSONObject.toJSONString(dynamicLib));
    String pluginClassLoaderKey = pluginManager.getPluginClassNameToBitSailIoMap().getOrDefault(pluginClassName, pluginClassName);
    URLClassLoader urlClassLoader = pluginClassLoader.get(pluginClassLoaderKey);
    if (urlClassLoader != null) {
      return urlClassLoader;
    }
    synchronized (this) {
      registerJars(this.environ, dynamicLib);
      URLClassLoader returnClassLoader;
      returnClassLoader = classLoader;
      Class urlClass = URLClassLoader.class;
      Method method = urlClass.getDeclaredMethod("addURL", new Class[] {URL.class});
      method.setAccessible(true);
      for (URL pluginUrl : dynamicLib) {
        if (!loadedPluginLibs.contains(pluginUrl.getPath())) {
          loadedPluginLibs.add(pluginUrl.getPath());
          method.invoke(returnClassLoader, pluginUrl);
        }
      }
      if (this.printClassLoaderUrls) {
        printClassLoaderUrls(returnClassLoader);
      }
      pluginClassLoader.put(pluginClassLoaderKey, returnClassLoader);
      return returnClassLoader;
    }
  }

  private void registerJars(ExecutionEnviron environ, List<URL> dynamicLibraries) throws URISyntaxException {
    List<URI> pendingAddLibraries = Lists.newArrayList();
    for (URL library : dynamicLibraries) {
      if (!loadedPluginLibs.contains(library.getPath())) {
        pendingAddLibraries.add(library.toURI());
      }
    }

    log.info("Register new libraries =  {}.", JsonSerializer.serialize(pendingAddLibraries));
    environ.registerLibraries(pendingAddLibraries);
  }

  @Builder
  @AllArgsConstructor
  @Getter
  public static class PackageManagerParam {
    private final ExecutionEnviron environ;
    private final Boolean dryRun;
    private final Boolean printClassLoaderUrls;
    private final String pluginLibDir;
    private final String pluginConfDir;
    private final Boolean enableDynamicLoader;
    private final String rootPath;
  }
}
