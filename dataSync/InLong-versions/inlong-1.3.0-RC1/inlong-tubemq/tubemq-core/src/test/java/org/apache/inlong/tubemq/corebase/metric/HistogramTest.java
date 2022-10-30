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

package org.apache.inlong.tubemq.corebase.metric;

import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.SimpleHistogram;
import org.junit.Assert;
import org.junit.Test;

public class HistogramTest {

    @Test
    public void testSimpleHistogram() {
        SimpleHistogram histogram =
                new SimpleHistogram("stats", "api");
        // test getValue by StringBuilder
        histogram.update(10L);
        histogram.update(100000L);
        histogram.update(3000L);
        histogram.update(-5L);
        StringBuilder strBuff = new StringBuilder(512);
        histogram.getValue(strBuff, false);
        String result1 = "\"api_stats\":{\"count\":4,\"min\":-5,\"max\":100000}";
        Assert.assertEquals(result1, strBuff.toString());
        strBuff.delete(0, strBuff.length());
        // test getValue by Map
        histogram.update(3L);
        histogram.update(10000L);
        histogram.update(900000L);
        histogram.update(10L);
        Map<String, Long> tmpMap = new LinkedHashMap();
        histogram.getValue(tmpMap, false);
        Assert.assertEquals(tmpMap.get("api_stats_count").longValue(), 8L);
        Assert.assertEquals(tmpMap.get("api_stats_max").longValue(), 900000L);
        Assert.assertEquals(tmpMap.get("api_stats_min").longValue(), -5L);
        tmpMap.clear();
        // test snapShort
        histogram.update(2L);
        histogram.update(10000000L);
        histogram.update(500L);
        histogram.update(1L);
        histogram.snapShort(tmpMap, false);
        Assert.assertEquals(tmpMap.get("api_stats_count").longValue(), 12L);
        Assert.assertEquals(tmpMap.get("api_stats_max").longValue(), 10000000L);
        Assert.assertEquals(tmpMap.get("api_stats_min").longValue(), -5L);
        tmpMap.clear();
        // test getValue by string buffer
        histogram.update(3L);
        histogram.update(10000L);
        histogram.update(900000L);
        histogram.update(10L);
        histogram.snapShort(strBuff, false);
        String result2 = "\"api_stats\":{\"count\":4,\"min\":3,\"max\":900000}";
        Assert.assertEquals(result2, strBuff.toString());
        strBuff.delete(0, strBuff.length());
        histogram.update(1L);
        histogram.getValue(strBuff, false);
        String result3 = "\"api_stats\":{\"count\":1,\"min\":1,\"max\":1}";
        Assert.assertEquals(result3, strBuff.toString());
        strBuff.delete(0, strBuff.length());
    }

    @Test
    public void testESTHistogram() {
        ESTHistogram estHistogram =
                new ESTHistogram("dlt", "disk");
        estHistogram.update(30L);
        estHistogram.update(1000L);
        estHistogram.update(-5L);
        estHistogram.update(131070L);
        estHistogram.update(131071L);
        estHistogram.update(131072L);
        estHistogram.update(131100L);
        // test get value by strBuff
        StringBuilder strBuff = new StringBuilder(512);
        estHistogram.getValue(strBuff, false);
        String result1 = "\"disk_dlt\":{\"count\":7,\"min\":-5,\"max\":131100,"
                + "\"cells\":{\"cell_0t2\":1,\"cell_16t32\":1,\"cell_512t1024\":1"
                + ",\"cell_65536t131072\":2,\"cell_131072tMax\":2}}";
        Assert.assertEquals(result1, strBuff.toString());
        strBuff.delete(0, strBuff.length());
        // test for map
        Map<String, Long> tmpMap = new LinkedHashMap();
        estHistogram.getValue(tmpMap, false);
        Assert.assertEquals(tmpMap.get("disk_dlt_count").longValue(), 7L);
        Assert.assertEquals(tmpMap.get("disk_dlt_max").longValue(), 131100L);
        Assert.assertEquals(tmpMap.get("disk_dlt_min").longValue(), -5L);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_0t2").longValue(), 1);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_16t32").longValue(), 1);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_512t1024").longValue(), 1);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_65536t131072").longValue(), 2);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_131072tMax").longValue(), 2);
        tmpMap.clear();
        // test snapShort
        estHistogram.snapShort(tmpMap, false);
        tmpMap.clear();
        estHistogram.update(1L);
        estHistogram.update(100L);
        estHistogram.getValue(tmpMap, false);
        Assert.assertEquals(tmpMap.get("disk_dlt_count").longValue(), 2L);
        Assert.assertEquals(tmpMap.get("disk_dlt_max").longValue(), 100L);
        Assert.assertEquals(tmpMap.get("disk_dlt_min").longValue(), 1L);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_0t2").longValue(), 1);
        Assert.assertEquals(tmpMap.get("disk_dlt_cell_64t128").longValue(), 1);
        tmpMap.clear();
        // test get value by strBuff
        estHistogram.getValue(strBuff, false);
        String result2 =
                "\"disk_dlt\":{\"count\":2,\"min\":1,\"max\":100,\"cells\":{\"cell_0t2\":1,\"cell_64t128\":1}}";
        Assert.assertEquals(result2, strBuff.toString());
        strBuff.delete(0, strBuff.length());
        // test clear()
        estHistogram.clear();
        estHistogram.getValue(tmpMap, false);
        Assert.assertEquals(tmpMap.get("disk_dlt_count").longValue(), 0L);
        Assert.assertEquals(tmpMap.get("disk_dlt_max").longValue(), Long.MIN_VALUE);
        Assert.assertEquals(tmpMap.get("disk_dlt_min").longValue(), Long.MAX_VALUE);
    }
}
