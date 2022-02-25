/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.ldap.service.ads.detectors;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.ambari.server.ldap.service.AttributeDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for attribute detector chains.
 */
@Singleton
public class AttributeDetectorFactory {

  private static final Logger LOG = LoggerFactory.getLogger(AttributeDetectorFactory.class);
  private static final String USER_ATTRIBUTES_DETECTORS = "UserAttributesDetectors";
  private static final String GROUP_ATTRIBUTES_DETECTORS = "GroupAttributesDetectors";
  /**
   * The set of group attribute detectors, configured by GUICE (check the relevant guice module implementation)
   */
  @Inject
  @Named(GROUP_ATTRIBUTES_DETECTORS)
  Set<AttributeDetector> groupAttributeDetectors;
  /**
   * The set of user attribute detectors, configured by GUICE (check the relevant guice module implementation)
   */
  @Inject
  @Named(USER_ATTRIBUTES_DETECTORS)
  private Set<AttributeDetector> userAttributeDetectors;

  @Inject
  public AttributeDetectorFactory() {
  }

  /**
   * Creates a chained attribute detector instance with user attribute detectors
   *
   * @return the constructed ChainedAttributeDetector instance
   */
  public ChainedAttributeDetector userAttributDetector() {
    LOG.info("Creating instance with user attribute detectors: [{}]", userAttributeDetectors);
    return new ChainedAttributeDetector(userAttributeDetectors);
  }

  /**
   * Creates a chained attribute detector instance with user attribute detectors
   *
   * @return the constructed ChainedAttributeDetector instance
   */

  public ChainedAttributeDetector groupAttributeDetector() {
    LOG.info("Creating instance with group attribute detectors: [{}]", groupAttributeDetectors);
    return new ChainedAttributeDetector(groupAttributeDetectors);
  }


}
