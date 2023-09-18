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
package com.qlangtech.tis.workflow.dao.impl;

import com.qlangtech.tis.workflow.dao.*;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ComDfireTisWorkflowDAOFacadeImpl implements IWorkflowDAOFacade {

  private final IWorkFlowDAO workFlowDAO;

  private final IWorkFlowBuildHistoryDAO workFlowBuildHistoryDAO;

  //private final IDatasourceTableDAO datasourceTableDAO;

  private final IDatasourceDbDAO datasourceDbDAO;


  public IWorkFlowDAO getWorkFlowDAO() {
    return this.workFlowDAO;
  }

  public IWorkFlowBuildHistoryDAO getWorkFlowBuildHistoryDAO() {
    return this.workFlowBuildHistoryDAO;
  }

 // public IDatasourceTableDAO getDatasourceTableDAO() {
   // return this.datasourceTableDAO;
  //}

  public IDatasourceDbDAO getDatasourceDbDAO() {
    return this.datasourceDbDAO;
  }

  public ComDfireTisWorkflowDAOFacadeImpl(IWorkFlowDAO workFlowDAO, IWorkFlowBuildHistoryDAO workFlowBuildHistoryDAO
    , IDatasourceTableDAO datasourceTableDAO, IDatasourceDbDAO datasourceDbDAO) {
    this.workFlowDAO = workFlowDAO;
    this.workFlowBuildHistoryDAO = workFlowBuildHistoryDAO;
   // this.datasourceTableDAO = datasourceTableDAO;
    this.datasourceDbDAO = datasourceDbDAO;
  }
}
