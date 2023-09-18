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
package com.qlangtech.tis.manage.servlet;

import com.google.common.collect.Lists;
import com.qlangtech.tis.async.message.client.consumer.IMQConsumerStatusFactory;
import com.qlangtech.tis.cloud.ITISCoordinator;
import com.qlangtech.tis.cloud.MockZKUtils;
import com.qlangtech.tis.manage.common.ConfigFileContext;
import com.qlangtech.tis.manage.common.HttpUtils;
import com.qlangtech.tis.manage.common.MockHttpURLConnection;
import com.qlangtech.tis.manage.spring.ZooKeeperGetter;
import com.qlangtech.tis.realtime.transfer.IIncreaseCounter;
import com.qlangtech.tis.trigger.jst.ILogListener;
import com.qlangtech.tis.trigger.socket.ExecuteState;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-08-31 10:28
 */
public class TestIncrTagHeatBeatMonitor extends TestCase {

  private static final String collectionName = "search4totalpay";

  private static final String tag_talpayinfo = "totalpayinfo";

  private static final String tag_servicebillinfo = "servicebillinfo";

  // public void test(){
  // System.out.println(  String.format("hell%d",1) );
  // }
  public void testBuild() throws Exception {
    String resName = "collection_TopicTags_status_%d.json";
    AtomicInteger resFetchCount = new AtomicInteger();
    HttpUtils.mockConnMaker = new HttpUtils.DefaultMockConnectionMaker() {
      @Override
      protected MockHttpURLConnection createConnection(URL url, List<ConfigFileContext.Header> heads, ConfigFileContext.HTTPMethod method
        , byte[] content, HttpUtils.IClasspathRes cpRes) {
        String res = String.format(resName, resFetchCount.incrementAndGet());
        try {
          //  return new MockHttpURLConnection(cpRes.getResourceAsStream(res));

          return new MockHttpURLConnection(TestIncrTagHeatBeatMonitor.class.getResourceAsStream(res));

        } catch (Throwable e) {
          throw new RuntimeException(res, e);
        }
      }

//          @Override
//            protected MockHttpURLConnection createConnection( List<ConfigFileContext.Header> heads
//              , ConfigFileContext.HTTPMethod method, byte[] content, HttpUtils.ClasspathRes cpRes) {
//                // return super.createConnection(appendOrder, heads, method, content, cpRes);
//                String res = String.format(resName, resFetchCount.incrementAndGet());
//                try {
//                    return new MockHttpURLConnection(cpRes.getResourceAsStream(res));
//                } catch (Throwable e) {
//                    throw new RuntimeException(res, e);
//                }
//            }
    };
    HttpUtils.addMockApply(0, "incr-control", StringUtils.EMPTY, TestIncrTagHeatBeatMonitor.class);

    ZooKeeperGetter zookeeper = EasyMock.createMock("zooKeeperGetter", ZooKeeperGetter.class);
    ITISCoordinator coordinator = MockZKUtils.createZkMock();// EasyMock.createMock("coordinator", ITISCoordinator.class);
    int times = 5;
    // TestCollectionAction.createAssembleLogCollectPathMock(coordinator,times);

//    final String childNodeName = "000001";

//    EasyMock.expect(coordinator.getChildren("/tis/incr-transfer-group/incr-state-collect", null, true))
//      .andReturn(Collections.singletonList(childNodeName)).times(times);
//    EasyMock.expect(coordinator.getData("/tis/incr-transfer-group/incr-state-collect/" + childNodeName, null, new Stat(), true))
//      .andReturn("127.0.0.1".getBytes(TisUTF8.get())).times(times);
    EasyMock.expect(zookeeper.getInstance()).andReturn(coordinator).times(times);
    // 增量节点处理
    final Map<String, TopicTagStatus> /* tag */
      transferTagStatus = new HashMap<>();
    final Map<String, TopicTagStatus> /* tag */
      binlogTopicTagStatus = new HashMap<>();
    Collection<TopicTagIncrStatus.FocusTags> focusTags = Lists.newArrayList();
    // String topic, Collection<String> tags
    ArrayList<String> tags = Lists.newArrayList(tag_servicebillinfo, tag_talpayinfo
      , "orderdetail", "specialfee", "ent_expense", "payinfo", "order_bill", "instancedetail", "ent_expense_order");
    focusTags.add(new TopicTagIncrStatus.FocusTags("test-topic", tags));
    TopicTagIncrStatus topicTagIncrStatus = new TopicTagIncrStatus(focusTags);
    MockWebSocketMessagePush wsMessagePush = new MockWebSocketMessagePush();
    MockMQConsumerStatus mqConsumerStatus = new MockMQConsumerStatus();
//    IncrTagHeatBeatMonitor incrTagHeatBeatMonitor = new IncrTagHeatBeatMonitor(
//      this.collectionName, wsMessagePush, transferTagStatus, binlogTopicTagStatus, topicTagIncrStatus, mqConsumerStatus, zookeeper);

    EasyMock.replay(zookeeper, coordinator);

    //incrTagHeatBeatMonitor.build();
    assertEquals(6, wsMessagePush.count);

    EasyMock.verify(zookeeper, coordinator);
  }

  private static final class MockWebSocketMessagePush implements ILogListener {

    private int count;

    @Override
    public void read(Object event) {
    }

    @Override
    public boolean isClosed() {
      return this.count++ >= 5;
    }

    @Override
    public void sendMsg2Client(Object biz) throws IOException {
      ExecuteState<TopicTagIncrStatus.TisIncrStatus> event = (ExecuteState<TopicTagIncrStatus.TisIncrStatus>) biz;
      TopicTagIncrStatus.TisIncrStatus msg = event.getMsg();
      // System.out.println("SOLR_CONSUME_COUNT:" + msg.getSummary().get(IIncreaseCounter.SOLR_CONSUME_COUNT));
      if (count > 1) {
        assertEquals(100, (int) msg.getSummary().get(IIncreaseCounter.SOLR_CONSUME_COUNT));
        assertEquals(100, (int) msg.getSummary().get(IIncreaseCounter.TABLE_CONSUME_COUNT));
        Optional<TopicTagIncrStatus.TopicTagIncr> talpayinfo = msg.getTags().stream().filter((t) -> tag_talpayinfo.equals(t.getTag())).findFirst();
        assertTrue(talpayinfo.isPresent());
        assertEquals(10, talpayinfo.get().getTrantransferIncr());
        Optional<TopicTagIncrStatus.TopicTagIncr> servicebillinfo = msg.getTags().stream().filter((t) -> tag_servicebillinfo.equals(t.getTag())).findFirst();
        assertTrue(servicebillinfo.isPresent());
        assertEquals(20, servicebillinfo.get().getTrantransferIncr());
      }
      // System.out.println(JSON.toJSONString(biz, true));
    }
  }

  private static final class MockMQConsumerStatus implements IMQConsumerStatusFactory.IMQConsumerStatus {

    private long totalDiff;
    // @Override
    // public long getTotalDiff() {
    // return this.totalDiff += 1000;
    // }
    //
    // @Override
    // public void close() {
    //
    // }
  }
}
