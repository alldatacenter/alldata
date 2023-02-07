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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.ConfigObserver;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.KeepAliveService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;

public interface MetaConfigMapper extends KeepAliveService {

    /**
     * Register meta configure change observer
     *
     * @param eventObserver  the event observer
     */
    void regMetaConfigObserver(ConfigObserver eventObserver);

    boolean checkStoreStatus(boolean checkIsMaster, ProcessResult result);

    /**
     * Add or update cluster default setting
     * @param opEntity       operator information
     * @param brokerPort     broker port
     * @param brokerTlsPort  broker tls port
     * @param brokerWebPort  broker web port
     * @param maxMsgSizeMB   max cluster message size in MB
     * @param qryPriorityId  the default query priority id
     * @param flowCtrlEnable enable or disable flow control function
     * @param flowRuleCnt    the default flow rule count
     * @param flowCtrlInfo   the default flow control information
     * @param topicProps     default topic property information
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdClusterDefSetting(BaseEntity opEntity, int brokerPort,
            int brokerTlsPort, int brokerWebPort,
            int maxMsgSizeMB, int qryPriorityId,
            EnableStatus flowCtrlEnable, int flowRuleCnt,
            String flowCtrlInfo, TopicPropGroup topicProps,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get cluster configure information
     *
     * @param isMustConf  whether must be configured data.
     * @return the cluster configure
     */
    ClusterSettingEntity getClusterDefSetting(boolean isMustConf);

    // ////////////////////////////////////////////////////////////

    /**
     * Add or update broker configure information
     *
     * @param isAddOp   whether add operation
     * @param entity    need add or update configure information
     * @param strBuff   the print information string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdBrokerConfig(boolean isAddOp, BrokerConfEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Change broker configure status
     *
     * @param opEntity      operator
     * @param brokerId      need deleted broker id
     * @param newMngStatus  manage status
     * @param strBuff       the print information string buffer
     * @param result        the process result return
     * @return true if success otherwise false
     */
    boolean changeBrokerConfStatus(BaseEntity opEntity,
            int brokerId, ManageStatus newMngStatus,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete broker configure information
     *
     * @param operator  operator
     * @param brokerId  need deleted broker id
     * @param rsvData   reserve broker data
     * @param strBuff   the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean delBrokerConfInfo(String operator, int brokerId, boolean rsvData,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Query broker configure information
     *
     * @param qryEntity  the query condition, must not null
     * @return  the query result
     */
    Map<Integer, BrokerConfEntity> getBrokerConfInfo(BrokerConfEntity qryEntity);

    /**
     * Get broker configure information
     *
     * @param brokerIdSet  the broker id set need to query
     * @param brokerIpSet  the broker ip set need to query
     * @param qryEntity    the query condition
     * @return broker configure information
     */
    Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
            Set<String> brokerIpSet,
            BrokerConfEntity qryEntity);

    /**
     * Get broker configure information
     *
     * @param brokerId  need queried broker id
     * @return  the broker configure record
     */
    BrokerConfEntity getBrokerConfByBrokerId(int brokerId);

    /**
     * Get broker configure information
     *
     * @param brokerIp  need queried broker ip
     * @return  the broker configure record
     */
    BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp);

    // ////////////////////////////////////////////////////////////

    /**
     * Add or Update topic control configure info
     *
     * @param isAddOp  whether add operation
     * @param entity   the topic control info entity will be added
     * @param strBuff  the print info string buffer
     * @param result   the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdTopicCtrlConf(boolean isAddOp, TopicCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add topic control record, or update records if data exists
     *
     * @param opEntity operator information
     * @param topicName topic info
     * @param enableTopicAuth if authenticate check
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean insertTopicCtrlConf(BaseEntity opEntity,
            String topicName, EnableStatus enableTopicAuth,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add topic control record, or update records if data exists
     *
     * @param entity  operator information
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean insertTopicCtrlConf(TopicCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete topic control configure
     *
     * @param operator   operator
     * @param topicName  the topicName will be deleted
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean delTopicCtrlConf(String operator, String topicName,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get topic control record by topic name
     *
     * @param topicName  the topic name
     * @return    the topic control record
     */
    TopicCtrlEntity getTopicCtrlByTopicName(String topicName);

    /**
     * Get topic max message size in MB configure
     *
     * @param topicName   the topic name
     * @return   the max message size
     */
    int getTopicMaxMsgSizeInMB(String topicName);

    /**
     * Get topic control entity list
     *
     * @param qryEntity   the query condition
     * @return   the query result list
     */
    List<TopicCtrlEntity> queryTopicCtrlConf(TopicCtrlEntity qryEntity);

    /**
     * Get topic control entity list
     *
     * @param topicNameSet  the topic name set
     * @param qryEntity     the query condition
     * @return   the query result list
     */
    Map<String, TopicCtrlEntity> getTopicCtrlConf(Set<String> topicNameSet,
            TopicCtrlEntity qryEntity);

    /**
     * get topic max message size configure info from store
     *
     * @param defMaxMsgSizeInB  the default max message size in B
     * @param topicNameSet  need matched topic name set
     * @return result, only read
     */
    Map<String, Integer> getMaxMsgSizeInBByTopics(int defMaxMsgSizeInB,
            Set<String> topicNameSet);

    // ////////////////////////////////////////////////////////////

    /**
     * Add system topic deploy information
     *
     * @param brokerId        the broker id
     * @param brokerPort      the broker port
     * @param brokerIp        the broker ip
     * @param strBuff         the string buffer
     */
    void addSystemTopicDeploy(int brokerId, int brokerPort,
            String brokerIp, StringBuilder strBuff);

    /**
     * Add or update topic deploy configure info
     *
     * @param isAddOp  whether add operation
     * @param deployEntity   the topic deploy info entity
     * @param strBuff  the print info string buffer
     * @param result   the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdTopicDeployInfo(boolean isAddOp, TopicDeployEntity deployEntity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Update topic deploy status info
     *
     * @param opEntity       the operation information
     * @param brokerId       the broker id
     * @param topicName      the topic name
     * @param topicStatus    the topic deploy status
     * @param strBuff  the print info string buffer
     * @param result   the process result return
     * @return true if success otherwise false
     */
    boolean updTopicDeployStatusInfo(BaseEntity opEntity, int brokerId,
            String topicName, TopicStatus topicStatus,
            StringBuilder strBuff, ProcessResult result);

    /**
     * delete topic deploy configure info from store
     *
     * @param operator   the operator
     * @param brokerId   the broker id
     * @param topicName  the topic name need to delete
     * @param strBuff    the string buffer
     * @param result     the process result
     * @return           whether success
     */
    boolean delTopicDeployInfo(String operator, int brokerId, String topicName,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @param topicNameSet  query by topicNameSet
     * @param brokerIdSet   query by brokerIdSet
     * @param qryEntity     query conditions
     * @return topic deploy entity map
     */
    Map<String, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity);

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @param topicNameSet   the query topic set
     * @param brokerIdSet    the query broker id set
     * @return topic deploy entity map
     */
    Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet);

    /**
     * Get broker topic entity by topic name and broker id set
     *
     * @param topicNameSet   the query topic set
     * @param brokerIdSet    the query broker id set
     * @return topic entity map
     */
    Map<String, List<TopicDeployEntity>> getTopicConfInfoByTopicAndBrokerIds(
            Set<String> topicNameSet, Set<Integer> brokerIdSet);

    /**
     * Get configured topic information in the special broker node
     *
     * @param brokerId   the broker id need to query
     * @return topic entity map
     */
    Map<String, TopicDeployEntity> getConfiguredTopicInfo(int brokerId);

    /**
     * Get topic deploy information by the broker id and topic name
     *
     * @param brokerId   the broker id need to query
     * @param topicName  the topic name need to query
     * @return topic entity map
     */
    TopicDeployEntity getConfiguredTopicInfo(int brokerId, String topicName);

    /**
     * Get configured topic name in the special brokers
     *
     * @param brokerIdSet   the broker id set need to query
     * @return brokerid - topic name map
     */
    Map<Integer/* brokerId */, Set<String>> getConfiguredTopicInfo(Set<Integer> brokerIdSet);

    /**
     * Get deployed broker id and ip information for the special topic name set
     *
     * @param topicNameSet   the topic name set need to query
     * @return  the topic - (broker id, broker ip)  map
     */
    Map<String, Map<Integer, String>> getTopicBrokerInfo(Set<String> topicNameSet);

    /**
     * Get deployed broker id for the special topic name set
     *
     * @param topicNameSet   the topic name set need to query
     * @return  the broker id set
     */
    Set<Integer> getDeployedBrokerIdByTopic(Set<String> topicNameSet);

    /**
     * Get deployed topic set
     *
     * @return  the deployed topic set
     */
    Set<String> getDeployedTopicSet();

    // ////////////////////////////////////////////////////////////

    /**
     * Add group resource control configure info
     *
     * @param entity     the group resource control info entity will be add
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdGroupResCtrlConf(boolean isAddOp, GroupResCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add group control configure, or update records if data exists
     *
     * @param opEntity       operator information
     * @param groupName      the group name
     * @param qryPriorityId  the query priority id
     * @param flowCtrlEnable enable or disable flow control
     * @param flowRuleCnt    the flow control rule count
     * @param flowCtrlRuleInfo   the flow control information
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true if success otherwise false
     */
    boolean insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            int qryPriorityId, EnableStatus flowCtrlEnable,
            int flowRuleCnt, String flowCtrlRuleInfo,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add group control configure, or update records if data exists
     *
     * @param opEntity         the group resource control info entity will be add
     * @param groupName        operate target
     * @param resChkEnable     resource check status
     * @param allowedB2CRate   allowed B2C rate
     * @param strBuff          the print info string buffer
     * @param result           the process result return
     * @return true if success otherwise false
     */
    boolean insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            EnableStatus resChkEnable, int allowedB2CRate,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add group control configure, or update records if data exists
     *
     * @param entity  the group resource control info entity will be add
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    boolean insertGroupCtrlConf(GroupResCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete group control information
     *
     * @param operator       operator
     * @param groupName      need deleted group
     * @param strBuff        the print info string buffer
     * @param result         the process result return
     * @return    delete result
     */
    boolean delGroupCtrlConf(String operator, String groupName,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get group control information by group and query condition
     *
     * @param groupSet       need queried group set
     * @param qryEntity      query condition
     * @return    query result
     */
    Map<String, GroupResCtrlEntity> getGroupCtrlConf(Set<String> groupSet,
            GroupResCtrlEntity qryEntity);

    /**
     * Get group control information by group name
     *
     * @param groupName       need queried group name
     * @return    query result
     */
    GroupResCtrlEntity getGroupCtrlConf(String groupName);

    // //////////////////////////////////////////////////////////////////

    /**
     * add or update group's consume control information
     *
     * @param isAddOp   whether add operation
     * @param entity    need add or update group configure info
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    boolean addOrUpdConsumeCtrlInfo(boolean isAddOp, GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add consume control information, or update records if data exists
     *
     * @param opEntity   add or update base information, include creator, create time, etc.
     * @param groupName  add or update groupName information
     * @param topicName  add or update topicName information
     * @param enableCsm  add or update consume enable status information
     * @param disReason  add or update disable consume reason
     * @param enableFlt  add or update filter enable status information
     * @param fltCondStr add or update filter configure information
     * @param strBuff    the print info string buffer
     * @param result     the process result return
     * @return    process result
     */
    boolean insertConsumeCtrlInfo(BaseEntity opEntity, String groupName,
            String topicName, EnableStatus enableCsm,
            String disReason, EnableStatus enableFlt,
            String fltCondStr, StringBuilder strBuff,
            ProcessResult result);

    /**
     * Add consume control information, or update records if data exists
     *
     * @param entity     add or update group consume control info
     * @param strBuff    the print info string buffer
     * @param result     the process result return
     * @return    process result
     */
    boolean insertConsumeCtrlInfo(GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete consume control configure
     *
     * @param operator   the operator
     * @param groupName  the group name
     * @param topicName  the topic name
     * @param strBuff    the print info string buffer
     * @param result     the process result return
     * @return    process result
     */
    boolean delConsumeCtrlConf(String operator,
            String groupName, String topicName,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get all group consume control record for a specific topic
     *
     * @param topicName  the queried topic name
     * @return group consume control list
     */
    List<GroupConsumeCtrlEntity> getConsumeCtrlByTopic(String topicName);

    /**
     * Get all group consume control record for the specific topic set
     *
     * @param topicSet  the queried topic name set
     * @return group consume control list
     */
    Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByTopic(Set<String> topicSet);

    /**
     * Get consume control record
     *
     * @param groupName  the group name need to query
     * @param topicName  the topic name need to query
     * @return group consume control list
     */
    GroupConsumeCtrlEntity getConsumeCtrlByGroupAndTopic(String groupName, String topicName);

    /**
     * Get all disable consumed topic for a specific group
     *
     * @param groupName  the queried group name
     * @return  the disable consumed topic list
     */
    Set<String> getDisableTopicByGroupName(String groupName);

    /**
     * Get consume control records by group name
     *
     * @param groupName  the queried group name
     * @return  the consume control record list
     */
    List<GroupConsumeCtrlEntity> getConsumeCtrlByGroupName(String groupName);

    /**
     * Get consume control records by group name set
     *
     * @param groupSet  the queried group name set
     * @return  the consume control record list
     */
    Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByGroupName(Set<String> groupSet);

    /**
     * Get group consume control configure for topic & group set
     *
     * @param groupSet the topic name set
     * @param topicSet the group name set
     * @param qryEntry the query conditions
     * @return  the queried consume control record
     */
    Map<String, List<GroupConsumeCtrlEntity>> getGroupConsumeCtrlConf(
            Set<String> groupSet, Set<String> topicSet, GroupConsumeCtrlEntity qryEntry);

    /**
     * Judge whether the group in use
     *
     * @param groupName the group name
     * @return  whether in use
     */
    boolean isGroupInUse(String groupName);

    /**
     * Judge whether the topic in use
     *
     * @param topicName the topic name
     * @return  whether in use
     */
    boolean isTopicInUse(String topicName);
}
