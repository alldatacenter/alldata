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
import com.koubei.web.tag.pager.Pager;
import com.qlangtech.tis.ibatis.BasicCriteria;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.dao.IOperationLogDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.OperationLogCriteria;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import junit.framework.Assert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月8日
 */
public class OperationLogAction extends BasicModule {

  private static final long serialVersionUID = 1L;

  private IOperationLogDAO operationLogDAO;

  /**
   * @param context
   */
  public void doGetErrorDetail(Context context) {
    final String logFileName = this.getString("logfilename");
    this.setBizResult(context, TisException.getLogError(logFileName).getDetail());
  }

  public void doGetErrorLogList(Context context) {
    Pager pager = this.createPager();
    pager.setRowsPerPage(10);
    List<TisException.ErrMsg> errorLogs = TisException.getErrorLogs();
    pager.setTotalCount(errorLogs.size());

    BasicCriteria criteria = new BasicCriteria();
    criteria.setPageSize(pager.getRowsPerPage());
    criteria.setPage(pager.getCurPage());

    int fromIndex = criteria.getSkip();
    int toIndex = fromIndex + pager.getRowsPerPage();
    int listSize = errorLogs.size();
    if ((toIndex) >= listSize) {
      toIndex = listSize;
    }
    List<TisException.ErrMsg> pageView = errorLogs.subList(fromIndex, toIndex);
    pageView.forEach((err) -> {
      TisException.ILogErrorDetail logError = TisException.getLogError(err.getLogFileName());
      err.setAbstractInfo(logError.getAbstractInfo());
    });
    this.setBizResult(context, new PaginationResult(pager, pageView));
  }

  /**
   * log 显示页面显示日志信息
   *
   * @param context
   */
  public void doGetInitData(Context context) {
    OperationLogCriteria query = createOperationLogCriteria();
    Pager pager = this.createPager();
    query.setOrderByClause("op_id desc");
    pager.setTotalCount(this.operationLogDAO.countByExample(query));
    this.setBizResult(context
      , new PaginationResult(pager
        , this.operationLogDAO.selectByExampleWithoutBLOBs(query, pager.getCurPage(), pager.getRowsPerPage())));
  }

  protected OperationLogCriteria createOperationLogCriteria() {
    final String appName = this.getAppDomain().getAppName();
    OperationLogCriteria lcriteria = new OperationLogCriteria();
    if (StringUtils.isBlank(appName)) {
      return lcriteria;
    }
    // RunEnvironment runtime =  RunEnvironment.getSysEnvironment();
    final RunEnvironment runtime = RunEnvironment.getSysRuntime();
    Assert.assertNotNull(appName);
    // Assert.assertNotNull(this.getString("tab"));
    // Assert.assertNotNull(this.getString("opt"));
    OperationLogCriteria.Criteria criteria = lcriteria.createCriteria().andAppNameEqualTo(appName);
    // }
    return lcriteria;
  }

  protected int getPageSize() {
    return PAGE_SIZE;
  }

  @Func(PermissionConstant.APP_BUILD_RESULT_VIEW)
  public void doGetDetail(Context context) throws Exception {
    this.disableNavigationBar(context);
    Integer opid = this.getInt("opid");
    Assert.assertNotNull(opid);
    // this.addActionMessage(context, operationLogDAO.loadFromWriteDB(opid).getOpDesc());
    this.setBizResult(context, operationLogDAO.loadFromWriteDB(opid));
  }

  @Autowired
  public void setOperationLogDAO(IOperationLogDAO operationLogDAO) {
    this.operationLogDAO = operationLogDAO;
  }
}
