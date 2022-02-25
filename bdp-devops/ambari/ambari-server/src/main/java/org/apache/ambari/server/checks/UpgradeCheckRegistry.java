/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.checks;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.CheckHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.spi.upgrade.CheckQualification;
import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * The {@link UpgradeCheckRegistry} contains the ordered list of all pre-upgrade
 * checks. This will order the checks according to
 * {@link PreUpgradeCheckComparator}.
 * <p>
 * There are two types of checks which can be loaded into this registry:
 * <ul>
 * <li>Built-in: checks which ship with Ambari
 * <li>Plug-in: checks which are specified in the upgrade pack for a stack, and
 * either come from Ambari's checks or from the library classpath of the stack.
 * </ul>
 */
public class UpgradeCheckRegistry {
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCheckRegistry.class);

  @Inject
  private Provider<AmbariMetaInfo> metainfoProvider;

  /**
   * Used for injecting dependencies into plugin upgrade check classes. There
   * are cases where an upgrade pack specifies a built-in check which uses a
   * dependency, such as the Ambari Configuration singleton.
   */
  @Inject
  private Injector m_injector;

  /**
   * The list of upgrade checks provided by the system which are required, as
   * specified by {@link UpgradeCheckInfo#required()}.
   */
  private final Set<UpgradeCheck> m_builtInChecks = new TreeSet<>(
    new PreUpgradeCheckComparator());

  /**
   * Represents the upgrade checks (both those which were able to be loaded and
   * which failed to be loaded) which were discovered in the upgrade pack.
   */
  private final Map<UpgradePack, PluginUpgradeChecks> m_pluginChecks = new HashMap<>();

  /**
   * Register an upgrade check.
   *
   * @param upgradeCheck
   *          the check to register (not {@code null}).
   */
  public void register(UpgradeCheck upgradeCheck) {
    m_builtInChecks.add(upgradeCheck);
  }

  /**
   * Gets a list of all of the built-in upgrade checks.
   *
   * @return
   */
  public List<UpgradeCheck> getBuiltInUpgradeChecks() {
    return new ArrayList<>(m_builtInChecks);
  }

  /**
   * Gets the ordered upgrade checks which should execute for the specified
   * upgrade pack. This list of checks will include required built-in checks
   * from Ambari and plugin checks loaded from the upgrade pack.
   * <p>
   * This collection of upgrade checks will be further scrutinized via
   * {@link CheckQualification}s to determine if they indeed need to run by the
   * {@link CheckHelper}.
   *
   * @param upgradePack
   *          Upgrade pack object with the list of required checks to be
   *          included
   * @return
   */
  public List<UpgradeCheck> getFilteredUpgradeChecks(UpgradePack upgradePack) throws AmbariException {
    List<UpgradeCheck> builtInRequiredChecks = new ArrayList<>();

    // iterate over all of the built-in checks, dropping any which are not
    // required for the upgrade pack's upgrade type
    for (UpgradeCheck builtInCheck : m_builtInChecks) {
      if (isBuiltInCheckRequired(builtInCheck, upgradePack.getType())) {
        builtInRequiredChecks.add(builtInCheck);
      }
    }

    // for any checks defined in the upgrade pack, add them since they have been
    // explicitely defined
    PluginUpgradeChecks pluginChecks = m_pluginChecks.get(upgradePack);

    // see if the upgrade checks have been processes for this pack yet
    if (null == pluginChecks) {
      pluginChecks = new PluginUpgradeChecks(new TreeSet<>(new PreUpgradeCheckComparator()),
          new TreeSet<>());

      m_pluginChecks.put(upgradePack, pluginChecks);

      List<String> pluginCheckClassNames = upgradePack.getPrerequisiteChecks();
      if (null != pluginCheckClassNames && !pluginCheckClassNames.isEmpty()) {
        loadPluginUpgradeChecksFromStack(upgradePack, pluginChecks);
      }
    }

    final Set<UpgradeCheck> combinedUpgradeChecks = new TreeSet<>(new PreUpgradeCheckComparator());
    combinedUpgradeChecks.addAll(builtInRequiredChecks);
    combinedUpgradeChecks.addAll(pluginChecks.m_loadedChecks);

    return new LinkedList<>(combinedUpgradeChecks);
  }

  /**
   * Gets all of the upgrade check classes defined in upgrade packs which were
   * not able to be loaded and instantiated.
   *
   * @return all of the failed upgrade check classes.
   */
  public Set<String> getFailedPluginClassNames() {
    Collection<PluginUpgradeChecks> pluginUpgradeChecks = m_pluginChecks.values();

    return pluginUpgradeChecks.stream()
        .flatMap(plugins -> plugins.m_failedChecks.stream())
        .collect(Collectors.toSet());
  }

  /**
   * Uses the library classloader from the the target stack in order to find any
   * plugin-in {@link UpgradeCheck}s which are declared in the upgrade pack as
   * well as any upgrade checks which are found in the classloader and marked as
   * {@link UpgradeCheckInfo#required()} for this {@link UpgradeType}.
   * <p/>
   * This method uses a {@link Reflections} instance which has been created
   * using only the {@link URL}s which the stack library is comprised of. This
   * means that scanning the path for {@link UpgradeCheck} instances is quick.
   * However, this also means that the {@link ClassLoader} is unable to load
   * classes which are defined in the stack but ship with Ambari's
   * {@link ClassLoader}. For this reason, we must use a different
   * {@link ClassLoader} for loading explicitly defined classes versus those
   * which are discovered by {@link Reflections}.
   *
   * @param upgradePack
   *          the upgrade pack which defines the upgrade check classes.
   * @param pluginChecks
   *          the collection to popoulate.
   */
  private void loadPluginUpgradeChecksFromStack(UpgradePack upgradePack,
      PluginUpgradeChecks pluginChecks) throws AmbariException {
    Set<String> pluginCheckClassNames = new HashSet<>(upgradePack.getPrerequisiteChecks());
    StackId ownerStackId = upgradePack.getOwnerStackId();
    StackInfo stackInfo = metainfoProvider.get().getStack(ownerStackId);

    URLClassLoader classLoader = stackInfo.getLibraryClassLoader();
    if (null != classLoader) {

      // first find all of the plugins which are explicitely defined in the
      // upgrade pack and attempt to load and register them
      for (String pluginCheckClassName : pluginCheckClassNames) {
        try {
          UpgradeCheck upgradeCheck = stackInfo.getLibraryInstance(m_injector,
              pluginCheckClassName);

          pluginChecks.m_loadedChecks.add(upgradeCheck);

          LOG.info("Registered pre-upgrade check {} for stack {}", pluginCheckClassName, ownerStackId);
        } catch (Exception exception) {
          LOG.error("Unable to load the upgrade check {}", pluginCheckClassName, exception);

          // keep track of the failed check
          pluginChecks.m_failedChecks.add(pluginCheckClassName);
        }
      }

      // next find all plugin checks which are required for this upgrade type by
      // scanning just the classes shipped with the stack's library JAR
      Reflections reflections = new Reflections(
          new ConfigurationBuilder()
            .addClassLoader(classLoader)
            .addUrls(classLoader.getURLs())
            .setScanners(new SubTypesScanner(),new TypeAnnotationsScanner()));

      Set<Class<? extends UpgradeCheck>> upgradeChecksFromLoader = reflections.getSubTypesOf(
          UpgradeCheck.class);

      if(null != upgradeChecksFromLoader && !upgradeChecksFromLoader.isEmpty()) {
        for (Class<? extends UpgradeCheck> clazz : upgradeChecksFromLoader) {
          // first check to make sure we didn't already try to load this one if it
          // was explicitely defined in the upgrade pack (from above)
          if (pluginCheckClassNames.contains(clazz.getName())) {
            continue;
          }

          // see if this check required by inspecting the annotation
          UpgradeCheckInfo upgradeCheckInfo = clazz.getAnnotation(UpgradeCheckInfo.class);
          if (null != upgradeCheckInfo && ArrayUtils.contains(upgradeCheckInfo.required(), upgradePack.getType())) {
            // if the annotation says the check is required, then load it
            try {
              pluginChecks.m_loadedChecks.add(clazz.newInstance());

              LOG.info("Registered pre-upgrade check {} for stack {}", clazz, ownerStackId);
            } catch (Exception exception) {
              LOG.error("Unable to load the upgrade check {}", clazz, exception);

              // keep track of the failed check
              pluginChecks.m_failedChecks.add(clazz.getName());
            }
          }
        }
      }
    } else {
      LOG.error(
          "Unable to perform the following upgrade checks because no libraries could be loaded for the {} stack: {}",
          ownerStackId, StringUtils.join(pluginCheckClassNames, ", "));

      pluginChecks.m_failedChecks.addAll(pluginCheckClassNames);
    }
  }

  /**
   * Gets whether a built-in upgrade check is required to run.
   */
  private boolean isBuiltInCheckRequired(UpgradeCheck upgradeCheck, UpgradeType upgradeType) {
    if (upgradeType == null) {
      return true;
    }

    UpgradeCheckInfo annotation = upgradeCheck.getClass().getAnnotation(UpgradeCheckInfo.class);
    if (null == annotation) {
      return false;
    }

    UpgradeType[] upgradeTypes = annotation.required();
    return ArrayUtils.contains(upgradeTypes, upgradeType);
  }

  /**
   * THe {@link PreUpgradeCheckComparator} class is used to compare
   * {@link UpgradeCheck} based on their {@link UpgradeCheck}
   * annotations.
   */
  private static final class PreUpgradeCheckComparator implements
      Comparator<UpgradeCheck> {

    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(UpgradeCheck check1, UpgradeCheck check2) {
      Class<? extends UpgradeCheck> clazz1 = check1.getClass();
      Class<? extends UpgradeCheck> clazz2 = check2.getClass();

      UpgradeCheckInfo annotation1 = clazz1.getAnnotation(UpgradeCheckInfo.class);
      UpgradeCheckInfo annotation2 = clazz2.getAnnotation(UpgradeCheckInfo.class);

      UpgradeCheckGroup group1 = UpgradeCheckGroup.DEFAULT;
      UpgradeCheckGroup group2 = UpgradeCheckGroup.DEFAULT;
      Float groupOrder1 = Float.valueOf(group1.getOrder());
      Float groupOrder2 = Float.valueOf(group2.getOrder());

      Float order1 = 1.0f;
      Float order2 = 1.0f;

      if (null != annotation1) {
        group1 = annotation1.group();
        groupOrder1 = Float.valueOf(group1.getOrder());
        order1 = Float.valueOf(annotation1.order());
      }

      if (null != annotation2) {
        group2 = annotation2.group();
        groupOrder2 = Float.valueOf(group2.getOrder());
        order2 = Float.valueOf(annotation2.order());
      }

      int groupComparison = groupOrder1.compareTo(groupOrder2);
      if (groupComparison != 0) {
        return groupComparison;
      }

      int orderComparison = order1.compareTo(order2);
      if (orderComparison != 0) {
        return orderComparison;
      }

      return clazz1.getName().compareTo(clazz2.getName());
    }
  }

  /**
   * Encapsulates information about which plugins could and could not be loaded
   * from a stack.
   */
  public static final class PluginUpgradeChecks {
    /**
     * The upgrade checks discovered on a per-stack, per-upgrade pack basis.
     */
    private final Set<UpgradeCheck> m_loadedChecks;

    /**
     * The upgrade checks discovered on a per-stack, per-upgrade pack basis
     * which could not be loaded from the stack's 3rd party library
     * {@link ClassLoader}.
     */
    private final Set<String> m_failedChecks;

    /**
     * Constructor.
     *
     * @param loadedChecks
     *          the checks which have been successfully loaded from the stack.
     * @param failedChecks
     *          the checks which failed to load from the stack and cannot run.
     */
    private PluginUpgradeChecks(Set<UpgradeCheck> loadedChecks, Set<String> failedChecks) {
      m_loadedChecks = loadedChecks;
      m_failedChecks = failedChecks;
    }

  }
}
