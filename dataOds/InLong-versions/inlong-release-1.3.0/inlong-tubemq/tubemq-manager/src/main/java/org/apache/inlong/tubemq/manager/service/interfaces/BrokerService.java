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

package org.apache.inlong.tubemq.manager.service.interfaces;

import java.util.List;

import org.apache.inlong.tubemq.manager.controller.TubeMQResult;
import org.apache.inlong.tubemq.manager.controller.group.request.DeleteOffsetReq;
import org.apache.inlong.tubemq.manager.controller.group.request.QueryOffsetReq;
import org.apache.inlong.tubemq.manager.controller.group.result.OffsetQueryRes;
import org.apache.inlong.tubemq.manager.controller.node.request.CloneOffsetReq;

public interface BrokerService {

    /**
     * reset the brokers in a region to be default region
     *
     * @param regionId
     * @param clusterId
     */
    void resetBrokerRegions(long regionId, long clusterId);

    /**
     * update brokers to be in a region
     *
     * @param brokerIdList
     * @param regionId
     * @param clusterId
     */
    void updateBrokersRegion(List<Long> brokerIdList, Long regionId, Long clusterId);

    /**
     * check if all the brokers exsit in this cluster
     *
     * @param brokerIdList
     * @param clusterId
     * @return
     */
    boolean checkIfBrokersAllExsit(List<Long> brokerIdList, long clusterId);

    /**
     * get all broker id list in a region
     *
     * @param regionId
     * @param cluster
     * @return
     */
    List<Long> getBrokerIdListInRegion(long regionId, long cluster);

    /**
     * clone offset from one group to another
     *
     * @param brokerIp
     * @param brokerWebPort
     * @param req
     * @return
     */
    TubeMQResult cloneOffset(String brokerIp, int brokerWebPort, CloneOffsetReq req);

    /**
     * delete offset
     *
     * @param brokerIp
     * @param brokerWebPort
     * @param req
     * @return
     */
    TubeMQResult deleteOffset(String brokerIp, int brokerWebPort,
                              DeleteOffsetReq req);

    OffsetQueryRes queryOffset(String brokerIp, int brokerWebPort, QueryOffsetReq req);
}
