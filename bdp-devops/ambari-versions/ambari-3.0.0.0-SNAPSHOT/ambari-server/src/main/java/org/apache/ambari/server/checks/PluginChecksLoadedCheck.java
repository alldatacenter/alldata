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

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckGroup;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

/**
 * The {@link PluginChecksLoadedCheck} class is used to display warnings if
 * there were any stack plugin checks which were not able to be loaded.
 */
@Singleton
@UpgradeCheckInfo(
    group = UpgradeCheckGroup.INFORMATIONAL_WARNING,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class PluginChecksLoadedCheck extends ClusterCheck {

  private static final UpgradeCheckDescription PLUGIN_CHECK_LOAD_FAILURE = new UpgradeCheckDescription(
      "PLUGIN_CHECK_LOAD_FAILURE", UpgradeCheckType.CLUSTER, "Plugin Upgrade Checks",
      new ImmutableMap.Builder<String, String>().put(UpgradeCheckDescription.DEFAULT,
          "The following upgrade checks could not be loaded and were not run. "
              + "Although this will not prevent your ability to upgrade, it is advised that you "
              + "correct these checks before proceeding.").build());

  @Inject
  Provider<UpgradeCheckRegistry> m_upgradeCheckRegistryProvider;

  /**
   * Constructor.
   */
  public PluginChecksLoadedCheck() {
    super(PLUGIN_CHECK_LOAD_FAILURE);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request) throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this, UpgradeCheckStatus.PASS);

    // get the fully-qualified class names
    Set<String> failedPluginClasses = m_upgradeCheckRegistryProvider.get().getFailedPluginClassNames();

    // quick return a pass
    if (null == failedPluginClasses || failedPluginClasses.isEmpty()) {
      return result;
    }

    // strip out the package name for readability
    Set<FailedPluginClassDetail> failedPluginSimpleClasses = failedPluginClasses.stream()
        .map(FailedPluginClassDetail::new)
        .collect(Collectors.toSet());

    result.setStatus(UpgradeCheckStatus.WARNING);
    result.getFailedDetail().addAll(failedPluginSimpleClasses);
    result.setFailReason(getFailReason(result, request));

    result.getFailedOn().addAll(failedPluginSimpleClasses.stream()
        .map(detail -> detail.toSimpleString())
        .collect(Collectors.toSet()));

    return result;
  }

  /**
   * Used to represent serializable structured information about plugin upgrade
   * checks which could not load.
   */
  static class FailedPluginClassDetail {
    final String m_fullyQualifiedClass;

    @JsonProperty("package_name")
    final String m_packageName;

    @JsonProperty("class_name")
    final String m_className;

    FailedPluginClassDetail(String fullyQualifiedClass) {
      m_fullyQualifiedClass = fullyQualifiedClass;

      int indexOfLastDot = fullyQualifiedClass.lastIndexOf('.');
      if(indexOfLastDot >= 0) {
        m_packageName = fullyQualifiedClass.substring(0, indexOfLastDot);
        m_className = fullyQualifiedClass.substring(indexOfLastDot + 1);
      } else {
        m_packageName = "";
        m_className = fullyQualifiedClass;
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return m_fullyQualifiedClass;
    }

    /**
     * {@inheritDoc}
     */
    public String toSimpleString() {
      return m_className;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return Objects.hash(m_packageName, m_className);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }

      if (obj == null) {
        return false;
      }

      if (getClass() != obj.getClass()) {
        return false;
      }

      FailedPluginClassDetail other = (FailedPluginClassDetail) obj;
      return Objects.equals(m_packageName, other.m_packageName)
          && Objects.equals(m_className, other.m_className);
    }
  }
}
