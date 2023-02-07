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

package org.apache.inlong.tubemq.server.common.webbase;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebMethodMapper {

    // log printer
    private static final Logger logger =
            LoggerFactory.getLogger(WebMethodMapper.class);
    private static final Map<String, WebApiRegInfo> WEB_METHOD_MAP =
            new HashMap<>();

    public static WebApiRegInfo getWebApiRegInfo(String webMethodName) {
        return WEB_METHOD_MAP.get(webMethodName);
    }

    public static void registerWebMethod(String webMethodName,
            String clsMethodName,
            boolean onlyMasterOp,
            boolean needAuthToken,
            Object webHandler) {
        Method[] methods = webHandler.getClass().getMethods();
        for (Method item : methods) {
            if (item.getName().equals(clsMethodName)) {
                WEB_METHOD_MAP.put(webMethodName,
                        new WebApiRegInfo(item, webHandler, onlyMasterOp, needAuthToken));
                return;
            }
        }
        logger.error(new StringBuilder(512)
                .append("registerWebMethod failure, not found Method by clsMethodName ")
                .append(clsMethodName).append(" in WebHandler class ")
                .append(webHandler.getClass().getName()).toString());
    }

    public static int getRegisteredWebMethod(StringBuilder sBuffer) {
        int totalCnt = 0;
        if (WEB_METHOD_MAP.isEmpty()) {
            return totalCnt;
        }
        for (Map.Entry<String, WebMethodMapper.WebApiRegInfo> entry : WEB_METHOD_MAP.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            if (totalCnt++ > 0) {
                sBuffer.append(",");
            }
            sBuffer.append("{\"method\":\"").append(entry.getKey())
                    .append("\",\"needAuth\":").append(entry.getValue().needAuthToken)
                    .append("}");
        }
        return totalCnt;
    }

    public static class WebApiRegInfo {

        public Method method;
        public Object webHandler;
        public boolean onlyMasterOp = false;
        public boolean needAuthToken = false;

        public WebApiRegInfo(Method method,
                Object webHandler,
                boolean onlyMasterOp,
                boolean needAuthToken) {
            this.method = method;
            this.webHandler = webHandler;
            this.onlyMasterOp = onlyMasterOp;
            this.needAuthToken = needAuthToken;
        }
    }

}
