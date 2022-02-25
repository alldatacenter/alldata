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

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.ambari.annotations.UpgradeCheckInfo;
import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.DesiredConfig;
import org.apache.ambari.spi.upgrade.UpgradeCheckDescription;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeCheckType;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.apache.commons.lang.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Singleton;

/**
 * Checks for properties that contain hardcoded CURRENT hdp version string.
 * Presence of such properties usually means that some paths are hardcoded to
 * point to concrete version of HDP, instead of pointing to current symlink.
 * That is a potential problem when doing stack update.
 */
@Singleton
@UpgradeCheckInfo(
    order = 98.0f,
    required = { UpgradeType.ROLLING, UpgradeType.NON_ROLLING, UpgradeType.HOST_ORDERED })
public class HardcodedStackVersionPropertiesCheck extends ClusterCheck {

  static final UpgradeCheckDescription HARDCODED_STACK_VERSION_PROPERTIES_CHECK = new UpgradeCheckDescription("HARDCODED_STACK_VERSION_PROPERTIES_CHECK",
      UpgradeCheckType.CLUSTER,
      "Found hardcoded stack version in property value.",
      new ImmutableMap.Builder<String, String>()
        .put(UpgradeCheckDescription.DEFAULT,
            "Some properties seem to contain hardcoded stack version string \"%s\"." +
            " That is a potential problem when doing stack update.").build());

  public HardcodedStackVersionPropertiesCheck() {
    super(HARDCODED_STACK_VERSION_PROPERTIES_CHECK);
  }

  @Override
  public UpgradeCheckResult perform(UpgradeCheckRequest request)
      throws AmbariException {
    UpgradeCheckResult result = new UpgradeCheckResult(this);

    Cluster cluster = clustersProvider.get().getCluster(request.getClusterName());

    Set<String> versions = new HashSet<>();
    Set<String> failures = new HashSet<>();
    Set<String> failedVersions = new HashSet<>();

    Map<String, DesiredConfig> desiredConfigs = cluster.getDesiredConfigs();
    for (Entry<String, DesiredConfig> configEntry : desiredConfigs.entrySet()) {
      String configType = configEntry.getKey();
      DesiredConfig desiredConfig = configEntry.getValue();
      final Config config = cluster.getConfig(configType, desiredConfig.getTag());

      Map<String, String> properties = config.getProperties();
      for (Entry<String, String> property : properties.entrySet()) {

        // !!! this code is already iterating every config property, so an extra loop for the small-ish
        // numbers of repository versions won't add that much more overhead
        for (String version : versions) {
          Pattern searchPattern = getHardcodeSearchPattern(version);
          if (stringContainsVersionHardcode(property.getValue(), searchPattern)) {
            failedVersions.add(version);
            failures.add(String.format("%s/%s found a hardcoded value %s",
              configType, property.getKey(), version));
          }
        }
      }
    }

    if (failures.size() > 0) {
      result.setStatus(UpgradeCheckStatus.WARNING);
      String failReason = getFailReason(result, request);

      result.setFailReason(String.format(failReason, StringUtils.join(failedVersions, ',')));
      result.setFailedOn(new LinkedHashSet<>(failures));

    } else {
      result.setStatus(UpgradeCheckStatus.PASS);
    }

    return result;
  }

  /**
   * Returns pattern that looks for hdp version hardcoded occurences, except
   * those that start with "-Dhdp.version=" string
   * @param hdpVersion hdp version to search for
   * @return compiled pattern
   */
  public static Pattern getHardcodeSearchPattern(String hdpVersion) {
    // Only things like -Dhdp.version=2.3.4.0-1234 are allowed at hardcode
    return Pattern.compile("(?<!-Dhdp\\.version=)" + hdpVersion.replace(".", "\\."));
  }

  /**
   * Looks for search pattern in string
   * @param string string to look into
   * @param searchPattern compiled regex
   * @return true if string contains pattern
   */
  public static boolean stringContainsVersionHardcode(String string, Pattern searchPattern) {
    Matcher matcher = searchPattern.matcher(string);
    return matcher.find();
  }

}
