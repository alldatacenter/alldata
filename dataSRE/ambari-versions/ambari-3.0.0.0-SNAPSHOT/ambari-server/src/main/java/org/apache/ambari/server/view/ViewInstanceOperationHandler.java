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

package org.apache.ambari.server.view;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ViewInstanceOperationHandler {
  private final static Logger LOG = LoggerFactory.getLogger(ViewInstanceOperationHandler.class);

  @Inject
  ViewDAO viewDAO;

  /**
   * View instance data access object.
   */
  @Inject
  ViewInstanceDAO instanceDAO;

  /**
   * Privilege data access object.
   */
  @Inject
  PrivilegeDAO privilegeDAO;

  // remove a privilege entity.
  private void removePrivilegeEntity(PrivilegeEntity privilegeEntity) {

    PrincipalEntity principalEntity = privilegeEntity.getPrincipal();
    if (principalEntity != null) {
      principalEntity.removePrivilege(privilegeEntity);
    }

    privilegeDAO.remove(privilegeEntity);
  }

  public void uninstallViewInstance(ViewInstanceEntity instanceEntity) {
    LOG.info("uninstalling ViewInstance : {} ", instanceEntity);
    ViewEntity viewEntity = viewDAO.findByName(instanceEntity.getViewName());
    LOG.info("viewEntity received corresponding to the view entity : {} ", viewEntity);

    if (viewEntity != null) {
      String instanceName = instanceEntity.getName();
      String viewName = viewEntity.getCommonName();
      String version = viewEntity.getVersion();

      ViewInstanceEntity instanceDefinition = instanceDAO.findByName(instanceEntity.getViewName(), instanceEntity.getName()); //getInstanceDefinition(viewName, version, instanceName);
      LOG.debug("view instance entity received from database : {}", instanceDefinition);
      if (instanceDefinition != null) {
        if (instanceDefinition.isXmlDriven()) {
          throw new IllegalStateException("View instances defined via xml can't be deleted through api requests");
        }
        List<PrivilegeEntity> instancePrivileges = privilegeDAO.findByResourceId(instanceEntity.getResource().getId());
        LOG.info("Removing privilege entities : {}", instancePrivileges);
        for (PrivilegeEntity privilegeEntity : instancePrivileges) {
          removePrivilegeEntity(privilegeEntity);
        }
        LOG.info("Deleting view instance : view name : {}, version : {}, instanceName : {}", viewName, version, instanceName);
        instanceDAO.remove(instanceDefinition);
      }else{
        throw new IllegalStateException("View instance '" + instanceEntity.getName() + "' not found.");
      }
    }else{
      throw new IllegalStateException("View '" + instanceEntity.getViewName() + "' not found corresponding to view instance '" + instanceEntity.getName() + "'");
    }
  }
}
