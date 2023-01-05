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
package com.qlangtech.tis.collect.servlet;

import java.io.IOException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.quartz.JobExecutionException;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.qlangtech.tis.ClusterStateCollectManager;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年5月4日上午10:04:12
 */
public class FullReportCreateServlet extends GenericServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        ClusterStateCollectManager appLauncherListener = WebApplicationContextUtils.getWebApplicationContext(this.getServletContext()).getBean("clusterStateCollectManager", ClusterStateCollectManager.class);
        if (appLauncherListener == null) {
            throw new IllegalStateException("servletContext obj can not be null");
        }
        try {
            appLauncherListener.exportReport();
        } catch (JobExecutionException e) {
            throw new ServletException(e);
        }
    }
}
