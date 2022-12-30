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

package org.apache.inlong.tubemq.server.common;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.junit.Assert;
import org.junit.Test;

/**
 * WebCallStatsHolder test.
 */
public class WebCallStatsHolderTest {

    @Test
    public void testWebCallStatsHolder() {
        // call method, test, 3, 10, 500
        WebCallStatsHolder.addMethodCall("test", 20);
        WebCallStatsHolder.addMethodCall("test", 10);
        WebCallStatsHolder.addMethodCall("test", 500);
        // call method aaa, 1, 50, 50
        WebCallStatsHolder.addMethodCall("aaa", 50);
        // check result
        Map<String, Long> retMap = new LinkedHashMap<>();
        WebCallStatsHolder.getValue(retMap);
        Assert.assertEquals(4, retMap.get("web_calls_count").longValue());
        Assert.assertEquals(500, retMap.get("web_calls_max").longValue());
        Assert.assertEquals(10, retMap.get("web_calls_min").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_8t16").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_16t32").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_32t64").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_256t512").longValue());
        Assert.assertEquals(3, retMap.get("method_test_count").longValue());
        Assert.assertEquals(500, retMap.get("method_test_max").longValue());
        Assert.assertEquals(10, retMap.get("method_test_min").longValue());
        Assert.assertEquals(1, retMap.get("method_aaa_count").longValue());
        Assert.assertEquals(50, retMap.get("method_aaa_max").longValue());
        Assert.assertEquals(50, retMap.get("method_aaa_min").longValue());
        // get content by StringBuilder
        StringBuilder strBuff = new StringBuilder(TBaseConstants.BUILDER_DEFAULT_SIZE);
        WebCallStatsHolder.getValue(strBuff);
        // System.out.println(strBuff.toString());
        strBuff.delete(0, strBuff.length());
        // test snapshot
        WebCallStatsHolder.snapShort(retMap);
        retMap.clear();
        // call method test, 1, 300, 300
        WebCallStatsHolder.addMethodCall("test", 300);
        // call method mmm, 1, 40, 40
        WebCallStatsHolder.addMethodCall("mmm", 40);
        // call method test, 1, 1000, 1000
        WebCallStatsHolder.addMethodCall("test", 1000);
        WebCallStatsHolder.getValue(retMap);
        Assert.assertEquals(3, retMap.get("web_calls_count").longValue());
        Assert.assertEquals(1000, retMap.get("web_calls_max").longValue());
        Assert.assertEquals(40, retMap.get("web_calls_min").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_32t64").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_512t1024").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_256t512").longValue());
        Assert.assertEquals(1, retMap.get("web_calls_cell_256t512").longValue());
        Assert.assertEquals(2, retMap.get("method_test_count").longValue());
        Assert.assertEquals(1000, retMap.get("method_test_max").longValue());
        Assert.assertEquals(300, retMap.get("method_test_min").longValue());
        Assert.assertEquals(1, retMap.get("method_mmm_count").longValue());
        Assert.assertEquals(40, retMap.get("method_mmm_max").longValue());
        Assert.assertEquals(40, retMap.get("method_mmm_min").longValue());
        // test no data
        WebCallStatsHolder.snapShort(retMap);
        // get content by StringBuilder
        WebCallStatsHolder.getValue(strBuff);
        // System.out.println(strBuff.toString());
    }
}
