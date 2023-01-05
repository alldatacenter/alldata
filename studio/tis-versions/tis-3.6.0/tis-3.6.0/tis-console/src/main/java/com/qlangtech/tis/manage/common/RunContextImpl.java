/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.common;

import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.manage.biz.dal.dao.*;
import com.qlangtech.tis.manage.spring.ZooKeeperGetter;
import com.qlangtech.tis.workflow.dao.IWorkflowDAOFacade;
//import org.apache.solr.common.cloud.TISZkStateReader;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class RunContextImpl implements RunContext {

  private final IApplicationDAO applicationDAO;
  private final IGroupInfoDAO groupInfoDAO;

  private final IServerDAO serverDAO;

  private final IServerGroupDAO serverGroupDAO;

  private final ISnapshotDAO snapshotDAO;

  private final ISnapshotViewDAO snapshotViewDAO;

  private final IUploadResourceDAO uploadResourceDAO;


  private final IAppTriggerJobRelationDAO appTriggerJobRelationDAO;

  // private final RpcCoreManage rpcCoreManage;
  private final IFuncDAO funcDAO;

  private final IFuncRoleRelationDAO funcRoleRelationDAO;

  private final IRoleDAO roleDAO;

  private final IResourceParametersDAO resourceParametersDAO;

  private final ZooKeeperGetter zooKeeperGetter;

  // private final ClusterStateReader clusterStateReader;

  private final IWorkflowDAOFacade comDfireTisWorkflowDAOFacade;

  private final IUsrDptRelationDAO usrDptRelationDAO;

  private final IDepartmentDAO departmentDAO;

  private IBizFuncAuthorityDAO bizFuncAuthorityDAO;

  public RunContextImpl(
    IApplicationDAO applicationDAO, // AdminUserService
    IGroupInfoDAO groupInfoDAO, // AdminUserService
    IServerDAO serverDAO, // AdminUserService
    IServerGroupDAO serverGroupDAO, // AdminUserService
    ISnapshotDAO snapshotDAO, // AdminUserService
    ISnapshotViewDAO snapshotViewDAO, // OrgService orgService,
    IUploadResourceDAO uploadResourceDAO, // RpcCoreManage rpcCoreManage,
    IBizFuncAuthorityDAO bizFuncAuthorityDAO, // RpcCoreManage rpcCoreManage,
    IUsrDptRelationDAO usrDptRelationDAO, // RpcCoreManage rpcCoreManage,
    IDepartmentDAO departmentDAO, // RpcCoreManage rpcCoreManage,
    IAppTriggerJobRelationDAO appTriggerJobRelationDAO, // IIsvDAO isvDAO,
    IFuncDAO funcDAO, // IIsvDAO isvDAO,
    IFuncRoleRelationDAO funcRoleRelationDAO, // IIsvDAO isvDAO,
    IRoleDAO roleDAO, // IIsvDAO isvDAO,
    IResourceParametersDAO resourceParametersDAO, // IIsvDAO isvDAO,
    ZooKeeperGetter zooKeeperGetter //
    , IWorkflowDAOFacade workflowDAOFacade) {
    super();

    this.applicationDAO = applicationDAO;
    // this.bizDomainDAO = bizDomainDAO;
    this.groupInfoDAO = groupInfoDAO;
    this.serverDAO = serverDAO;
    this.serverGroupDAO = serverGroupDAO;
    this.snapshotDAO = snapshotDAO;
    this.snapshotViewDAO = snapshotViewDAO;
    this.uploadResourceDAO = uploadResourceDAO;
    this.bizFuncAuthorityDAO = bizFuncAuthorityDAO;
    this.usrDptRelationDAO = usrDptRelationDAO;
    this.departmentDAO = departmentDAO;
    this.appTriggerJobRelationDAO = appTriggerJobRelationDAO;
    this.funcDAO = funcDAO;
    this.funcRoleRelationDAO = funcRoleRelationDAO;
    this.roleDAO = roleDAO;
    this.resourceParametersDAO = resourceParametersDAO;
    this.zooKeeperGetter = zooKeeperGetter;
    // this.clusterStateReader = clusterStateReader;
    this.comDfireTisWorkflowDAOFacade = workflowDAOFacade;
  }

//  @Override
//  public TISZkStateReader getZkStateReader() {
//    return this.clusterStateReader.getInstance();
//  }

  @Override
  public ITISCoordinator getSolrZkClient() {
    return zooKeeperGetter.getInstance();
  }


  @Override
  public IResourceParametersDAO getResourceParametersDAO() {
    return this.resourceParametersDAO;
  }

  @Override
  public IFuncDAO getFuncDAO() {
    return this.funcDAO;
  }

  @Override
  public IFuncRoleRelationDAO getFuncRoleRelationDAO() {
    return this.funcRoleRelationDAO;
  }

  @Override
  public IRoleDAO getRoleDAO() {
    return this.roleDAO;
  }

  @Override
  public IAppTriggerJobRelationDAO getAppTriggerJobRelationDAO() {
    return appTriggerJobRelationDAO;
  }


  public IUploadResourceDAO getUploadResourceDAO() {
    return uploadResourceDAO;
  }

  @Override
  public IBizFuncAuthorityDAO getBizFuncAuthorityDAO() {
    return bizFuncAuthorityDAO;
  }

  public ISnapshotViewDAO getSnapshotViewDAO() {
    return snapshotViewDAO;
  }


  @Override
  public IApplicationDAO getApplicationDAO() {
    return applicationDAO;
  }

  @Override
  public IGroupInfoDAO getGroupInfoDAO() {
    return groupInfoDAO;
  }

  @Override
  public IServerGroupDAO getServerGroupDAO() {
    return serverGroupDAO;
  }

  @Override
  public ISnapshotDAO getSnapshotDAO() {
    return snapshotDAO;
  }

  public IUsrDptRelationDAO getUsrDptRelationDAO() {
    return this.usrDptRelationDAO;
  }

  public IDepartmentDAO getDepartmentDAO() {
    return this.departmentDAO;
  }


  @Override
  public IWorkflowDAOFacade getWorkflowDAOFacade() {
    return this.comDfireTisWorkflowDAOFacade;
  }
}
