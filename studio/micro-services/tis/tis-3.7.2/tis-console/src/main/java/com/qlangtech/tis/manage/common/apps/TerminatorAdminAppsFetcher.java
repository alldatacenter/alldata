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
package com.qlangtech.tis.manage.common.apps;

import com.qlangtech.tis.manage.biz.dal.dao.IUsrDptRelationDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria.Criteria;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.IUser;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.TriggerCrontab;

import java.util.ArrayList;
import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-28
 */
public class TerminatorAdminAppsFetcher extends NormalUserApplicationFetcher {

  public TerminatorAdminAppsFetcher(IUser user, Department department, RunContext context) {
    super(user, department, context);
  }

  @Override
  protected Criteria process(Criteria criteria) {
    // return criteria.andDptIdEqualTo(user.getDepartmentid());
    return criteria;
  }

  @Override
  public List<TriggerCrontab> getTriggerTabs(IUsrDptRelationDAO usrDptRelationDAO) {
    return getAllTriggerTabs(usrDptRelationDAO);
  }

  /**
   * @param usrDptRelationDAO
   * @return
   */
  public static List<TriggerCrontab> getAllTriggerTabs(IUsrDptRelationDAO usrDptRelationDAO) {
    UsrDptRelationCriteria ucriteria = new UsrDptRelationCriteria();
    ucriteria.createCriteria().andIsAutoDeploy();
    // 应用触发器一览
    return usrDptRelationDAO.selectAppDumpJob(ucriteria);
  }

  // @Override
  // public List<ApplicationApply> getAppApplyList(
  // IApplicationApplyDAO applicationApplyDAO) {
  //
  // return super.getAppApplyList(applicationApplyDAO);
  // }
  @Override
  public List<Department> getDepartmentBelongs(RunContext runContext) {
    DepartmentCriteria criteria = new DepartmentCriteria();
    criteria.createCriteria().andIsLeaf(true);
    return runContext.getDepartmentDAO().selectByExample(criteria, 1, 500);
  }

  @Override
  protected List<String> initAuthorityFuncList() {
    return new ArrayList<String>() {

      private static final long serialVersionUID = 0;

      @Override
      public boolean contains(Object o) {
        return true;
      }
    };
  }
}
