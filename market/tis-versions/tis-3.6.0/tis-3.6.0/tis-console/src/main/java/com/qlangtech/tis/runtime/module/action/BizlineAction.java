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
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.spring.aop.Func;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 业务线控制類
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月8日
 */
public class BizlineAction extends BasicModule {

  private static final long serialVersionUID = 1L;

  @Func(value = PermissionConstant.APP_DEPARTMENT_LIST, sideEffect = false)
  public void doBizData(Context context) {
    this.setBizResult(context, getAllBizDomain(true));
  }

  @Func(value = PermissionConstant.APP_DEPARTMENT_LIST, sideEffect = false)
  public void doGetBizline(Context context) {
    this.setBizResult(context, getAllBizDomain(false));
  }

  @Func(value = PermissionConstant.APP_DEPARTMENT_MANAGE)
  public void doAddBizline(Context context) {
    this.errorsPageShow(context);
    final String name = this.getString("name");
    if (StringUtils.isBlank(name)) {
      this.addErrorMessage(context, "请填写业务线名称");
      return;
    }
    DepartmentCriteria criteria = new DepartmentCriteria();
    criteria.createCriteria().andIsLeaf(false).andNameEqualTo(name).andParentIdEqualTo(-1);

    if (this.getDepartmentDAO().countByExample(criteria) > 0) {
      this.addErrorMessage(context, "业务线:''" + name + "' 已经存在，不能重复添加");
      return;
    }
    Department dpt = new Department();
    dpt.setLeaf(false);
    dpt.setName(name);
    dpt.setFullName(name);
    dpt.setGmtModified(new Date());
    dpt.setGmtCreate(new Date());
    dpt.setParentId(-1);
    dpt.setDptId(this.getDepartmentDAO().insertSelective(dpt));
    this.setBizResult(context, dpt);
    this.addActionMessage(context, "业务线:'" + name + "' 添加成功");
    //this.setBizResult(context, getAllBizDomain(false));
  }

  /**
   * 取得所有的业务线实体
   *
   * @return
   */
  protected final List<Department> getAllBizDomain(boolean leaf) {
    DepartmentCriteria q = new DepartmentCriteria();
    q.createCriteria().andIsLeaf(leaf);
    q.setOrderByClause("dpt_id desc");
    List<Department> dpts = this.getDepartmentDAO().selectByExample(q, 1, 200);
    return dpts;
  }
}
