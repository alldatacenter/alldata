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

package org.apache.ambari.server.controller.internal;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.crypto.Cipher;

import org.apache.ambari.server.controller.RootComponent;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.PropertyProvider;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RootServiceComponentPropertyProvider is a PropertyProvider implementation providing additional
 * properties for RootServiceComponent resources, like AMBARI_SERVER and AMBARI_AGENT.
 * <p/>
 * This implementation conditionally calculates and returns cipher and JCE details upon request.
 * The Cipher data is cached after the first request for it and held in memory until Ambari is
 * restarted.  The user is required to explicitly query for one or both of the following properties
 * (or fields) to get access to the data:
 * <ul>
 * <li>RootServiceComponents/jce_policy</li>
 * <li>RootServiceComponents/ciphers</li>
 * </ul>
 * <p/>
 * For example:
 * <ul>
 * <li><code>GET /api/v1/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/ciphers</code></li>
 * <li><code>GET /api/v1/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/jce_policy</code></li>
 * <li><code>GET /api/v1/services/AMBARI/components/AMBARI_SERVER?fields=RootServiceComponents/jce_policy,RootServiceComponents/ciphers</code></li>
 * </ul>
 * <p/>
 * When querying for ciphers the returned data is a listing if installed cipher algorithms and their
 * maximum key lengths. If all maximum key lengths are <code>2147483647</code>, then the unlimited
 * key JCE policy is installed, else each item will have some smaller value (in bytes).
 * <p/>
 * For example:
 * <pre>
 * "ciphers" : {
 *   "sunjce.aes" : 2147483647,
 *   "sunjce.aeswrap" : 2147483647,
 *   "sunjce.aeswrap_128" : 2147483647,
 *   "sunjce.aeswrap_192" : 2147483647,
 *   "sunjce.aeswrap_256" : 2147483647,
 *   "sunjce.arcfour" : 2147483647,
 *   "sunjce.blowfish" : 2147483647,
 *   ...
 * }
 * </pre>
 * <p/>
 * When querying for the JDC policy, the returned data is a structure with details about the JCE
 * policy - namely whether the unlimited key length policy is installed or not.
 * <p/>
 * For example:
 * <pre>
 * "jce_policy" : {
 *   "unlimited_key" : true
 * }
 * </pre>
 */
public class RootServiceComponentPropertyProvider extends BaseProvider implements PropertyProvider {
  public static final String JCE_POLICY_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceComponents", "jce_policy");

  public static final String CIPHER_PROPERTIES_PROPERTY_ID = PropertyHelper
      .getPropertyId("RootServiceComponents", "ciphers");

  private static final Set<String> SUPPORTED_PROPERTY_IDS;

  private final static Logger LOG = LoggerFactory.getLogger(RootServiceComponentPropertyProvider.class);

  static {
    Set<String> propertyIds = new HashSet<>();
    propertyIds.add(JCE_POLICY_PROPERTY_ID);
    propertyIds.add(CIPHER_PROPERTIES_PROPERTY_ID);
    SUPPORTED_PROPERTY_IDS = Collections.unmodifiableSet(propertyIds);
  }

  /**
   * Map of cipher algorithm names to maximum key lengths.
   * <p/>
   * This is cached in memory after the first time it is needed.
   */
  private static final Map<String, Integer> CACHED_CIPHER_MAX_KEY_LENGTHS = new HashMap<>();

  /**
   * Constructor
   */
  public RootServiceComponentPropertyProvider() {
    super(SUPPORTED_PROPERTY_IDS);
  }

  @Override
  public Set<Resource> populateResources(Set<Resource> resources, Request request, Predicate predicate) throws SystemException {

    Set<String> requestedIds = request.getPropertyIds();

    for (Resource resource : resources) {
      // If this resource represents the AMBARI_SERVER component, handle it's specific properties...
      if (RootComponent.AMBARI_SERVER.name().equals(resource.getPropertyValue(RootServiceComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID))) {
        // Attempt to fill in the cipher details only if explicitly asked for.
        if (requestedIds.contains(JCE_POLICY_PROPERTY_ID) || requestedIds.contains(CIPHER_PROPERTIES_PROPERTY_ID)) {
          setCipherDetails(resource, requestedIds);
        }
      }
    }

    return resources;
  }

  /**
   * Retrieve details about the active JCE policy and installed ciphers, then set the resource
   * properties for the relevant resource.
   * <p/>
   * The following properties are set:
   * <dl>
   * <dt>RootServiceComponents/jce_policy/unlimited_key</dt>
   * <dd>true if the unlimited key JCE policy is detected; false if it is not detected; null if unknown</dd>
   * <dt>RootServiceComponents/ciphers</dt>
   * <dd>A list of installed ciphers and their maximum key length values</dd>
   * </dl>
   *
   * @param resource     the resource to update
   * @param requestedIds the request ids to populate
   */
  private void setCipherDetails(Resource resource, Set<String> requestedIds) {
    // Lazily fill the cache on first request....
    synchronized (CACHED_CIPHER_MAX_KEY_LENGTHS) {
      if (CACHED_CIPHER_MAX_KEY_LENGTHS.isEmpty()) {
        // Get the list of cipher algorithms and determine maximum key lengths. Report as a resource property.
        for (Provider provider : Security.getProviders()) {
          String providerName = provider.getName();

          for (Provider.Service service : provider.getServices()) {
            String algorithmName = service.getAlgorithm();

            if ("Cipher".equalsIgnoreCase(service.getType())) {
              try {
                CACHED_CIPHER_MAX_KEY_LENGTHS.put(String.format("%s.%s", providerName, algorithmName).toLowerCase(),
                    Cipher.getMaxAllowedKeyLength(algorithmName));
              } catch (NoSuchAlgorithmException e) {
                // This is unlikely since we are getting the algorithm names from the service providers.
                // In any case, if a bad algorithm is listed it can be skipped since this is only for
                // informational purposes.
                LOG.warn(String.format("Failed to get the max key length of cipher %s, skipping.", algorithmName), e);
              }
            }
          }
        }
      }
    }

    // Report each cipher as a resource property, if requested
    if (requestedIds.contains(CIPHER_PROPERTIES_PROPERTY_ID)) {
      for (Map.Entry<String, Integer> entry : CACHED_CIPHER_MAX_KEY_LENGTHS.entrySet()) {
        setResourceProperty(resource,
            PropertyHelper.getPropertyId(CIPHER_PROPERTIES_PROPERTY_ID, entry.getKey()),
            entry.getValue(),
            requestedIds);
      }
    }

    // Determine if the unlimited key JCE policy is installed.  This is determined by selecting a
    // valid cipher algorithm and seeing if its max allowed key length value is set to the maximum
    // size of an Integer.
    if (requestedIds.contains(JCE_POLICY_PROPERTY_ID)) {
      Boolean unlimitedKeyJCEPolicyInstalled = null;

      Map.Entry<String, Integer> entry = CACHED_CIPHER_MAX_KEY_LENGTHS.entrySet().iterator().next();
      if (entry != null) {
        unlimitedKeyJCEPolicyInstalled = (Integer.MAX_VALUE == entry.getValue());
      }

      setResourceProperty(resource,
          PropertyHelper.getPropertyId(JCE_POLICY_PROPERTY_ID, "unlimited_key"),
          unlimitedKeyJCEPolicyInstalled,
          requestedIds);
    }
  }
}
