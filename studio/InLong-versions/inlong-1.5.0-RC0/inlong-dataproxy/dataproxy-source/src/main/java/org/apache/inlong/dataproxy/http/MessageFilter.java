/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.dataproxy.http;

import org.apache.commons.lang3.StringUtils;
import org.apache.flume.ChannelException;
import org.apache.inlong.common.enums.DataProxyErrCode;
import org.apache.inlong.common.msg.AttributeConstants;
import org.apache.inlong.dataproxy.config.ConfigManager;
import org.apache.inlong.dataproxy.consts.AttrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class MessageFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(MessageFilter.class);

    private final int maxMsgLength;

    public MessageFilter(int maxMsgLength) {
        this.maxMsgLength = maxMsgLength;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request,
            ServletResponse response,
            FilterChain chain) throws IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String pathInfo = req.getPathInfo();
        if (pathInfo.startsWith("/")) {
            pathInfo = pathInfo.substring(1);
        }
        if ("heartbeat".equals(pathInfo)) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.SUCCESS.getErrCode(),
                    DataProxyErrCode.SUCCESS.getErrMsg());
            return;
        }
        // check sink service status
        if (!ConfigManager.getInstance().isMqClusterReady()) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.SINK_SERVICE_UNREADY.getErrCode(),
                    DataProxyErrCode.SINK_SERVICE_UNREADY.getErrMsg());
            return;
        }
        // get and check groupId
        String groupId = req.getParameter(AttributeConstants.GROUP_ID);
        if (StringUtils.isEmpty(groupId)) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.MISS_REQUIRED_GROUPID_ARGUMENT.getErrCode(),
                    DataProxyErrCode.MISS_REQUIRED_GROUPID_ARGUMENT.getErrMsg());
            return;
        }
        // get and check streamId
        String streamId = req.getParameter(AttributeConstants.STREAM_ID);
        if (StringUtils.isEmpty(streamId)) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.MISS_REQUIRED_STREAMID_ARGUMENT.getErrCode(),
                    DataProxyErrCode.MISS_REQUIRED_STREAMID_ARGUMENT.getErrMsg());
            return;
        }
        // get and check dt
        String dt = req.getParameter(AttributeConstants.DATA_TIME);
        if (StringUtils.isEmpty(dt)) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.MISS_REQUIRED_DT_ARGUMENT.getErrCode(),
                    DataProxyErrCode.MISS_REQUIRED_DT_ARGUMENT.getErrMsg());
            return;
        }
        // get and check body
        String body = req.getParameter(AttrConstants.BODY);
        if (StringUtils.isEmpty(body)) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.MISS_REQUIRED_BODY_ARGUMENT.getErrCode(),
                    DataProxyErrCode.MISS_REQUIRED_BODY_ARGUMENT.getErrMsg());
            return;
        }
        // check body length
        if (body.length() > maxMsgLength) {
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.BODY_EXCEED_MAX_LEN.getErrCode(),
                    "Bad request, body length exceeds the limit:" + maxMsgLength);
            return;
        }
        try {
            chain.doFilter(request, response);
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.SUCCESS.getErrCode(),
                    DataProxyErrCode.SUCCESS.getErrMsg());
        } catch (Throwable t) {
            String errMsg;
            if ((t instanceof ChannelException)) {
                errMsg = "Channel error! " + t.getMessage();
            } else {
                errMsg = "Service error! " + t.getMessage();
            }
            LOG.error("Request error!", t);
            returnRspPackage(resp, req.getCharacterEncoding(),
                    DataProxyErrCode.UNKNOWN_ERROR.getErrCode(), errMsg);
        }
    }

    @Override
    public void destroy() {
    }

    private void returnRspPackage(HttpServletResponse resp, String charEncoding,
            int errCode, String errMsg) throws IOException {
        StringBuilder builder =
                new StringBuilder().append("{\"code\":\"").append(errCode)
                        .append("\",\"msg\":\"").append(errMsg).append("\"}");
        resp.setCharacterEncoding(charEncoding);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(builder.toString());
        resp.flushBuffer();
    }
}
