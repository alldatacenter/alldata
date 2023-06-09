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
package com.qlangtech.tis.fullbuild.servlet;

import com.qlangtech.tis.fullbuild.taskflow.TaskWorkflow;
import com.qlangtech.tis.fullbuild.taskflow.WorkflowTaskConfigParser;
import com.qlangtech.tis.git.GitUtils;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通过索引名称 取得索引workflow的依赖关系，提供给页面上的vis-4.21.0来绘制工作流的可视化图
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年12月4日
 */
public class TisWorkflowParserServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String workflowName = req.getParameter("wfname");
            if (StringUtils.isEmpty(workflowName)) {
                throw new IllegalArgumentException("param 'workflowName' can not be null");
            }
            // 日常测试
            GitUtils.GitBranchInfo branch = GitUtils.GitBranchInfo.$(GitUtils.GitBranch.DEVELOP);
            WorkflowTaskConfigParser wfParser = WorkflowTaskConfigParser.getInstance(workflowName, branch);
            TaskWorkflow workflow = wfParser.getWorkflow();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
