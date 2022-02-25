/**
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

import java.util.Set;

import org.apache.ambari.spi.upgrade.UpgradeCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;

/**
 * The {@link UpgradeCheckRegistryProvider} is a provider which binds any
 * requests for the {@link UpgradeCheckRegistry}. When first requested, it will
 * perform a classpath scan looking for instances of {@link UpgradeCheck} and
 * then return an initialized {@link UpgradeCheckRegistry}.
 */
public class UpgradeCheckRegistryProvider implements Provider<UpgradeCheckRegistry> {

  /**
   * Logger.
   */
  private static final Logger LOG = LoggerFactory.getLogger(UpgradeCheckRegistryProvider.class);

  /**
   * Used to injecting members into the {@link UpgradeCheck} instances which are
   * discovered.
   */
  @Inject
  private Injector m_injector;

  /**
   * The singleton registry to inject with found checks.
   */
  private UpgradeCheckRegistry m_checkRegistry;

  /**
   * The scanned instances of {@link UpgradeCheck} from the classpath.
   */
  private Set<BeanDefinition> m_beanDefinitions = null;

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckRegistry get() {
    if (null == m_beanDefinitions || m_beanDefinitions.isEmpty()) {
      String packageName = ClusterCheck.class.getPackage().getName();
      LOG.info("Searching package {} for classes matching {}", packageName, UpgradeCheck.class);

      ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);

      // match all implementations of the base check class
      AssignableTypeFilter filter = new AssignableTypeFilter(UpgradeCheck.class);
      scanner.addIncludeFilter(filter);

      m_beanDefinitions = scanner.findCandidateComponents(packageName);
    }

    // no checks is a problem
    if (null == m_beanDefinitions || m_beanDefinitions.size() == 0) {
      LOG.error("No instances of {} found to register", UpgradeCheck.class);
      return null;
    }

    m_checkRegistry = new UpgradeCheckRegistry();
    m_injector.injectMembers(m_checkRegistry);

    // for every discovered check, singleton-ize them and register with the
    // registry
    for (BeanDefinition beanDefinition : m_beanDefinitions) {
      String className = beanDefinition.getBeanClassName();
      Class<?> clazz = ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader());

      try {
        UpgradeCheck upgradeCheck = (UpgradeCheck) m_injector.getInstance(clazz);
        m_checkRegistry.register(upgradeCheck);

        LOG.info("Registered pre-upgrade check {}", upgradeCheck.getClass());
      } catch (Exception exception) {
        LOG.error("Unable to bind and register upgrade check {}", clazz, exception);
      }
    }

    return m_checkRegistry;
  }
}
