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

package org.apache.inlong.tubemq.server.master.web.action.screen.cluster;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.web.common.ClusterQueryResult;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;
import org.apache.inlong.tubemq.server.master.web.simplemvc.Action;
import org.apache.inlong.tubemq.server.master.web.simplemvc.RequestContext;

public class ClusterManager implements Action {

    private final TMaster master;

    public ClusterManager(TMaster master) {
        this.master = master;
    }

    @Override
    public void execute(RequestContext context) {
        HttpServletRequest req = context.getReq();
        MetaDataService defMetaDataService = this.master.getMetaDataService();
        List<ClusterGroupVO> clusterGroupVOList = new ArrayList<>();
        ClusterGroupVO clusterGroupVO = defMetaDataService.getGroupAddressStrInfo();
        if (clusterGroupVO != null) {
            clusterGroupVOList.add(clusterGroupVO);
        }
        ClusterQueryResult clusterQueryResult = new ClusterQueryResult();
        clusterQueryResult.setResultList(clusterGroupVOList);
        clusterQueryResult.setTotalCount(clusterGroupVOList.size());
        context.put("queryResult", clusterQueryResult);
        context.put("page", "clusterManage");
    }

    public class BrokerComparator implements Comparator<BrokerInfo> {
        @Override
        public int compare(BrokerInfo o1, BrokerInfo o2) {
            return o1.getBrokerId() - o2.getBrokerId();
        }
    }
}
