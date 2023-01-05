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

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年5月4日下午1:00:49
 */
public class DailyReportCreateServlet extends GenericServlet {

    private static final long serialVersionUID = 1L;

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
    // SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    //
    // try {
    // Date date = format.parse(req.getParameter("date"));
    // //Map<String, ICoreStatistics> empty = Collections.emptyMap();
    // AppLauncherListener.createDailyReport(
    // AppLauncherListener.getTSearcherClusterInfoCollect(), date);
    // } catch (Exception e) {
    // throw new ServletException(e);
    // }
    }
}
