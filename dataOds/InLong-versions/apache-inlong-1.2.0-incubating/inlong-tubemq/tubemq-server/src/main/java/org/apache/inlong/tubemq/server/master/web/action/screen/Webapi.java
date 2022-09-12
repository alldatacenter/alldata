/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.web.action.screen;

import static org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper.getWebApiRegInfo;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corerpc.exception.StandbyException;
import org.apache.inlong.tubemq.server.common.utils.WebParameterUtils;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.apache.inlong.tubemq.server.common.webbase.WebMethodMapper;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.web.handler.AbstractWebHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebAdminFlowRuleHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebAdminGroupCtrlHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebAdminTopicAuthHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebBrokerConfHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebGroupConsumeCtrlHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebGroupResCtrlHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebMasterInfoHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebOtherInfoHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebTopicCtrlHandler;
import org.apache.inlong.tubemq.server.master.web.handler.WebTopicDeployHandler;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;
import org.apache.inlong.tubemq.server.master.web.simplemvc.RequestContext;

/**
 * Public APIs for master
 *
 * Encapsulate query and modify logic mainly.
 * Generate output JSON by concatenating strings, to improve the performance.
 */
public class Webapi implements Action {

    private final TMaster master;

    public Webapi(TMaster master) {
        this.master = master;
        registerHandler(new WebBrokerConfHandler(this.master));
        registerHandler(new WebTopicCtrlHandler(this.master));
        registerHandler(new WebTopicDeployHandler(this.master));
        registerHandler(new WebGroupConsumeCtrlHandler(this.master));
        registerHandler(new WebGroupResCtrlHandler(this.master));
        registerHandler(new WebMasterInfoHandler(this.master));
        registerHandler(new WebOtherInfoHandler(this.master));
        registerHandler(new WebAdminGroupCtrlHandler(this.master));
        registerHandler(new WebAdminTopicAuthHandler(this.master));
        registerHandler(new WebAdminFlowRuleHandler(this.master));
    }

    @Override
    public void execute(RequestContext requestContext) {
        String method = null;
        ProcessResult result = new ProcessResult();
        StringBuilder sBuffer = new StringBuilder();
        long startTime = System.currentTimeMillis();
        try {
            HttpServletRequest req = requestContext.getReq();
            if (this.master.isStopped()) {
                throw new Exception("Server is stopping...");
            }
            MetaDataService defMetaDataService = this.master.getMetaDataService();
            if (!defMetaDataService.isSelfMaster()) {
                throw new StandbyException("Please send your request to the master Node.");
            }
            method = req.getParameter("method");
            String strCallbackFun = req.getParameter("callback");
            if ((TStringUtils.isNotEmpty(strCallbackFun))
                    && (strCallbackFun.length() <= TBaseConstants.META_MAX_CALLBACK_STRING_LENGTH)
                    && (strCallbackFun.matches(TBaseConstants.META_TMP_CALLBACK_STRING_VALUE))) {
                strCallbackFun = strCallbackFun.trim();
            }
            if (method == null) {
                throw new Exception("Please take with method parameter!");
            }
            WebMethodMapper.WebApiRegInfo webApiRegInfo = getWebApiRegInfo(method);
            if (webApiRegInfo == null) {
                throw new Exception("unsupported method!");
            }
            // check master is current node
            if (webApiRegInfo.onlyMasterOp
                    && defMetaDataService.isPrimaryNodeActive()) {
                throw new Exception(
                        "DesignatedPrimary happened...please check if the other member is down");
            }
            // valid operation authorize info
            if (!WebParameterUtils.validReqAuthorizeInfo(req,
                    webApiRegInfo.needAuthToken, master, sBuffer, result)) {
                throw new Exception(result.getErrMsg());
            }
            sBuffer = (StringBuilder) webApiRegInfo.method.invoke(
                    webApiRegInfo.webHandler, req, sBuffer, result);
            // Carry callback information
            if (TStringUtils.isEmpty(strCallbackFun)) {
                requestContext.put("sb", sBuffer.toString());
            } else {
                requestContext.put("sb", strCallbackFun + "(" + sBuffer.toString() + ")");
                requestContext.getResp().addHeader("Content-type", "text/plain");
                requestContext.getResp().addHeader("charset", TBaseConstants.META_DEFAULT_CHARSET_NAME);
            }
        } catch (Throwable e) {
            sBuffer.append("{\"result\":false,\"errCode\":400,\"errMsg\":\"Bad request from client, ")
                    .append(e.getMessage()).append("\"}");
            requestContext.put("sb", sBuffer.toString());
        } finally {
            WebCallStatsHolder.addMethodCall(method, System.currentTimeMillis() - startTime);
        }
    }

    private void registerHandler(AbstractWebHandler abstractWebHandler) {
        abstractWebHandler.registerWebApiMethod();
    }

}
