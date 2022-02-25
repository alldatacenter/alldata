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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.controller.spi.NoSuchParentResourceException;
import org.apache.ambari.server.controller.spi.NoSuchResourceException;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.Resource.Type;
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.security.authorization.AuthorizationHelper;
import org.apache.ambari.server.security.authorization.ResourceType;
import org.apache.ambari.server.security.authorization.RoleAuthorization;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.kerberos.KerberosDescriptor;
import org.apache.commons.lang.StringUtils;

/**
 * A read-only resource provider to get a the Kerberos Descriptor relevant to the cluster.
 * <p/>
 * The following types are available
 * <ul>
 * <li>STACK - the default descriptor for the relevant stack</li>
 * <li>USER - the user-supplied updates to be applied to the default descriptor</li>
 * <li>COMPOSITE - the default descriptor for the relevant stack with the the user-supplied updates applied</li>
 * </ul>
 */
public class ClusterKerberosDescriptorResourceProvider extends ReadOnlyResourceProvider {

  /**
   * A directive to indicate whether or not the <code>when</code> clauses for Kerberos Identity
   * definitions are to be evaluated or not.  By default, they are not evaluated since historically
   * this provider returned unprocessed Kerberos descriptors.
   */
  public static final String DIRECTIVE_EVALUATE_WHEN_CLAUSE = "evaluate_when";

  /**
   * A directive to indicate additional services to consider when evaluating <code>when</code>
   * clauses.  This value is expcected to be a comma-delimited string that is only used when
   * <code>evaluate_when</code> is <code>true</code>.
   */
  public static final String DIRECTIVE_ADDITIONAL_SERVICES = "additional_services";

  // ----- Property ID constants ---------------------------------------------

  public static final String CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "cluster_name");
  public static final String CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "type");
  public static final String CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID = PropertyHelper.getPropertyId("KerberosDescriptor", "kerberos_descriptor");

  private static final Set<String> PK_PROPERTY_IDS;
  private static final Set<String> PROPERTY_IDS;
  private static final Map<Type, String> KEY_PROPERTY_IDS;

  private static final Set<RoleAuthorization> REQUIRED_GET_AUTHORIZATIONS = EnumSet.of(RoleAuthorization.CLUSTER_TOGGLE_KERBEROS,
      RoleAuthorization.CLUSTER_VIEW_CONFIGS,
      RoleAuthorization.HOST_VIEW_CONFIGS,
      RoleAuthorization.SERVICE_VIEW_CONFIGS);

  static {
    Set<String> set;
    set = new HashSet<>();
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    PK_PROPERTY_IDS = Collections.unmodifiableSet(set);

    set = new HashSet<>();
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    set.add(CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID);
    PROPERTY_IDS = Collections.unmodifiableSet(set);

    HashMap<Type, String> map = new HashMap<>();
    map.put(Type.Cluster, CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);
    map.put(Type.ClusterKerberosDescriptor, CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    KEY_PROPERTY_IDS = Collections.unmodifiableMap(map);
  }

  /**
   * Create a new resource provider.
   */
  public ClusterKerberosDescriptorResourceProvider(AmbariManagementController managementController) {
    super(Type.ClusterKerberosDescriptor, PROPERTY_IDS, KEY_PROPERTY_IDS, managementController);
  }

  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {

    // Ensure the authenticated use has access to this data for any cluster...
    AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, null, REQUIRED_GET_AUTHORIZATIONS);

    Set<String> requestedIds = getRequestPropertyIds(request, predicate);
    Set<Resource> resources = new HashSet<>();

    AmbariManagementController managementController = getManagementController();
    Clusters clusters = managementController.getClusters();

    for (Map<String, Object> propertyMap : getPropertyMaps(predicate)) {
      String clusterName = getClusterName(propertyMap);

      Cluster cluster;
      try {
        cluster = clusters.getCluster(clusterName);

        if (cluster == null) {
          throw new NoSuchParentResourceException(String.format("A cluster with the name %s does not exist.", clusterName));
        }
      } catch (AmbariException e) {
        throw new NoSuchParentResourceException(String.format("A cluster with the name %s does not exist.", clusterName));
      }

      // Ensure the authenticated use has access to this data for the requested cluster...
      AuthorizationHelper.verifyAuthorization(ResourceType.CLUSTER, cluster.getResourceId(), REQUIRED_GET_AUTHORIZATIONS);

      KerberosHelper.KerberosDescriptorType kerberosDescriptorType = getKerberosDescriptorType(propertyMap);
      if (kerberosDescriptorType == null) {
        for (KerberosHelper.KerberosDescriptorType type : KerberosHelper.KerberosDescriptorType.values()) {
          resources.add(toResource(clusterName, type, null, requestedIds));
        }
      } else {
        KerberosDescriptor kerberosDescriptor;
        try {
          KerberosHelper kerberosHelper = getManagementController().getKerberosHelper();
          Map<String, String> requestInfoProperties= request.getRequestInfoProperties();

          kerberosDescriptor = kerberosHelper.getKerberosDescriptor(kerberosDescriptorType,
              cluster,
              getEvaluateWhen(requestInfoProperties),
              getAdditionalServices(requestInfoProperties),
              false);
        } catch (AmbariException e) {
          throw new SystemException("An unexpected error occurred building the cluster's composite Kerberos Descriptor", e);
        }

        if (kerberosDescriptor != null) {
          resources.add(toResource(clusterName, kerberosDescriptorType, kerberosDescriptor, requestedIds));
        }
      }
    }

    return resources;
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Retrieves the cluster name from the request property map.
   *
   * @param propertyMap the request property map
   * @return a cluster name
   * @throws IllegalArgumentException if the cluster name value is missing or empty.
   */
  private String getClusterName(Map<String, Object> propertyMap) {
    String clusterName = (String) propertyMap.get(CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID);

    if (StringUtils.isEmpty(clusterName)) {
      throw new IllegalArgumentException("Invalid argument, cluster name is required");
    }

    return clusterName;
  }

  /**
   * Retrieves the Kerberos descriptor type from the request property map, if one was specified.
   * <p/>
   * See {@link KerberosHelper.KerberosDescriptorType}
   * for expected values.
   *
   * @param propertyMap the request property map
   * @return a KerberosDescriptorType; or null is not specified in the request propery map
   * @throws IllegalArgumentException if the Kerberos descriptor type value is specified but not an expected value.
   */
  private KerberosHelper.KerberosDescriptorType getKerberosDescriptorType(Map<String, Object> propertyMap) {
    String type = (String) propertyMap.get(CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID);
    KerberosHelper.KerberosDescriptorType kerberosDescriptorType = null;

    if (!StringUtils.isEmpty(type)) {
      try {
        kerberosDescriptorType = KerberosHelper.KerberosDescriptorType.valueOf(type.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid argument, kerberos descriptor type of 'STACK', 'USER', or 'COMPOSITE' is required");
      }
    }

    return kerberosDescriptorType;
  }

  /**
   * Determine if <code>when</code> chauses should be evaluated or not.
   * <p>
   * This is determined by the existance and value of the <code>evaluate_when</code> directive.
   * If it exists and is set to "true", then <code>when</code> clauses should be evaluated, else
   * they should not.
   *
   * @param requestInfoProperties a map a request info properties
   * @return true if <code>when</code> clauses are to be evaluted; false otherwise
   */
  private boolean getEvaluateWhen(Map<String, String> requestInfoProperties) {
    return (requestInfoProperties != null) && "true".equalsIgnoreCase(requestInfoProperties.get(DIRECTIVE_EVALUATE_WHEN_CLAUSE));
  }

  /**
   * Get the optional list of services indicated as <code>additional_servides</code> which are to be
   * used in addtion to currently installed services when evaluating <code>when</code> clauses.
   *
   * @param requestInfoProperties a map a request info properties
   * @return a collection of service names, or <code>null</code> if not specified
   */
  private Collection<String> getAdditionalServices(Map<String, String> requestInfoProperties) {
    if (requestInfoProperties != null) {
      String value = requestInfoProperties.get(DIRECTIVE_ADDITIONAL_SERVICES);

      if(!StringUtils.isEmpty(value)) {
        return Arrays.asList(value.split("\\s*,\\s*"));
      }
    }

    return null;
  }

  /**
   * Creates a new resource from the given cluster name, alias, and persist values.
   *
   * @param clusterName            a cluster name
   * @param kerberosDescriptorType a Kerberos descriptor type
   * @param kerberosDescriptor     a Kerberos descriptor
   * @param requestedIds           the properties to include in the resulting resource instance
   * @return a resource
   */
  private Resource toResource(String clusterName, KerberosHelper.KerberosDescriptorType kerberosDescriptorType,
                              KerberosDescriptor kerberosDescriptor, Set<String> requestedIds) {
    Resource resource = new ResourceImpl(Type.ClusterKerberosDescriptor);

    setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_CLUSTER_NAME_PROPERTY_ID, clusterName, requestedIds);

    if (kerberosDescriptorType != null) {
      setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_TYPE_PROPERTY_ID, kerberosDescriptorType.name(), requestedIds);
    }

    if (kerberosDescriptor != null) {
      setResourceProperty(resource, CLUSTER_KERBEROS_DESCRIPTOR_DESCRIPTOR_PROPERTY_ID, kerberosDescriptor.toMap(), requestedIds);
    }

    return resource;
  }

}
