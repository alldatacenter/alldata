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
package com.qlangtech.tis.openapi.impl;

import com.qlangtech.tis.manage.biz.dal.dao.ISnapshotViewDAO;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.common.RunContext;
import com.qlangtech.tis.manage.common.SnapshotDomain;
import com.qlangtech.tis.openapi.SnapshotNotFindException;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-20
 */
public class SnapshotDomainGetter {

  // private static final long serialVersionUID = 1L;
  private final RunContext runContext;

  public SnapshotDomainGetter(RunContext runContext) {
    super();
    this.runContext = runContext;
  }

  // @Override
  public // HttpServletRequest
  SnapshotDomain getSnapshot(// request)
                             AppKey appKey) throws SnapshotNotFindException {
    SnapshotInfoFromRequest result = new SnapshotInfoFromRequest();
    // final String resources = getResources(request);
    if (appKey.getTargetSnapshotId() != null && appKey.getTargetSnapshotId() > 0) {
      result.snapshotId = appKey.getTargetSnapshotId().intValue();
    } else {
      final ServerGroup group = runContext.getServerGroupDAO().load(appKey.appName, appKey.groupIndex, appKey.runtime.getId());
      if (group == null) {
        throw new SnapshotNotFindException("appName:" + appKey.appName + " groupIndex:" + appKey.groupIndex + " runtime:"
          + appKey.runtime + " has not a corresponding server group in db");
      }
      if (group.getPublishSnapshotId() == null) {
        throw new SnapshotNotFindException("groupid:" + group.getGid() + " has not set publish snapshot id");
      }
      result.snapshotId = group.getPublishSnapshotId();
    }
    // 如果在request中设置了unmergeglobalparams 这个参数
    if (!appKey.unmergeglobalparams) {
      result.runtime = appKey.runtime;
    }
    if (result.snapshotId == null) {
      throw new IllegalStateException("result.snapshotId can not be null");
    }
    ISnapshotViewDAO snapshotViewDAO = runContext.getSnapshotViewDAO();
    if (snapshotViewDAO == null) {
      throw new IllegalStateException("snapshotViewDAO can not be null");
    }
    return snapshotViewDAO.getView(result.snapshotId);
  }
}
