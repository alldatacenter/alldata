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

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.Arrays;
import java.util.List;

import org.apache.ambari.server.orm.dao.PrivilegeDAO;
import org.apache.ambari.server.orm.dao.ViewDAO;
import org.apache.ambari.server.orm.dao.ViewInstanceDAO;
import org.apache.ambari.server.orm.entities.PrincipalEntity;
import org.apache.ambari.server.orm.entities.PrivilegeEntity;
import org.apache.ambari.server.orm.entities.ResourceEntity;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.easymock.EasyMock;
import org.junit.Test;

public class ViewInstanceOperationHandlerTest {
  @Test
  public void uninstallViewInstance() throws Exception {
    ViewInstanceOperationHandler viewInstanceOperationHandler = getViewInstanceOperationHandler();

    ResourceEntity resourceEntity = new ResourceEntity();
    resourceEntity.setId(3L);
    ViewInstanceEntity instanceEntity = new ViewInstanceEntity();
    instanceEntity.setName("VIEW_INSTANCE_NAME");
    instanceEntity.setViewName("VIEW_NAME");
    instanceEntity.setResource(resourceEntity);

    ViewDAO viewDAO = viewInstanceOperationHandler.viewDAO;
    ViewEntity viewEntity = EasyMock.createNiceMock(ViewEntity.class);
    expect(viewDAO.findByName(instanceEntity.getViewName())).andReturn(viewEntity);
    expect(viewEntity.getCommonName()).andReturn("view-common-name");
    expect(viewEntity.getVersion()).andReturn("0.0.1");

    ViewInstanceDAO viewInstanceDAO = viewInstanceOperationHandler.instanceDAO;
    ViewInstanceEntity viewInstanceEntity = createNiceMock(ViewInstanceEntity.class);
    expect(viewInstanceDAO.findByName(instanceEntity.getViewName(), instanceEntity.getName())).andReturn(viewInstanceEntity);
    expect(viewInstanceEntity.isXmlDriven()).andReturn(false);
    expect(viewInstanceEntity.isXmlDriven()).andReturn(false);

    PrivilegeEntity privilege1 = createNiceMock(PrivilegeEntity.class);
    PrivilegeEntity privilege2 = createNiceMock(PrivilegeEntity.class);
    List<PrivilegeEntity> privileges = Arrays.asList(privilege1, privilege2);

    PrivilegeDAO privilegeDAO = viewInstanceOperationHandler.privilegeDAO;

    PrincipalEntity principalEntity = createNiceMock(PrincipalEntity.class);

    expect(privilege1.getPrincipal()).andReturn(principalEntity);
    expect(privilege2.getPrincipal()).andReturn(principalEntity);

    principalEntity.removePrivilege(privilege1);
    principalEntity.removePrivilege(privilege2);

    expect(privilegeDAO.findByResourceId(3L)).andReturn(privileges);

    privilegeDAO.remove(privilege1);
    privilegeDAO.remove(privilege2);

    viewInstanceDAO.remove(viewInstanceEntity);

    replay(privilegeDAO, viewDAO, viewInstanceDAO, principalEntity, privilege1, privilege2,viewInstanceEntity, viewEntity);

    viewInstanceOperationHandler.uninstallViewInstance(instanceEntity);

    verify(privilegeDAO, viewDAO, viewInstanceDAO);
  }

  private ViewInstanceOperationHandler getViewInstanceOperationHandler() {
    ViewDAO viewDAO = EasyMock.createNiceMock(ViewDAO.class);
    ViewInstanceDAO instanceDAO = EasyMock.createNiceMock(ViewInstanceDAO.class);
    PrivilegeDAO privilegeDAO = EasyMock.createNiceMock(PrivilegeDAO.class);

    ViewInstanceOperationHandler instance = new ViewInstanceOperationHandler();
    instance.viewDAO = viewDAO;
    instance.instanceDAO = instanceDAO;
    instance.privilegeDAO = privilegeDAO;

    return instance;
  }

}