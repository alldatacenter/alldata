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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.TIS;
import com.qlangtech.tis.exec.impl.TrackableExecuteInterceptor;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.fullbuild.phasestatus.PhaseStatusCollection;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.util.DescriptorsJSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 通过taskid取全量执行状态
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年7月12日
 */
public class TaskStatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory.getLogger(TaskStatusServlet.class);

    protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        String extendPoint = null;
        try {
            if (StringUtils.isNotEmpty(extendPoint = req.getParameter(DescriptorsJSON.KEY_EXTEND_POINT))) {

                int removeCount = TIS.cleanPluginStore((Class<Describable>) Class.forName(extendPoint));
//                IPluginStore<Describable> pluginStore = TIS.getPluginStore((Class<Describable>) Class.forName(extendPoint));
//                pluginStore.cleanPlugins();
                logger.info("key of '{}' pluginStore has been clean,remove count:{}", extendPoint, removeCount);
                return;
            }
        } catch (ClassNotFoundException e) {
            throw new ServletException("clean plugin store cache faild ", e);
        }

        if (Boolean.parseBoolean(req.getParameter(TIS.KEY_ACTION_CLEAN_TIS))) {
            TIS.clean();
            logger.info(" clean TIS cache", extendPoint);
            return;
        }


        int taskid = Integer.parseInt(req.getParameter(JobCommon.KEY_TASK_ID));
        // 是否要获取全部的日志信息，比如dump已經完成了，那麼只需要獲取dump之後的日志信息
        // boolean all = Boolean.parseBoolean(req.getParameter("all"));
        PhaseStatusCollection statusSet = TrackableExecuteInterceptor.getTaskPhaseReference(taskid);
        JSONObject result = new JSONObject();
        boolean success = false;
        if (statusSet != null) {
            result.put("status", statusSet);
            success = true;
        }
        result.put("success", success);
        IOUtils.write(JSON.toJSONString(result, true), res.getWriter());
    }
}
