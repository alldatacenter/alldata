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
import com.qlangtech.tis.manage.biz.dal.pojo.Application;
import com.qlangtech.tis.manage.common.TriggerCrontab;
import com.qlangtech.tis.manage.common.apps.AppsFetcher.CriteriaSetter;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 *  @date 2014年7月26日下午7:15:16
 */
public interface IAppsFetcher extends IDepartmentGetter {

  boolean hasGrantAuthority(String permissionCode);

  /**
   * 取得当前用户所在部门的应用
   *
   * @param setter
   * @return
   */
  List<Application> getApps(CriteriaSetter setter);

  /**
   * 统计符合条件的应用数目
   *
   * @param setter
   * @return
   */
  int count(CriteriaSetter setter);

  /**
   * 更新应用
   *
   * @param app
   * @param setter
   * @return
   */
  int update(Application app, CriteriaSetter setter);


  /**
   * 显示所有的定时任务
   *
   * @param usrDptRelationDAO
   * @return
   */
  List<TriggerCrontab> getTriggerTabs(IUsrDptRelationDAO usrDptRelationDAO);

}
