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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper;

import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;

public interface BrokerConfigMapper extends AbstractMapper {

    /**
     * Add a new broker configure info into store
     *
     * @param entity   need add record
     * @param strBuff  the string buffer
     * @param result   process result with old value
     * @return  the process result
     */
    boolean addBrokerConf(BrokerConfEntity entity,
                          StringBuilder strBuff, ProcessResult result);

    /**
     * Update a broker configure info into store
     *
     * @param entity   need update record
     * @param strBuff  the string buffer
     * @param result   process result with old value
     * @return  the process result
     */
    boolean updBrokerConf(BrokerConfEntity entity,
                          StringBuilder strBuff, ProcessResult result);

    /**
     * Update a broker manage status
     *
     * @param opEntity      the operator information
     * @param brokerId      the broker id need to updated
     * @param newMngStatus  the new manage status
     * @param strBuff       the string buffer
     * @param result        process result with old value
     * @return  the process result
     */
    boolean updBrokerMngStatus(BaseEntity opEntity,
                               int brokerId, ManageStatus newMngStatus,
                               StringBuilder strBuff, ProcessResult result);

    /**
     * delete broker configure info from store
     *
     * @param brokerId  the broker id to be deleted
     * @param strBuff   the string buffer
     * @param result    the process result
     * @return          whether success
     */
    boolean delBrokerConf(int brokerId, StringBuilder strBuff, ProcessResult result);

    /**
     * get broker configure info from store
     *
     * @param qryEntity    the query conditions
     * @return result, only read
     */
    Map<Integer, BrokerConfEntity> getBrokerConfInfo(BrokerConfEntity qryEntity);

    /**
     * get broker configure info from store
     *
     * @param brokerIdSet  need matched broker id set
     * @param brokerIpSet  need matched broker ip set
     * @param qryEntity    need matched properties
     * @return result, only read
     */
    Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
                                                     Set<String> brokerIpSet,
                                                     BrokerConfEntity qryEntity);

    /**
     * get broker configure info from store
     *
     * @param brokerId  the broker id to be queried
     * @return result, only read
     */
    BrokerConfEntity getBrokerConfByBrokerId(int brokerId);

    /**
     * get broker configure info from store
     *
     * @param brokerIp   the broker ip to be queried
     * @return result, only read
     */
    BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp);

    /**
     * get broker id set by region id set
     *
     * @param regionIdSet   the region id set
     * @return  the regionId and brokerId set map
     */
    Map<Integer, Set<Integer>> getBrokerIdByRegionId(Set<Integer> regionIdSet);
}
