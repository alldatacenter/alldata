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

package org.apache.ambari.server.security.authorization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PermissionHelper {

  private final static Logger LOG = LoggerFactory.getLogger(PermissionHelper.class);

  @Inject
  private ClusterDAO clusterDAO;

  @Inject
  private ViewInstanceDAO viewInstanceDAO;

  /**
   * Retrieve permission labels based on the details of the authenticated user
   * @param authentication the authenticated user and associated access privileges
   * @return human-readable permissions
   */
  public Map<String,List<String>> getPermissionLabels(Authentication authentication) {
    Map<String,List<String>> permissionLabels = new HashMap<>();
    if (authentication.getAuthorities() != null) {
      for (GrantedAuthority grantedAuthority : authentication.getAuthorities()) {
        if(!(grantedAuthority instanceof AmbariGrantedAuthority)) {
          continue;
        }
        AmbariGrantedAuthority ambariGrantedAuthority = (AmbariGrantedAuthority) grantedAuthority;

        PrivilegeEntity privilegeEntity = ambariGrantedAuthority.getPrivilegeEntity();

        String key = null;
        try {
          switch(privilegeEntity.getResource().getResourceType().getName()) {
            case "CLUSTER":
              key = clusterDAO.findByResourceId(privilegeEntity.getResource().getId()).getClusterName();
              break;
            case "AMBARI":
              key = "Ambari";
              break;
            default:
              key = viewInstanceDAO.findByResourceId(privilegeEntity.getResource().getId()).getLabel();
              break;
          }
        } catch (Throwable ignored) {
          LOG.warn("Error occurred when cluster or view is searched based on resource id", ignored);
        }

        if(key != null) {
          if(!permissionLabels.containsKey(key)) {
            permissionLabels.put(key, new LinkedList<>());
          }
          permissionLabels.get(key).add(privilegeEntity.getPermission().getPermissionLabel());
        }
      }
    }
    return permissionLabels;
  }
}
