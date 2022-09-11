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

package org.apache.inlong.agent.common;

import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.utils.AgentDbUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAgentUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestAgentUtils.class);

    @Test
    public void testReplaceDynamicSequence() throws Exception {
        String[] result = AgentDbUtils.replaceDynamicSeq("${1, 99}");
        assert result != null;
        String[] expectResult = new String[99];
        for (int index = 1; index < 100; index++) {
            expectResult[index - 1] = String.valueOf(index);
        }
        Assert.assertArrayEquals(expectResult, result);

        result = AgentDbUtils.replaceDynamicSeq("${0x0, 0xf}");
        expectResult = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a",
            "b", "c", "d", "e", "f"};
        Assert.assertArrayEquals(expectResult, result);

        result = AgentDbUtils.replaceDynamicSeq("${O01,O10}");
        expectResult = new String[]{"01", "02", "03", "04", "05",
            "06", "07", "10"};
        Assert.assertArrayEquals(expectResult, result);
    }

    @Test
    public void testDateFormatter() {
        String time = AgentUtils.formatCurrentTime("yyyyMMdd HH:mm:ss");
        LOGGER.info("agent time is {}", time);
    }

    @Test
    public void testDateFormatterWithOffset() {
        String timeOffset = "-1d";
        String number = StringUtils.substring(timeOffset, 0, timeOffset.length() - 1);
        String mark = StringUtils.substring(timeOffset, timeOffset.length() - 1);
        Assert.assertEquals("-1", number);
        Assert.assertEquals("d", mark);
        String time = AgentUtils.formatCurrentTimeWithOffset("yyyyMMdd HH:mm:ss", 1, 1, 1);
        LOGGER.info("agent time is {}", time);
        time = AgentUtils.formatCurrentTimeWithOffset("yyyyMMdd HH:mm:ss", -1, -1, -1);
        LOGGER.info("agent time is {}", time);
    }

    @Test
    public void testParseAddictiveStr() {
        String addStr = "m=10&__addcol1__worldid=&t=1";
        Map<String, String> attr = AgentUtils.getAdditionAttr(addStr);
        Assert.assertEquals("", attr.get("__addcol1__worldid"));
        Assert.assertEquals("1", attr.get("t"));
    }

    @Test
    public void testTimeConvertToMillsec() {
        Assert.assertEquals(1620316800000L, AgentUtils.timeStrConvertToMillSec("202105071554", "D"));
        Assert.assertEquals(1620370800000L, AgentUtils.timeStrConvertToMillSec("202105071554", "H"));
        Assert.assertEquals(1620374040000L, AgentUtils.timeStrConvertToMillSec("202105071554", "M"));
    }

}
