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
import com.google.common.collect.Maps;
import com.koubei.web.tag.pager.Pager;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.pojo.Department;
import com.qlangtech.tis.manage.biz.dal.pojo.DepartmentCriteria;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelation;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.ManageUtils;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.manage.common.UserUtils;
import com.qlangtech.tis.manage.common.valve.AjaxValve;
import com.qlangtech.tis.manage.spring.aop.Func;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * 用户更新
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-3-1
 */
public class UserAction extends BasicModule {

  private static final long serialVersionUID = 1L;
  // private static Properties tisMetaProps;

  /**
   * @param context
   */
  public void doGetUserInfo(Context context) throws Exception {
    Map<String, Object> sysInfo = Maps.newHashMap();
    sysInfo.put("usr", UserUtils.getUser(this.getRequest(), this.getDaoContext()));
    sysInfo.put("sysInitialized", SysInitializeAction.isSysInitialized());

//    if (tisMetaProps == null) {
//      try (InputStream reader = UserAction.class.getResourceAsStream("/tis-meta")) {
//        tisMetaProps = new Properties();
//        tisMetaProps.load(reader);
//      }
//    }
    sysInfo.put("tisMeta", Config.getMetaProps().tisMetaProps);
    this.setBizResult(context, sysInfo);
  }

  /**
   * 取得初始化頁面數據
   *
   * @param context
   */
  public void doGetInitData(Context context) {
    UsrDptRelationCriteria criteria = new UsrDptRelationCriteria();
    criteria.setOrderByClause("create_time desc");
    Pager pager = createPager();
    pager.setTotalCount(this.getUsrDptRelationDAO().countByExample(criteria));
    this.setBizResult(context, new PaginationResult(pager, this.getUsrDptRelationDAO().selectByExample(criteria, pager.getCurPage(), pager.getRowsPerPage())));
  }

  /**
   * get child department
   *
   * @param context
   */
  public void doGetChildDpt(Context context) {
    Integer dptid = this.getInt("pid");
    DepartmentCriteria query = new DepartmentCriteria();
    query.createCriteria().andParentIdEqualTo(dptid);
    List<Option> result = new ArrayList<Option>();
    for (Department dpt : this.getDepartmentDAO().selectByExample(query)) {
      result.add(new Option(dpt.getName(), String.valueOf(dpt.getDptId())));
    }
    context.put(AjaxValve.BIZ_RESULT, result);
  }

  /**
   * 添加用户，默认是给系统用户添加的
   *
   * @param context
   */
  public void doUsrAdd(Context context) {
    String userAccount = this.getString("userAccount");
    String realName = this.getString("realName");
    String password = this.getString("password");
    Integer dptid = this.getInt("dptid");
    if (StringUtils.isEmpty(userAccount)) {
      this.addErrorMessage(context, "请填写登陆账户名");
      return;
    }
    if (StringUtils.isEmpty(realName)) {
      this.addErrorMessage(context, "请填写真实名称");
      return;
    }
    if (StringUtils.isEmpty(password)) {
      this.addErrorMessage(context, "请填写密码");
      return;
    }
    UsrDptRelationCriteria q = new UsrDptRelationCriteria();
    q.createCriteria().andUserNameEqualTo(userAccount);
    if (this.getUsrDptRelationDAO().countByExample(q) > 0) {
      this.addErrorMessage(context, "用户“" + userAccount + "”已经存在");
      return;
    }
    UsrDptRelation user = new UsrDptRelation();
    user.setUsrId(StringUtils.remove(UUID.randomUUID().toString(), "-"));
    user.setUserName(userAccount);
    user.setRealName(realName);
    user.setPassword(ManageUtils.md5(password));
    user.setDptId(dptid);
    user.setCreateTime(new Date());
    user.setUpdateTime(new Date());
    user.setDptId(-1);
    user.setDptName("none");
    user.setrId(-1);
    this.getUsrDptRelationDAO().insertSelective(user);
    this.addActionMessage(context, "用户“" + userAccount + "”添加成功");
  }

  /**
   * 用户部门更新
   *
   * @param context
   */
  @Func(PermissionConstant.AUTHORITY_USER_MANAGE)
  public void doChangeDepartment(Context context) {
    String usrid = this.getString("usrid");
    Integer selecteddptid = this.getInt("selecteddptid");
    Assert.assertNotNull(usrid);
    Assert.assertNotNull(selecteddptid);
    UsrDptRelation usr = this.getUsrDptRelationDAO().loadFromWriteDB(usrid);
    Assert.assertNotNull(usr);
    Department department = this.getDepartmentDAO().loadFromWriteDB(selecteddptid);
    Assert.assertNotNull(department);
    UsrDptRelationCriteria criteria = new UsrDptRelationCriteria();
    criteria.createCriteria().andUsrIdEqualTo(usrid);
    UsrDptRelation record = new UsrDptRelation();
    record.setDptName(department.getFullName());
    record.setDptId(department.getDptId());
    record.setUpdateTime(new Date());
    this.getUsrDptRelationDAO().updateByExampleSelective(record, criteria);
    this.addActionMessage(context, "已经将用户“" + usr.getUserName() + "”归属到新的部门：" + department.getFullName());
  }
}
