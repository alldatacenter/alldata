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

package org.apache.inlong.tubemq.server.master.web.action.screen.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TubeServerVersion;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.web.common.BrokerQueryResult;
import org.apache.inlong.tubemq.server.master.web.model.BrokerVO;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;
import org.apache.inlong.tubemq.server.master.web.simplemvc.RequestContext;

public class BrokerList implements Action {

    private final TMaster master;

    public BrokerList(TMaster master) {
        this.master = master;
    }

    @Override
    public void execute(RequestContext context) {
        HttpServletRequest req = context.getReq();
        String strPageNum = req.getParameter("page_num");
        String strPageSize = req.getParameter("page_size");
        //String strTopicName = req.getParameter("topicName");
        //String strConsumeGroup = req.getParameter("consumeGroup");
        int pageNum = TStringUtils.isNotEmpty(strPageNum)
                ? Integer.parseInt(strPageNum) : 1;
        pageNum = pageNum <= 0 ? 1 : pageNum;
        int pageSize = TStringUtils.isNotEmpty(strPageSize)
                ? Integer.parseInt(strPageSize) : 10;
        pageSize = Math.max(pageSize, 10);
        BrokerRunManager brokerRunManager = master.getBrokerRunManager();
        List<BrokerInfo> brokerInfoList =
                new ArrayList(brokerRunManager.getBrokerInfoMap(null).values());
        // *************************************************************************************
        for (int i = 0; i < 95; i++) {
            BrokerInfo info = new BrokerInfo(i, "127.0.0.1", 8123);
            brokerInfoList.add(info);
        }
        // *************************************************************************************

        int totalPage =
                brokerInfoList.size() % pageSize == 0 ? brokerInfoList.size() / pageSize : brokerInfoList
                        .size() / pageSize + 1;
        if (pageNum > totalPage) {
            pageNum = totalPage;
        }
        if (pageNum < 1) {
            pageNum = 1;
        }

        List<BrokerVO> brokerVOList = null;
        if (!brokerInfoList.isEmpty()) {
            Collections.sort(brokerInfoList, new BrokerComparator());
            int fromIndex = pageSize * (pageNum - 1);
            int toIndex =
                    Math.min(fromIndex + pageSize, brokerInfoList.size());
            List<BrokerInfo> firstPageList = brokerInfoList.subList(fromIndex, toIndex);
            brokerVOList = new ArrayList<>(brokerInfoList.size());
            for (BrokerInfo brokerInfo : firstPageList) {
                BrokerVO brokerVO = new BrokerVO();
                brokerVO.setId(brokerInfo.getBrokerId());
                brokerVO.setIp(brokerInfo.getHost());
                List<TopicInfo> topicInfoList =
                        brokerRunManager.getPubBrokerPushedTopicInfo(brokerInfo.getBrokerId());
                brokerVO.setTopicCount(topicInfoList.size());
                int totalPartitionNum = 0;
                for (TopicInfo topicInfo : topicInfoList) {
                    totalPartitionNum += topicInfo.getPartitionNum();
                }
                brokerVO.setPartitionCount(totalPartitionNum);
                brokerVO.setReadable(true);
                brokerVO.setWritable(true);
                brokerVO.setVersion(TubeServerVersion.SERVER_VERSION);
                brokerVO.setStatus(1);
                brokerVO.setLastOpTime(new Date());
                brokerVOList.add(brokerVO);
            }
        }
        BrokerQueryResult brokerQueryResult = new BrokerQueryResult();
        brokerQueryResult.setResultList(brokerVOList);
        brokerQueryResult.setCurrentPage(pageNum);
        brokerQueryResult.setTotalPage(totalPage);
        brokerQueryResult.setTotalCount(brokerInfoList.size());
        brokerQueryResult.setPageSize(pageSize);
        context.put("queryResult", brokerQueryResult);
        context.put("page", "brokerList");
    }

    public class BrokerComparator implements Comparator<BrokerInfo> {
        @Override
        public int compare(BrokerInfo o1, BrokerInfo o2) {
            return o1.getBrokerId() - o2.getBrokerId();
        }
    }
}
