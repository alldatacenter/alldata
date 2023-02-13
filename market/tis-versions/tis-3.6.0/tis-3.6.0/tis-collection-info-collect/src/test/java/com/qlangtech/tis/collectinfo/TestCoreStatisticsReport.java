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

package com.qlangtech.tis.collectinfo;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.HttpUtils;
import junit.framework.TestCase;
import org.apache.solr.common.cloud.Replica;
import org.apache.solr.common.cloud.Slice;
import org.easymock.EasyMock;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: baisui 百岁
 * @create: 2020-10-04 15:18
 **/
public class TestCoreStatisticsReport extends TestCase {

    public void testAddClusterCoreInfo() {
        // int order, String testStr, String classpath, Class<?> clazz
        final AtomicInteger httpGetCount = new AtomicInteger();
        HttpUtils.addMockApply(
                CoreStatisticsReport.GET_METRIX_PATH
                , (HttpUtils.IClasspathRes) (url) -> {
                    return TestCoreStatisticsReport.class.getResourceAsStream(
                            "search4totalpay_query_update_metrix_time" + httpGetCount.incrementAndGet() + ".xml");
                }
        );

        //String collectionName = "search4totalpay";
        //Slice slice, SolrZkClient zookeeper

        Slice slice = EasyMock.createMock("slice", Slice.class);
        EasyMock.expect(slice.getName()).andReturn("shard1").times(2);
        Replica replica = EasyMock.createMock("replica", Replica.class);

        EasyMock.expect(replica.getStr(Slice.LEADER)).andReturn("true").times(2);
        EasyMock.expect(replica.getCoreUrl()).andReturn("http://192.168.28.200:8080/solr/search4totalpay_shard1_replica_n1/").times(2);
        EasyMock.expect(replica.getNodeName()).andReturn("http://192.168.28.200:8080/solr").times(2);
        Collection<Replica> replicas = Collections.singleton(replica);
        EasyMock.expect(slice.getReplicas()).andReturn(replicas).times(2);
        //SolrZkClient zookeeper = EasyMock.createMock("solrZkClient", SolrZkClient.class);


        EasyMock.replay(slice, replica);
        CoreStatisticsReport statisticsReportTime1 = new CoreStatisticsReport(Config.S4TOTALPAY);

        assertTrue(statisticsReportTime1.addClusterCoreInfo(slice));

        assertEquals(1683, statisticsReportTime1.requestCount.getCount());
        assertEquals(9527, statisticsReportTime1.updateCount.getCount());
        assertEquals(23360, statisticsReportTime1.numDocs.get());
        assertEquals(9999, statisticsReportTime1.updateErrorCount.getCount());
        assertEquals(22222, statisticsReportTime1.requestErrorCount.getCount());

        CoreStatisticsReport statisticsReportTime2 = new CoreStatisticsReport(Config.S4TOTALPAY);

        statisticsReportTime2.addClusterCoreInfo(slice);
        assertEquals(2, httpGetCount.get());
        long requestIncreasement = statisticsReportTime1.getRequestIncreasement(statisticsReportTime2);

        assertEquals(10, requestIncreasement);

        EasyMock.verify(slice, replica);
    }

}
