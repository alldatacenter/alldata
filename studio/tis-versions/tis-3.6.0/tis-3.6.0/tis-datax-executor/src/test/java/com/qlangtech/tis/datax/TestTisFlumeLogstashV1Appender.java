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

package com.qlangtech.tis.datax;

import com.qlangtech.tis.job.common.JobCommon;
import com.qlangtech.tis.web.start.TisAppLaunch;
import junit.framework.TestCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-25 15:04
 **/
public class TestTisFlumeLogstashV1Appender extends TestCase {
    private static final Logger logger = LoggerFactory.getLogger(TestTisFlumeLogstashV1Appender.class);

    public void testSendMsg() throws Exception {
        TisAppLaunch.setTest(false);
        MDC.put(JobCommon.KEY_TASK_ID, String.valueOf(999));
        MDC.put(JobCommon.KEY_COLLECTION, "baisui");
        int i = 0;
        while (true) {
            logger.info("i am so hot:" + (i++));
            Thread.sleep(1000l);
            System.out.println("send turn:" + i);
        }
    }
}
