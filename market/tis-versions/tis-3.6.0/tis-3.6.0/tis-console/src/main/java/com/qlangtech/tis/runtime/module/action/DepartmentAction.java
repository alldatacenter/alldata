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
import com.qlangtech.tis.coredefine.module.action.DataxAction;
import com.qlangtech.tis.manage.biz.dal.pojo.ApplicationCriteria.Criteria;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.apps.AppsFetcher.CriteriaSetter;
import com.qlangtech.tis.manage.common.apps.IAppsFetcher;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.runtime.module.misc.impl.DelegateControl4JsonPostMsgHandler;
import junit.framework.Assert;

import java.util.Date;
import java.util.Map;

/**
 * 部门管理ACTION
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-5-28
 */
public class DepartmentAction extends BasicModule {

  private static final long serialVersionUID = 1L;

  /**
   * 更新部门
   *
   * @param context
   */
  public void doUpdateDepartment(Context context) {
    Department department = new Department();
    this.processDepartment(context, department, (dpt) -> {
        if (dpt.getDptId() == null) {
          throw new IllegalStateException("dpt.getDptId() can not be null");
        }
        final Integer dptId = dpt.getDptId();
        try {
          DepartmentCriteria criteria = new DepartmentCriteria();
          criteria.createCriteria().andDptIdEqualTo(dptId).andIsLeaf(true);
          dpt.setDptId(null);
          if (this.getDepartmentDAO().updateByExampleSelective(dpt, criteria) > 0) {
            DataxAction.cleanDepsCache();
            this.addActionMessage(context, "更新部门：'" + dpt.getName() + "'");
          } else {
            this.addErrorMessage(context, "更新部门 '" + dpt.getName() + "' 失败");
          }
        } finally {
          dpt.setDptId(dptId);
        }
      } //
      , "dptId"
      , new Validator.FieldValidators(Validator.require, Validator.integer) {
        @Override
        public void setFieldVal(String val) {
          department.setDptId(Integer.parseInt(val));
        }
      }
    );
  }

  /**
   * 添加部门
   *
   * @param context
   */
  public void doAddDepartment(Context context) {
    Department department = new Department();
    this.processDepartment(context, department, (dpt) -> {
      dpt.setDptId(this.getDepartmentDAO().insertSelective(dpt));
      DataxAction.cleanDepsCache();
      this.addActionMessage(context, "成功添加部门：" + dpt.getName());
    });
  }

  private boolean processDepartment(Context context, Department dpt, IDepartmentProcess dptProcess, Object... validateRuleParams) {

    Map<String, Validator.FieldValidators> validateRule = //
      Validator.fieldsValidator( //
        "name" //
        , new Validator.FieldValidators(Validator.require) {
          @Override
          public void setFieldVal(String val) {
            dpt.setName(val);
          }
        },
        "parentId" //
        , new Validator.FieldValidators(Validator.require, Validator.integer) {
          @Override
          public void setFieldVal(String val) {
            dpt.setParentId(Integer.parseInt(val));
          }
        }, validateRuleParams
      );


    ////////////////////
    IControlMsgHandler handler = new DelegateControl4JsonPostMsgHandler(this, this.parseJsonPost());
    if (!Validator.validate(handler, context, validateRule)) {
      return false;
    }

    Department parentDpt = this.getDepartmentDAO().selectByPrimaryKey(dpt.getParentId());
    dpt.setFullName("/" + parentDpt.getName() + "/" + dpt.getName());
    dpt.setGmtCreate(new Date());
    dpt.setGmtModified(new Date());
    dpt.setLeaf(true);
    dptProcess.process(dpt);
    this.setBizResult(context, dpt);
    return true;
  }

  interface IDepartmentProcess {
    void process(Department dpt);
  }

  /**
   * 删除部门
   *
   * @param context
   */
  public void doDeleteDepartment(Context context) {
    final Integer dptid = this.getInt("dptid");
    Assert.assertNotNull(dptid);
    UsrDptRelationCriteria rcriteria = null;
    DepartmentCriteria query = null;
    // 校验是否有子部门
    query = new DepartmentCriteria();
    query.createCriteria().andParentIdEqualTo(dptid);
    if (this.getDepartmentDAO().countByExample(query) > 0) {
      this.addErrorMessage(context, "该部门有子部门，不能删除");
      return;
    }
    // 校验是否有成员关联在该部门上
    rcriteria = new UsrDptRelationCriteria();
    rcriteria.createCriteria().andDptIdEqualTo(dptid);
    if (this.getUsrDptRelationDAO().countByExample(rcriteria) > 0) {
      this.addErrorMessage(context, "有成员关联在该部门，不能删除");
      return;
    }
    // 检验是否有应用绑定在部门上
    IAppsFetcher fetcher = getAppsFetcher();
    int appsCount = fetcher.count(new CriteriaSetter() {

      @Override
      public void set(Criteria criteria) {
        criteria.andDptIdEqualTo(dptid);
      }
    });
    if (appsCount > 0) {
      this.addErrorMessage(context, "该部门下有" + appsCount + "个应用，不能删除");
      return;
    }
    this.getDepartmentDAO().deleteByPrimaryKey(dptid);
    DataxAction.cleanDepsCache();
    this.addActionMessage(context, "已经成功删除部门:" + OrgAuthorityAction.getDepartmentName(this.getDepartmentDAO(), dptid));
  }

  // /**
  // * 解除会员和部门的关系
  // *
  // * @param context
  // */
  // public void doUnbindUser(Context context) {
  //
  // Integer duid = this.getInt("duid");
  // // Integer dptid = this.getInt("dptid");
  //
  // // Assert.assertNotNull(userid);
  // Assert.assertNotNull(duid);
  //
  // UsrDptRelation record = new UsrDptRelation();
  // record.setIsDeleted("Y");
  // UsrDptRelationCriteria query = new UsrDptRelationCriteria();
  // query.createCriteria().andUdIdEqualTo(duid);
  //
  // if (this.getUsrDptRelationDAO().updateByExampleSelective(record, query) >
  // 0) {
  // this.addActionMessage(context, "已经成功解除绑定");
  // return;
  // }
  //
  // this.addErrorMessage(context, "删除过程有错误");
  // }

  /**
   * 绑定会员
   *
   * @param context
   */
  public void doBindUser(Context context) {
    // Integer dptid = this.getInt("dptid");
    // Assert.assertNotNull(dptid);
    //
    // Integer userid = this.getInt("userid");
    //
    // if (userid == null) {
    // this.addErrorMessage(context, "请填写成员id");
    // return;
    // }
    //
    // final AdminUserDO user = this.getAuthService().getUserById(userid);
    //
    // if (user == null) {
    // this.addErrorMessage(context, "userid:" + userid + " 没有对应的用户实体存在");
    // return;
    // }
    // UsrDptRelationCriteria criteria = new UsrDptRelationCriteria();
    // criteria.createCriteria().andUsrIdEqualTo(userid);
    //
    // if (this.getUsrDptRelationDAO().countByExample(criteria) > 0) {
    //
    // this.addErrorMessage(context, "用户" + user.getName() + "("
    // + user.getId() + ") 已经绑定");
    // return;
    // }
    //
    // // 绑定会员
    // OrgAuthorityAction.bindUser2Dpt(this, dptid, new SysUser() {
    // private static final long serialVersionUID = 1L;
    // @Override
    // public int getId() {
    // return user.getId();
    // }
    // @Override
    // public String getName() {
    // return user.getName();
    // }
    //
    // });
    //
    // this.addActionMessage(context, "用户"
    // + user.getName()
    // + "("
    // + user.getId()
    // + ") 已经绑定到，"
    // + OrgAuthorityAction.getDepartmentName(this.getDepartmentDAO(),
    // dptid));
  }
}
