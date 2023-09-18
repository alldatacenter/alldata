/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gilt.logback.flume.tis;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.gilt.logback.flume.FlumeLogstashV1Appender;
import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.realtime.utils.NetUtils;
import org.apache.commons.lang.StringUtils;

import java.net.UnknownHostException;
import java.util.Map;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年4月15日
 */
public class TisFlumeLogstashV1Appender extends FlumeLogstashV1Appender {

    public static TisFlumeLogstashV1Appender instance;

    public TisFlumeLogstashV1Appender() {
        super();
        if (instance != null) {
            throw new IllegalStateException(this.getClass().getSimpleName() + " shall have not been initialize");
        }
        instance = this;
        super.setFlumeAgents(Config.getAssembleHost() + ":" + Config.LogFlumeAddressPORT);
    }

    public void setFlumeAgents(String flumeAgents) {
    }

    @Override
    public void append(ILoggingEvent eventObject) {
//        if (TisAppLaunch.isTestMock()) {
//            return;
//        }
        final Map<String, String> mdc = eventObject.getMDCPropertyMap();
        String taskId = mdc.get(JobCommon.KEY_TASK_ID);
        if (taskId == null) {
            return;
        }
        super.append(eventObject);
    }

    @Override
    protected Map<String, String> extractHeaders(ILoggingEvent eventObject) {
        Map<String, String> result = super.extractHeaders(eventObject);
        final Map<String, String> mdc = eventObject.getMDCPropertyMap();

        String taskId = mdc.get(JobCommon.KEY_TASK_ID);

        String collection = StringUtils.defaultIfEmpty(mdc.get(JobCommon.KEY_COLLECTION), "unknown");
        result.put(JobCommon.KEY_COLLECTION, collection);
        if (taskId != null) {
            result.put(JobCommon.KEY_TASK_ID, taskId);
        }
        return result;
    }

    @Override
    protected String resolveHostname() throws UnknownHostException {
        return NetUtils.getHost();
    }

    //    @Override
//    protected HashMap<String, String> createHeaders() {
//        HashMap<String, String> headers = new HashMap<>();
//        //headers.put(ENVIRONMENT_INCR_EXEC_GROUP, "tis-datax");
//        return headers;
//    }
}
