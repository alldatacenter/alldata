/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.qlangtech.tis.runtime.module.action;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.manage.biz.dal.pojo.UsrDptRelationCriteria;
import com.qlangtech.tis.manage.common.ManageUtils;
import com.qlangtech.tis.manage.common.Secret;
import com.qlangtech.tis.manage.common.TISHttpServletRequestWrapper;
import com.qlangtech.tis.manage.common.UserUtils;
import com.qlangtech.tis.web.start.TisAppLaunch;
import org.apache.commons.lang.StringUtils;
import org.apache.struts2.ServletActionContext;
import org.apache.struts2.dispatcher.StrutsRequestWrapper;

/**
 * 登录相应
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-20
 */
public class LoginAction extends BasicModule {

  private static final long serialVersionUID = 1L;

  public static final String TERMINATOR_INDEX_PAGE_PATH = "/runtime";

  private static final String USER_TOKEN_cryptKey = "%*)&^*(";

  /**
   * 取得当前zeppelin 状态是否激活，客户端中可以判断是否要初始化 zeppelin的websocket组件
   *
   * @param context
   */
  public void doGetZeppelinStatus(Context context) {
    boolean zeppelinActivate = TisAppLaunch.get().isZeppelinActivate();
    this.setBizResult(context, zeppelinActivate);
  }

  public void doLogin(Context context) throws Exception {
    // Map<String, String> userToken = Config.getUserToken();
    final String username = this.getString("username");
    if (StringUtils.isEmpty(username)) {
      this.addErrorMessage(context, "请填写用户名");
      return;
    }
    final String password = this.getString("password");
    if (StringUtils.isEmpty(password)) {
      this.addErrorMessage(context, "请填写密码");
      return;
    }
    UsrDptRelationCriteria usrQuery = new UsrDptRelationCriteria();
    usrQuery.createCriteria().andUserNameEqualTo(username).andPasswordEqualTo(ManageUtils.md5(password));
    if (this.getUsrDptRelationDAO().countByExample(usrQuery) < 1) {
      this.addErrorMessage(context, "非法账户");
      return;
    }
    final String host = this.getRequest().getHeader("Host");
    ChangeDomainAction.addCookie(getResponse(), UserUtils.USER_TOKEN, Secret.encrypt(username, USER_TOKEN_cryptKey), StringUtils.substringBefore(host, ":"), 60 * 60 * 24 * 365);
    this.getRundata().redirectTo(this.getRequest().getContextPath() + TERMINATOR_INDEX_PAGE_PATH);
  }

  public static String getDcodeUserName(String encryptUserName) {
    if (StringUtils.isBlank(encryptUserName)) {
      throw new IllegalArgumentException("encryptUserName can not be null");
    }
    try {
      return Secret.decrypt(encryptUserName, USER_TOKEN_cryptKey);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * 退出登录
   *
   * @param
   * @param context
   * @throws Exception
   */
  public void doLogout(Context context) throws Exception {
    // this.getRequest().getSession(true);
    ServletActionContext.getRequest().getSession().removeAttribute(UserUtils.USER_TOKEN_SESSION);
    final String host = this.getRequest().getHeader("Host");
    ChangeDomainAction.addCookie(getResponse(), UserUtils.USER_TOKEN, "", StringUtils.substringBefore(host, ":"), 0);
    final TISHttpServletRequestWrapper request = (TISHttpServletRequestWrapper) (((StrutsRequestWrapper) this.getRequest()).getRequest());
    request.removeCookie(UserUtils.USER_TOKEN);
    getRundataInstance().redirectTo("/runtime/login.htm");
  }
}
