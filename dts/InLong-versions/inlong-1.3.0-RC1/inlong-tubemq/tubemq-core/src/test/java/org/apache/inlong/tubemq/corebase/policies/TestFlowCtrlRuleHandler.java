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

package org.apache.inlong.tubemq.corebase.policies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;

public class TestFlowCtrlRuleHandler {

    private static String mockFlowCtrlInfo() {
        // 0: current limit, 1: frequency limit, 2: SSD transfer 3: request frequency control
        return "[{\"type\":0,\"rule\":[{\"start\":\"08:00\",\"end\":\"17:59\",\"dltInM\":1024,"
                + "\"limitInM\":20,\"freqInMs\":1000},{\"start\":\"18:00\",\"end\":\"22:00\","
                + "\"dltInM\":1024,\"limitInM\":20,\"freqInMs\":5000}]},{\"type\":2,\"rule\""
                + ":[{\"start\":\"12:00\",\"end\":\"23:59\",\"dltStInM\":20480,\"dltEdInM\":2048}]}"
                + ",{\"type\":1,\"rule\":[{\"zeroCnt\":3,\"freqInMs\":300},{\"zeroCnt\":8,\"freqInMs\""
                + ":1000}]},{\"type\":3,\"rule\":[{\"normFreqInMs\":0,\"filterFreqInMs\":100,"
                + "\"minDataFilterFreqInMs\":400}]}]";
    }

    @Test
    public void testFlowCtrlRuleHandler() {
        try {
            FlowCtrlRuleHandler handler = new FlowCtrlRuleHandler(true);
            handler.updateFlowCtrlInfo(2, 10, mockFlowCtrlInfo());
            TimeZone timeZone = TimeZone.getTimeZone("GMT+8:00");
            Calendar rightNow = Calendar.getInstance(timeZone);
            int hour = rightNow.get(Calendar.HOUR_OF_DAY);
            int minu = rightNow.get(Calendar.MINUTE);
            int curTime = hour * 100 + minu;

            // current data limit test
            FlowCtrlResult result = handler.getCurDataLimit(2000);
            if (curTime >= 800 && curTime <= 1759) {
                assertEquals(result.dataLtInSize, 20 * 1024 * 1024L);
                assertEquals(1000L, result.freqLtInMs);
            } else if (curTime >= 1800 && curTime < 2200) {
                assertEquals(result.dataLtInSize, 20 * 1024 * 1024L);
                assertEquals(5000L, result.freqLtInMs);
            } else {
                assertNull("result should be null", result);
            }
            result = handler.getCurDataLimit(1000);
            assertNull("result should be null", result);

            //  request frequency control
            FlowCtrlItem item = handler.getFilterCtrlItem();
            assertEquals(item.getDataLtInSZ(), 0);
            assertEquals(item.getZeroCnt(), 400);
            assertEquals(item.getFreqLtInMs(), 100);

            //check values
            assertEquals(handler.getNormFreqInMs(), 100);
            assertEquals(handler.getFlowCtrlId(), 10);
            assertEquals(handler.getMinDataFreqInMs(), 400);
            assertEquals(handler.getMinZeroCnt(), 3);
            assertEquals(handler.getQryPriorityId(), 2);
            assertEquals(handler.getFlowCtrlId(), 10);

            System.out.println();
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
