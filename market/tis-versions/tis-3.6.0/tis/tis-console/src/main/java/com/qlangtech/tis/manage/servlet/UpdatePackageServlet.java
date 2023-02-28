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
package com.qlangtech.tis.manage.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2011-12-31
 */
public class UpdatePackageServlet extends BasicServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println(IOUtils.toString(req.getInputStream()));
    // Integer pid = Integer.parseInt(req.getParameter("pid"));
    // final String username = req.getParameter("username");
    //
    // if (StringUtils.isEmpty(username)) {
    // throw new IllegalArgumentException("username:" + username
    // + " can not be null");
    // }
    //
    // // 更新数据库
    // AppPackageCriteria criteria = new AppPackageCriteria();
    // criteria.createCriteria().andPidEqualTo(pid);
    // AppPackage pack = new AppPackage();
    // pack.setTestStatus(SnapshotCriteria.TEST_STATE_BUILD_INDEX);
    // pack.setSuccessSnapshotId(Integer.parseInt(req
    // .getParameter("snapshotid")));
    // pack.setLastTestTime(new Date());
    // pack.setLastTestUser(username);
    // getContext().getAppPackageDAO()
    // .updateByExampleSelective(pack, criteria);
    }
}
