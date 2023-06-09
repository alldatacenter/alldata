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
package org.apache.drill.common.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigMergeable;
import com.typesafe.config.ConfigRenderOptions;
import io.netty.util.internal.PlatformDependent;
import org.apache.drill.common.exceptions.DrillConfigurationException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.reflections.util.ClasspathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class DrillConfig extends NestedConfig {
  private static final Logger logger = LoggerFactory.getLogger(DrillConfig.class);

  private final ImmutableList<String> startupArguments;

  private static final long MAX_DIRECT_MEMORY = PlatformDependent.maxDirectMemory();

  @VisibleForTesting
  public DrillConfig(Config config) {
    super(config);
    logger.debug("Setting up DrillConfig object.");
    // we need to exclude sun.java.command config node while logging, because
    // it contains user password along with other parameters
    logger.trace("Given Config object is:\n{}", config.withoutPath("password").withoutPath("sun.java.command")
                 .root().render(ConfigRenderOptions.defaults()));
    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
    this.startupArguments = ImmutableList.copyOf(bean.getInputArguments());
    logger.debug("DrillConfig object initialized.");
  }

  /**
   * Get an instance of the provided interface using the configuration path provided. Construct the object based on the
   * provided constructor arguments.
   * @param path
   *          The configuration path to use.
   * @param iface
   *          The Interface or Superclass of the instance you requested.
   * @param constructorArgs
   *          Any arguments required for constructing the requested type.
   * @return The new Object instance that implements the provided Interface
   */
  @SuppressWarnings("unchecked")
  public <T> T getInstance(String path, Class<T> iface, Object... constructorArgs) {
    try{
      String className = this.getString(path);
      Class<?> clazz = Class.forName(className);
      Preconditions.checkArgument(iface.isAssignableFrom(clazz));
      Class<?>[] argClasses = new Class[constructorArgs.length];
      for (int i = 0; i < constructorArgs.length; i++) {
        argClasses[i] = constructorArgs[i].getClass();
      }
      Constructor<?> constructor = clazz.getConstructor(argClasses);
      return (T) constructor.newInstance(constructorArgs);
    }catch(Exception e){
      throw UserException.unsupportedError(e)
          .message("Failure while attempting to load instance of the class of type %s requested at path %s.",
              iface.getName(), path).build(logger);
    }
  }

  public List<String> getStartupArguments() {
    return startupArguments;
  }

  /**
   * Creates a DrillConfig object using the default config file name
   * and with server-specific configuration options enabled.
   * @return The new DrillConfig object.
   */
  public static DrillConfig create() {
    return create(null, true);
  }

  /**
   * Creates a {@link DrillConfig configuration} using the default config file
   * name and with server-specific configuration options disabled.
   *
   * @return {@link DrillConfig} instance
   */
  public static DrillConfig forClient() {
    return create(null, false);
  }

  /**
   * DrillConfig loads up Drill configuration information. It does this utilizing a combination of classpath scanning
   * and Configuration fallbacks provided by the TypeSafe configuration library. The order of precedence is as
   * follows:
   * <p>
   * Configuration values are retrieved as follows:
   * <ul>
   * <li>Check a single copy of "drill-override.conf".  If multiple copies are
   *     on the classpath, which copy is read is indeterminate.
   *     If a non-null value for overrideFileResourcePathname is provided, this
   *     is used instead of "{@code drill-override.conf}".</li>
   * <li>Check a single copy of "drill-distrib.conf". If multiple copies are
   *     on the classpath, which copy is read is indeterminate. </li>
   * <li>Check all copies of "{@code drill-module.conf}".  Loading order is
   *     indeterminate.</li>
   * <li>Check a single copy of "{@code drill-default.conf}".  If multiple
   *     copies are on the classpath, which copy is read is indeterminate.</li>
   * </ul>
   *
   * </p>
   * @param overrideFileResourcePathname
   *          the classpath resource pathname of the file to use for
   *          configuration override purposes; {@code null} specifies to use the
   *          default pathname ({@link ConfigConstants#CONFIG_OVERRIDE_RESOURCE_PATHNAME}) (does
   *          <strong>not</strong> specify to suppress trying to load an
   *          overrides file)
   *  @return A merged Config object.
   */
  public static DrillConfig create(String overrideFileResourcePathname) {
    return create(overrideFileResourcePathname, true);
  }

  /**
   * <b><u>Do not use this method outside of test code.</u></b>
   */
  @VisibleForTesting
  public static DrillConfig create(Properties testConfigurations) {
    return create(null, testConfigurations, true, new DrillExecConfigFileInfo(), null);
  }

  /**
   * @param overrideFileResourcePathname
   *          see {@link #create(String)}'s {@code overrideFileResourcePathname}
   */
  public static DrillConfig create(String overrideFileResourcePathname, boolean enableServerConfigs) {
    return create(overrideFileResourcePathname, null, enableServerConfigs, new DrillExecConfigFileInfo(), null);
  }

  /**
   * Merged DrillConfig object for all the RM Configurations provided through various resource files. The order of
   * precedence is as follows:
   * <p>
   * Configuration values are retrieved as follows:
   * <ul>
   * <li>Check a single copy of "drill-rm-override.conf".  If multiple copies are
   *     on the classpath, which copy is read is indeterminate.</li>
   * <li>Check a single copy of "drill-rm-distrib.conf". If multiple copies are
   *     on the classpath, which copy is read is indeterminate. </li>
   * <li>Check a single copy of "{@code drill-rm-default.conf}".  If multiple
   *     copies are on the classpath, which copy is read is indeterminate.</li>
   * </ul>
   * </p>
   *  @return A merged Config object.
   */
  public static DrillConfig createForRM() {
    return create(null, null, true, new DrillRMConfigFileInfo(), null);
  }

  /**
   * Creates a drill configuration using the provided config file.
   * @param config custom configuration file
   * @return {@link DrillConfig} instance
   */
  public static DrillConfig create(Config config) {
    return new DrillConfig(config.resolve());
  }

  /**
   * @param overrideFileResourcePathname
   *          see {@link #create(String)}'s {@code overrideFileResourcePathname}
   * @param overriderProps
   *          optional property map for further overriding (after override file
   *          is assimilated
   * @param enableServerConfigs
   *          whether to enable server-specific configuration options
   * @param configInfo
   *          see {@link ConfigFileInfo}
   * @param fallbackConfig
   *          existing config which will be used as fallback
   * @return {@link DrillConfig} object with all configs from passed in resource files
   */
  public static DrillConfig create(String overrideFileResourcePathname,
                                   Properties overriderProps,
                                   boolean enableServerConfigs,
                                   ConfigFileInfo configInfo,
                                   ConfigMergeable fallbackConfig) {
    final StringBuilder logString = new StringBuilder();
    final Stopwatch watch = Stopwatch.createStarted();
    overrideFileResourcePathname = overrideFileResourcePathname == null ?
      configInfo.getOverrideFileName() : overrideFileResourcePathname;

    // 1. Load defaults configuration file.
    Config fallback = ConfigFactory.empty();
    final ClassLoader[] classLoaders = ClasspathHelper.classLoaders();
    for (ClassLoader classLoader : classLoaders) {
      final URL url =
          classLoader.getResource(configInfo.getDefaultFileName());
      if (null != url) {
        logString.append("Base Configuration:\n\t- ").append(url).append("\n");
        fallback =
            ConfigFactory.load(classLoader, configInfo.getDefaultFileName());
        break;
      }
    }

    // 2. Load per-module configuration files.
    final String perModuleResourcePathName = configInfo.getModuleFileName();
    final Collection<URL> urls = (perModuleResourcePathName != null) ?
      ClassPathScanner.getConfigURLs(perModuleResourcePathName) : new ArrayList<>();
    logString.append("\nIntermediate Configuration and Plugin files, in order of precedence:\n");
    for (URL url : urls) {
      logString.append("\t- ").append(url).append("\n");
      fallback = ConfigFactory.parseURL(url).withFallback(fallback);
    }
    logString.append("\n");

    // Add fallback config for default and module configuration
    if (fallbackConfig != null) {
      fallback = fallback.withFallback(fallbackConfig);
    }

    final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

    // 3. Load distribution specific configuration file.
    final URL distribConfigFileUrl = classLoader.getResource(configInfo.getDistributionFileName());
    if (distribConfigFileUrl != null) {
      logString.append("Distribution Specific Configuration File: ").append(distribConfigFileUrl).append("\n");
    }
    fallback =
      ConfigFactory.load(configInfo.getDistributionFileName()).withFallback(fallback);

    // 4. Load any specified overrides configuration file along with any
    //    overrides from JVM system properties (e.g., {-Dname=value").

    // (Per ConfigFactory.load(...)'s mention of using Thread.getContextClassLoader():)
    final URL overrideFileUrl = classLoader.getResource(overrideFileResourcePathname);
    if (overrideFileUrl != null) {
      logString.append("Override File: ").append(overrideFileUrl).append("\n");
    }
    Config effectiveConfig =
        ConfigFactory.load(overrideFileResourcePathname).withFallback(fallback);

    // 5. Apply any overriding properties.
    if (overriderProps != null) {
      logString.append("Overridden Properties:\n");
      for(Entry<Object, Object> entry : overriderProps.entrySet()){
        if (!entry.getKey().equals("password")) {
          logString.append("\t-").append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }
      }
      logString.append("\n");
      effectiveConfig =
          ConfigFactory.parseProperties(overriderProps).withFallback(effectiveConfig);
    }

    // 6. Create DrillConfig object from Config object.
    logger.info("Configuration and plugin file(s) identified in {}ms.\n{}",
        watch.elapsed(TimeUnit.MILLISECONDS),
        logString);
    return new DrillConfig(effectiveConfig.resolve());
  }

  private <T> Class<T> getClassAt(String location, Class<T> clazz) throws DrillConfigurationException {
    final String className = getString(location);
    if (className == null) {
      throw new DrillConfigurationException(String.format(
          "No class defined at location '%s'. Expected a definition of the class [%s]",
          location, clazz.getCanonicalName()));
    }

    try {
      final Class<?> c = Class.forName(className);
      if (clazz.isAssignableFrom(c)) {
        @SuppressWarnings("unchecked")
        final Class<T> t = (Class<T>) c;
        return t;
      }

      throw new DrillConfigurationException(String.format("The class [%s] listed at location '%s' should be of type [%s].  It isn't.", className, location, clazz.getCanonicalName()));
    } catch (Exception ex) {
      if (ex instanceof DrillConfigurationException) {
        throw (DrillConfigurationException) ex;
      }
      throw new DrillConfigurationException(String.format("Failure while initializing class [%s] described at configuration value '%s'.", className, location), ex);
    }
  }

  public <T> T getInstanceOf(String location, Class<T> clazz) throws DrillConfigurationException{
    final Class<T> c = getClassAt(location, clazz);
    try {
      return c.newInstance();
    } catch (Exception ex) {
      throw new DrillConfigurationException(String.format("Failure while instantiating class [%s] located at '%s.", clazz.getCanonicalName(), location), ex);
    }
  }

  @Override
  public String toString() {
    return this.root().render();
  }

  public static long getMaxDirectMemory() {
    return MAX_DIRECT_MEMORY;
  }
}
