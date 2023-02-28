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
package com.qlangtech.tis.manage.biz.dal.dao.impl;

import com.qlangtech.tis.manage.biz.dal.dao.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TerminatorManageBizDalDAOFacadeImpl implements ITISManageBizDalDAOFacade {

  private final IUsrDptRelationDAO usrDptRelationDAO;

  private final IApplicationDAO applicationDAO;

  private final IDepartmentDAO departmentDAO;

  private final IFuncRoleRelationDAO funcRoleRelationDAO;

  private final IRoleDAO roleDAO;

  private final IFuncDAO funcDAO;

  public IUsrDptRelationDAO getUsrDptRelationDAO() {
    return this.usrDptRelationDAO;
  }

  public IApplicationDAO getApplicationDAO() {
    return this.applicationDAO;
  }

  public IDepartmentDAO getDepartmentDAO() {
    return this.departmentDAO;
  }

  public IFuncRoleRelationDAO getFuncRoleRelationDAO() {
    return this.funcRoleRelationDAO;
  }

  public IRoleDAO getRoleDAO() {
    return this.roleDAO;
  }

  public IFuncDAO getFuncDAO() {
    return this.funcDAO;
  }


  public TerminatorManageBizDalDAOFacadeImpl(// IIsvDAO isvDAO,
                                             IUsrDptRelationDAO usrDptRelationDAO, // IIsvDAO isvDAO,
                                             IApplicationDAO applicationDAO, // IIsvDAO isvDAO,
                                             IDepartmentDAO departmentDAO, // IIsvDAO isvDAO,
                                             IFuncRoleRelationDAO funcRoleRelationDAO, // IIsvDAO isvDAO,
                                             IRoleDAO roleDAO, // IIsvDAO isvDAO,
                                             IFuncDAO funcDAO // IIsvDAO isvDAO,
  ) {
    this.usrDptRelationDAO = usrDptRelationDAO;
    this.applicationDAO = applicationDAO;
    this.departmentDAO = departmentDAO;
    this.funcRoleRelationDAO = funcRoleRelationDAO;
    this.roleDAO = roleDAO;
    this.funcDAO = funcDAO;

  }
}
