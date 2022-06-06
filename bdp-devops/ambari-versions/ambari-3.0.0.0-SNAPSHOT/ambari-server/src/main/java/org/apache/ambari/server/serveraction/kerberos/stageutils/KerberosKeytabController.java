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

package org.apache.ambari.server.serveraction.kerberos.stageutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.controller.KerberosHelper;
import org.apache.ambari.server.orm.dao.KerberosKeytabDAO;
import org.apache.ambari.server.orm.dao.KerberosKeytabPrincipalDAO;
import org.apache.ambari.server.orm.entities.KerberosKeytabEntity;
import org.apache.ambari.server.orm.entities.KerberosKeytabPrincipalEntity;
import org.apache.ambari.server.orm.entities.KerberosPrincipalEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Service;
import org.apache.ambari.server.state.kerberos.KerberosIdentityDescriptor;
import org.apache.commons.collections.MapUtils;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Helper class to construct convenient wrappers around database entities related to kerberos.
 */
@Singleton
public class KerberosKeytabController {
  @Inject
  private KerberosKeytabDAO kerberosKeytabDAO;

  @Inject
  private KerberosKeytabPrincipalDAO kerberosKeytabPrincipalDAO;

  //TODO: due to circular dependencies in Guice this field cannot be injected with Guice's @Inject annotation; for now we should statically inject in AmbariServer
  private static KerberosHelper kerberosHelper;

  public static void setKerberosHelper(KerberosHelper kerberosHelper) {
    KerberosKeytabController.kerberosHelper = kerberosHelper;
  }

  /**
   * Tries to find keytab by keytab path in destination filesystem.
   *
   * @param file keytab path
   * @return found keytab or null
   */
  public ResolvedKerberosKeytab getKeytabByFile(String file) {
    return getKeytabByFile(file, true);
  }

  /**
   * Tries to find keytab by keytab path in destination filesystem.
   *
   * @param file keytab path
   * @param resolvePrincipals include resolved principals
   * @return found keytab or null
   */
  public ResolvedKerberosKeytab getKeytabByFile(String file, boolean resolvePrincipals) {
    return fromKeytabEntity(kerberosKeytabDAO.find(file), resolvePrincipals);
  }

  /**
   * Returns all keytabs managed by ambari.
   *
   * @return all keytabs
   */
  public Set<ResolvedKerberosKeytab> getAllKeytabs() {
    return fromKeytabEntities(kerberosKeytabDAO.findAll());
  }

  /**
   * Returns all keytabs that contains given principal.
   *
   * @param rkp principal to filter keytabs by
   * @return set of keytabs found
   */
  public Set<ResolvedKerberosKeytab> getFromPrincipal(ResolvedKerberosPrincipal rkp) {
    return fromKeytabEntities(kerberosKeytabDAO.findByPrincipalAndHost(rkp.getPrincipal(), rkp.getHostId()));
  }

  /**
   * Returns keytabs with principals filtered by host, principal name or service(and component) names.
   *
   * @param serviceComponentFilter service-component filter
   * @param hostFilter host filter
   * @param identityFilter identity(principal) filter
   * @return set of keytabs found
   */
  private Set<ResolvedKerberosKeytab> getFilteredKeytabs(Map<String, ? extends Collection<String>> serviceComponentFilter,
                                                        Set<String> hostFilter, Collection<String> identityFilter) {
    if (serviceComponentFilter == null && hostFilter == null && identityFilter == null) {
      return getAllKeytabs();
    }
    List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> filters = splitServiceFilter(serviceComponentFilter);
    for (KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter filter : filters) {
      filter.setHostNames(hostFilter);
      filter.setPrincipals(identityFilter);
    }

    Set<ResolvedKerberosPrincipal> filteredPrincipals = fromPrincipalEntities(kerberosKeytabPrincipalDAO.findByFilters(filters));
    HashMap<String, ResolvedKerberosKeytab> resultMap = new HashMap<>();
    for (ResolvedKerberosPrincipal principal : filteredPrincipals) {
      if (!resultMap.containsKey(principal.getKeytabPath())) {
        resultMap.put(principal.getKeytabPath(), getKeytabByFile(principal.getKeytabPath(), false));
      }
      ResolvedKerberosKeytab keytab = resultMap.get(principal.getKeytabPath());
      keytab.addPrincipal(principal);
    }
    return Sets.newHashSet(resultMap.values());
  }

  public Set<ResolvedKerberosKeytab> getFilteredKeytabs(Collection<KerberosIdentityDescriptor> serviceIdentities, Set<String> hostFilter, Collection<String> identityFilters) {
    final Collection<String> enhancedIdentityFilters = populateIdentityFilter(identityFilters, serviceIdentities);
    return getFilteredKeytabs((Map<String, ? extends Collection<String>>) null, hostFilter, enhancedIdentityFilters);
  }

  /**
   * This function split serviceComponentFilter to two filters, one with specific components, and another one with service
   * only. Can return only one filter if filter contain only one type of mapping(whole service or component based)
   * or empty filter if no serviceComponentFilter provided.
   *
   * @param serviceComponentFilter
   * @return
   */
  private List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> splitServiceFilter(Map<String, ? extends Collection<String>> serviceComponentFilter) {
    if (serviceComponentFilter != null && serviceComponentFilter.size() > 0) {
      Set<String> serviceSet = new HashSet<>();
      Set<String> componentSet = new HashSet<>();
      Set<String> serviceOnlySet = new HashSet<>();

      // Split the filter into a service/component filter or a service-only filter.
      for (Map.Entry<String, ? extends Collection<String>> entry : serviceComponentFilter.entrySet()) {
        String serviceName = entry.getKey();
        Collection<String> serviceComponents = entry.getValue();

        if((serviceComponents == null) || serviceComponents.contains("*")) {
          serviceOnlySet.add(serviceName);
        }
        else {
          serviceSet.add(serviceName);
          componentSet.addAll(serviceComponents);
        }
      }

      List<KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter> result = new ArrayList<>();
      // Handle the service/component filter
      if (serviceSet.size() > 0) {
        result.add(KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter.createFilter(
          null,
          serviceSet,
          componentSet,
          null
        ));
      }
      // Handler the service/* filter
      if (serviceOnlySet.size() > 0) {
        result.add(KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter.createFilter(
          null,
          serviceOnlySet,
          null,
          null
        ));
      }
      if (result.size() > 0) {
        return result;
      }
    }

    return Lists.newArrayList(KerberosKeytabPrincipalDAO.KerberosKeytabPrincipalFilter.createEmptyFilter());
  }

  private ResolvedKerberosKeytab fromKeytabEntity(KerberosKeytabEntity kke, boolean resolvePrincipals) {
    Set<ResolvedKerberosPrincipal> principals = resolvePrincipals ? fromPrincipalEntities(kke.getKerberosKeytabPrincipalEntities()) : new HashSet<>();
    return new ResolvedKerberosKeytab(
      kke.getKeytabPath(),
      kke.getOwnerName(),
      kke.getOwnerAccess(),
      kke.getGroupName(),
      kke.getGroupAccess(),
      principals,
      kke.isAmbariServerKeytab(),
      kke.isWriteAmbariJaasFile()
    );
  }

  private ResolvedKerberosKeytab fromKeytabEntity(KerberosKeytabEntity kke) {
    return fromKeytabEntity(kke, true);
  }

  private Set<ResolvedKerberosKeytab> fromKeytabEntities(Collection<KerberosKeytabEntity> keytabEntities) {
    ImmutableSet.Builder<ResolvedKerberosKeytab> builder = ImmutableSet.builder();
    for (KerberosKeytabEntity kkpe : keytabEntities) {
      builder.add(fromKeytabEntity(kkpe));
    }
    return builder.build();
  }

  private Set<ResolvedKerberosPrincipal> fromPrincipalEntities(Collection<KerberosKeytabPrincipalEntity> principalEntities) {
    ImmutableSet.Builder<ResolvedKerberosPrincipal> builder = ImmutableSet.builder();
    for (KerberosKeytabPrincipalEntity kkpe : principalEntities) {
      KerberosPrincipalEntity kpe = kkpe.getKerberosPrincipalEntity();
      if(kpe != null) {
        ResolvedKerberosPrincipal rkp = new ResolvedKerberosPrincipal(
            kkpe.getHostId(),
            kkpe.getHostName(),
            kkpe.getPrincipalName(),
            kpe.isService(),
            kpe.getCachedKeytabPath(),
            kkpe.getKeytabPath(),
            kkpe.getServiceMappingAsMultimap());
        builder.add(rkp);
      }
    }
    return builder.build();
  }

  /**
   * Adjust service component filter according to installed services
   *
   * @param cluster                cluster
   * @param includeAmbariAsService
   * @param serviceComponentFilter
   * @return
   * @throws AmbariException
   */
  public Map<String, Collection<String>> adjustServiceComponentFilter(Cluster cluster, boolean includeAmbariAsService, Map<String, ? extends Collection<String>> serviceComponentFilter) throws AmbariException {
    Map<String, Collection<String>> adjustedFilter = new HashMap<>();

    Map<String, Service> installedServices = (cluster == null) ? null : cluster.getServices();
    if(includeAmbariAsService) {
      installedServices = (installedServices == null) ? new HashMap<>() : new HashMap<>(installedServices);
      installedServices.put("AMBARI", null);
    }

    if (!MapUtils.isEmpty(installedServices)) {
      if (serviceComponentFilter != null) {
        // prune off services that are not installed, or considered installed - like AMBARI
        for (Map.Entry<String, ? extends Collection<String>> entry : serviceComponentFilter.entrySet()) {
          String serviceName = entry.getKey();

          if (installedServices.containsKey(serviceName)) {
            adjustedFilter.put(serviceName, entry.getValue());
          }
        }
      } else {
        // return only the set of installed services
        for (String serviceName : installedServices.keySet()) {
          // Add an entry to indicate the service and all of it's components should be considered
          adjustedFilter.put(serviceName, Collections.singletonList("*"));
        }
      }
    }

    return adjustedFilter;
  }

  public Collection<KerberosIdentityDescriptor> getServiceIdentities(String clusterName, Collection<String> services) throws AmbariException {
    final Collection<KerberosIdentityDescriptor> serviceIdentities = new ArrayList<>();
    for (String service : services) {
      for (Collection<KerberosIdentityDescriptor> activeIdentities : kerberosHelper.getActiveIdentities(clusterName, null, service, null, true).values()) {
        serviceIdentities.addAll(activeIdentities);
      }
    }
    return serviceIdentities;
  }

  private Collection<String> populateIdentityFilter(Collection<String> identityFilters, Collection<KerberosIdentityDescriptor> serviceIdentities) {
    if (serviceIdentities != null) {
      identityFilters = identityFilters == null ? new HashSet<>() : identityFilters;
      for (KerberosIdentityDescriptor serviceIdentity : serviceIdentities) {
        if (!KerberosHelper.AMBARI_SERVER_KERBEROS_IDENTITY_NAME.equals(serviceIdentity.getName())) {
          identityFilters.add(serviceIdentity.getPrincipalDescriptor().getName());
        }
      }
    }
    return identityFilters;
  }
}
