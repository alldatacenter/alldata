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

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Collections;
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
import org.apache.ambari.server.controller.spi.SystemException;
import org.apache.ambari.server.controller.spi.UnsupportedPropertyException;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.dao.KerberosPrincipalDAO;
import org.apache.ambari.server.orm.entities.HostEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosKeytabDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalDescriptor;
import org.apache.ambari.server.state.kerberos.KerberosPrincipalType;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

/**
 * Read-only resource provider for Kerberos identity resources.
 */
public class HostKerberosIdentityResourceProvider extends ReadOnlyResourceProvider {

  protected static final String KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID = "KerberosIdentity/cluster_name";
  protected static final String KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID = "KerberosIdentity/host_name";
  protected static final String KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID = "KerberosIdentity/description";
  protected static final String KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID = "KerberosIdentity/principal_name";
  protected static final String KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID = "KerberosIdentity/principal_type";
  protected static final String KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID = "KerberosIdentity/principal_local_username";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID = "KerberosIdentity/keytab_file_path";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID = "KerberosIdentity/keytab_file_owner";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID = "KerberosIdentity/keytab_file_owner_access";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID = "KerberosIdentity/keytab_file_group";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID = "KerberosIdentity/keytab_file_group_access";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID = "KerberosIdentity/keytab_file_mode";
  protected static final String KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID = "KerberosIdentity/keytab_file_installed";

  protected static final Map<Resource.Type, String> PK_PROPERTY_MAP = Collections.unmodifiableMap(
      new HashMap<Resource.Type, String>() {{
        put(Resource.Type.Cluster, KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID);
        put(Resource.Type.Host, KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID);
        put(Resource.Type.HostKerberosIdentity, KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID);
      }}
  );

  protected static final Set<String> PK_PROPERTY_IDS = Collections.unmodifiableSet(
      new HashSet<>(PK_PROPERTY_MAP.values())
  );

  protected static final Set<String> PROPERTY_IDS = Collections.unmodifiableSet(
      new HashSet<String>() {{
        add(KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID);
        add(KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID);
        add(KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID);
        add(KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID);
        add(KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID);
        add(KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID);
        add(KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID);
      }}
  );

  @Inject
  private KerberosHelper kerberosHelper;

  /**
   * KerberosPrincipalDAO used to get Kerberos principal details
   */
  @Inject
  private KerberosPrincipalDAO kerberosPrincipalDAO;

  /**
   * HostDAO used to translate host names to host ids
   */
  @Inject
  private HostDAO hostDAO;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  /**
   * Create a  new resource provider for the given management controller.
   *
   * @param managementController the management controller
   */
  @AssistedInject
  HostKerberosIdentityResourceProvider(@Assisted AmbariManagementController managementController) {
    super(Resource.Type.HostKerberosIdentity, PROPERTY_IDS, PK_PROPERTY_MAP, managementController);
  }


  @Override
  public Set<Resource> getResources(Request request, Predicate predicate)
      throws SystemException, UnsupportedPropertyException, NoSuchResourceException, NoSuchParentResourceException {
    return getResources(new GetResourcesCommand(getPropertyMaps(predicate), getRequestPropertyIds(request, predicate)));
  }

  @Override
  protected Set<String> getPKPropertyIds() {
    return PK_PROPERTY_IDS;
  }

  /**
   * Inner class to implement the "get resource" command.
   */
  private class GetResourcesCommand implements Command<Set<Resource>> {
    private final Set<Map<String, Object>> propertyMaps;
    private final Set<String> requestPropertyIds;

    public GetResourcesCommand(Set<Map<String, Object>> propertyMaps, Set<String> requestPropertyIds) {
      this.propertyMaps = propertyMaps;
      this.requestPropertyIds = requestPropertyIds;
    }

    @Override
    public Set<Resource> invoke() throws AmbariException {
      Set<Resource> resources = new HashSet<>();

      for (Map<String, Object> propertyMap : propertyMaps) {
        String clusterName = (String) propertyMap.get(KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID);
        String hostName = (String) propertyMap.get(KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID);

        // Retrieve the active identities for the cluster filtered and grouped by hostname
        Map<String, Collection<KerberosIdentityDescriptor>> hostDescriptors =
            kerberosHelper.getActiveIdentities(clusterName, hostName, null, null, true);

        if (hostDescriptors != null) {
          for (Map.Entry<String, Collection<KerberosIdentityDescriptor>> entry : hostDescriptors.entrySet()) {
            Collection<KerberosIdentityDescriptor> descriptors = entry.getValue();

            if (descriptors != null) {
              String currentHostName = entry.getKey();
              HostEntity host = hostDAO.findByName(currentHostName);
              Long hostId = (host == null) ? null : host.getHostId();

              for (KerberosIdentityDescriptor descriptor : descriptors) {
                KerberosPrincipalDescriptor principalDescriptor = descriptor.getPrincipalDescriptor();
                if (principalDescriptor != null) {
                  String principal = principalDescriptor.getValue();

                  if ((principal != null) && !principal.isEmpty()) {
                    Resource resource = new ResourceImpl(Resource.Type.HostKerberosIdentity);
                    KerberosPrincipalType principalType = principalDescriptor.getType();

                    // Assume the principal is a service principal if not specified
                    if (principalType == null) {
                      principalType = KerberosPrincipalType.SERVICE;
                    }

                    setResourceProperty(resource, KERBEROS_IDENTITY_CLUSTER_NAME_PROPERTY_ID, clusterName, requestPropertyIds);
                    setResourceProperty(resource, KERBEROS_IDENTITY_HOST_NAME_PROPERTY_ID, currentHostName, requestPropertyIds);

                    setResourceProperty(resource, KERBEROS_IDENTITY_PRINCIPAL_NAME_PROPERTY_ID, principal, requestPropertyIds);
                    setResourceProperty(resource, KERBEROS_IDENTITY_PRINCIPAL_TYPE_PROPERTY_ID, principalType, requestPropertyIds);
                    setResourceProperty(resource, KERBEROS_IDENTITY_PRINCIPAL_LOCAL_USERNAME_PROPERTY_ID, principalDescriptor.getLocalUsername(), requestPropertyIds);

                    KerberosKeytabDescriptor keytabDescriptor = descriptor.getKeytabDescriptor();

                    String installedStatus;

                    if ((hostId != null) && kerberosPrincipalDAO.exists(principal)) {
                      if (keytabDescriptor != null) {
                        KerberosKeytabPrincipalEntity entity = kerberosKeytabPrincipalDAO.findByNaturalKey(hostId, keytabDescriptor.getFile(), principal);
                        if (entity != null && entity.isDistributed()) {
                          installedStatus = "true";
                        } else {
                          installedStatus = "false";
                        }
                      } else {
                        installedStatus = "false";
                      }
                    } else {
                      installedStatus = "unknown";
                    }

                    setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_INSTALLED_PROPERTY_ID, installedStatus, requestPropertyIds);

                    if (keytabDescriptor != null) {
                      String ownerAccess = keytabDescriptor.getOwnerAccess();
                      String groupAccess = keytabDescriptor.getGroupAccess();
                      int mode = 0;

                      // Create the file access mode using *nix chmod values.
                      if ("rw".equals(ownerAccess)) {
                        mode += 600;
                      } else if ("r".equals(ownerAccess)) {
                        mode += 400;
                      }

                      if ("rw".equals(groupAccess)) {
                        mode += 60;
                      } else if ("r".equals(groupAccess)) {
                        mode += 40;
                      }

                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_PATH_PROPERTY_ID, keytabDescriptor.getFile(), requestPropertyIds);
                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_PROPERTY_ID, keytabDescriptor.getOwnerName(), requestPropertyIds);
                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_OWNER_ACCESS_PROPERTY_ID, ownerAccess, requestPropertyIds);
                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_PROPERTY_ID, keytabDescriptor.getGroupName(), requestPropertyIds);
                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_GROUP_ACCESS_PROPERTY_ID, groupAccess, requestPropertyIds);
                      setResourceProperty(resource, KERBEROS_IDENTITY_KEYTAB_FILE_MODE_PROPERTY_ID, new DecimalFormat("000").format(mode), requestPropertyIds);
                    }

                    setResourceProperty(resource, KERBEROS_IDENTITY_DESCRIPTION_PROPERTY_ID, descriptor.getName(), requestPropertyIds);

                    resources.add(resource);
                  }
                }
              }
            }
          }
        }
      }

      return resources;
    }
  }
}
