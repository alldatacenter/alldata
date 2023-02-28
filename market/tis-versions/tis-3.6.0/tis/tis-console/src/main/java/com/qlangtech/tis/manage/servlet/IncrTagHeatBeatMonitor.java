///**
// * Copyright (c) 2020 QingLang, Inc. <baisui@qlangtech.com>
// * <p>
// * This program is free software: you can use, redistribute, and/or modify
// * it under the terms of the GNU Affero General Public License, version 3
// * or later ("AGPL"), as published by the Free Software Foundation.
// * <p>
// * This program is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
// * FITNESS FOR A PARTICULAR PURPOSE.
// * <p>
// * You should have received a copy of the GNU Affero General Public License
// * along with this program. If not, see <http://www.gnu.org/licenses/>.
// */
//package com.qlangtech.tis.manage.servlet;
//
//import com.alibaba.fastjson.JSONObject;
//import com.qlangtech.tis.async.message.client.consumer.IMQConsumerStatusFactory;
//import com.qlangtech.tis.cloud.ITISCoordinator;
//import com.qlangtech.tis.coredefine.module.action.CoreAction;
//import com.qlangtech.tis.manage.spring.ZooKeeperGetter;
//import com.qlangtech.tis.realtime.yarn.rpc.JobType;
//import com.qlangtech.tis.trigger.jst.ILogListener;
//import com.qlangtech.tis.trigger.socket.ExecuteState;
//import com.qlangtech.tis.trigger.socket.LogType;
//import org.apache.commons.lang.StringUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Collections;
//import java.util.Map;
//
///**
// * @author 百岁（baisui@qlangtech.com）
// * @create: 2020-08-31 10:04
// */
//public class IncrTagHeatBeatMonitor {
//
//  private static final Logger logger = LoggerFactory.getLogger(IncrTagHeatBeatMonitor.class);
//
//  private final ILogListener messagePush;
//
//  private final Map<String, TopicTagStatus> transferTagStatus;
//
//  private final Map<String, TopicTagStatus> binlogTopicTagStatus;
//
//  private final TopicTagIncrStatus topicTagIncrStatus;
//
//  private final IMQConsumerStatusFactory.IMQConsumerStatus consumerStatus;
//
//  private final String collectionName;
//  private final ZooKeeperGetter zkGetter;
//
//  public IncrTagHeatBeatMonitor(String collectionName, ILogListener messagePush, Map<String, TopicTagStatus> transferTagStatus
//    , Map<String, TopicTagStatus> binlogTopicTagStatus, TopicTagIncrStatus topicTagIncrStatus //
//    , IMQConsumerStatusFactory.IMQConsumerStatus consumerStatus, ZooKeeperGetter zkGetter) {
//    this.collectionName = collectionName;
//    this.messagePush = messagePush;
//    this.transferTagStatus = transferTagStatus;
//    this.binlogTopicTagStatus = binlogTopicTagStatus;
//    this.topicTagIncrStatus = topicTagIncrStatus;
//    this.consumerStatus = consumerStatus;
//    this.zkGetter = zkGetter;
//  }
//
//  public void build() {
////    TopicTagStatus tagStat = null;
////    TopicTagIncrStatus.TisIncrStatus averageTopicTagIncr;
////    try {
////      while (!messagePush.isClosed()) {
////        // long start = System.currentTimeMillis();
////        long currSec = (System.currentTimeMillis() / 1000);
////
////        getIncrTransferTagUpdateMap(this.zkGetter.getInstance(), transferTagStatus, collectionName);
////        for (String tabTag : topicTagIncrStatus.getFocusTags()) {
////          topicTagIncrStatus.add(currSec, TopicTagIncrStatus.TopicTagIncr.create(tabTag, Collections.emptyMap(), transferTagStatus));
////        }
////        for (String summaryKey : TopicTagIncrStatus.ALL_SUMMARY_KEYS) {
////          topicTagIncrStatus.add(currSec, TopicTagIncrStatus.TopicTagIncr.create(summaryKey, Collections.emptyMap(), transferTagStatus));
////        }
////        // logger.info("p4{}", System.currentTimeMillis() - start);
////        // start = System.currentTimeMillis();
////        averageTopicTagIncr = topicTagIncrStatus.getAverageTopicTagIncr(true, /** average */false);
////        // logger.info("p5{}", System.currentTimeMillis() - start);
////        // start = System.currentTimeMillis();
////        ExecuteState<TopicTagIncrStatus.TisIncrStatus> event = ExecuteState.create(LogType.MQ_TAGS_STATUS, averageTopicTagIncr);
////        messagePush.sendMsg2Client(event);
////        // start = System.currentTimeMillis();
////        try {
////          Thread.sleep(1000l);
////        } catch (InterruptedException e) {
////        }
////      }
////    } catch (Exception e) {
////      logger.error(this.collectionName, e);
////      throw new RuntimeException(e);
////    } finally {
////      // consumerStatus.close();
////    }
//  }
//
//  /**
//   * @param collection
//   * @throws Exception
//   */
//  private void getIncrTransferTagUpdateMap(
//    ITISCoordinator coordinator,
//    final Map<String, /* this.tag */    TopicTagStatus> transferTagStatus, String collection) throws Exception {
//    // curl -d"collection=search4totalpay&action=collection_topic_tags_status" http://localhost:8080/incr-control?collection=search4totalpay
////    JobType.RemoteCallResult<Void> tagCountMap
////      = JobType.Collection_TopicTags_status.assembIncrControl(
////      CoreAction.getAssembleNodeAddress(coordinator),
////      collection, Collections.emptyList(), new JobType.IAssembIncrControlResult() {
////        @Override
////        public LogFeedbackServlet.TagCountMap deserialize(JSONObject json) {
////          // LogFeedbackServlet.TagCountMap result = new LogFeedbackServlet.TagCountMap();
////          for (String key : json.keySet()) {
////            setMetricCount(transferTagStatus, key, json.getIntValue(key));
////          }
////          return null;
////        }
////      });
//  }
//
//  private void setMetricCount(Map<String, TopicTagStatus> tagStatus, String tagName, Integer count) {
//    TopicTagStatus tagStat;
//    tagStat = tagStatus.get(tagName);
//    if (tagStat == null) {
//      // String topic, String tag, int count, long lastUpdates
//      tagStat = new TopicTagStatus(StringUtils.EMPTY, tagName, count, -1);
//      tagStatus.put(tagName, tagStat);
//    } else {
//      tagStat.setCount(count);
//    }
//  }
//}
