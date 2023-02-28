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
package com.qlangtech.tis.runtime.module.action.jarcontent;

import com.alibaba.citrus.turbine.Context;
import junit.framework.Assert;
import com.opensymphony.xwork2.ModelDriven;
import com.qlangtech.tis.manage.GroupChangeSnapshotForm;
import com.qlangtech.tis.manage.PermissionConstant;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroup;
import com.qlangtech.tis.manage.biz.dal.pojo.ServerGroupCriteria;
import com.qlangtech.tis.manage.spring.aop.Func;
import com.qlangtech.tis.runtime.module.action.BasicModule;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-16
 */
public class GroupChangeSnapshotAction extends BasicModule implements ModelDriven<GroupChangeSnapshotForm> {

    private static final long serialVersionUID = 1L;

    private final GroupChangeSnapshotForm form = new GroupChangeSnapshotForm();

    @Override
    public GroupChangeSnapshotForm getModel() {
        return this.form;
    }

    @Func(PermissionConstant.CONFIG_SNAPSHOT_CHANGE)
    public // Navigator nav,
    void doChange(Context context) throws Exception {
        Assert.assertNotNull("form can not be null", form);
        Assert.assertNotNull("form.getGroupSnapshot() can not be null", form.getGroupSnapshot());
        Assert.assertNotNull("form.getSnapshotId() can not be null", form.getSnapshotId());
        ServerGroupCriteria criteria = new ServerGroupCriteria();
        criteria.createCriteria().andGidEqualTo(form.getGroupId()).andRuntEnvironmentEqualTo(this.getAppDomain().getRunEnvironment().getId());
        ServerGroup group = new ServerGroup();
        group.setPublishSnapshotId(form.getSnapshotId());
        if (this.getServerGroupDAO().updateByExampleSelective(group, criteria) < 1) {
            throw new IllegalArgumentException("has not update success");
        }
        this.addActionMessage(context, "已经将第" + this.getServerGroupDAO().selectByPrimaryKey(form.getGroupId()).getGroupIndex() + "组，发布快照切换成：snapshot" + form.getSnapshotId());
    }
}
