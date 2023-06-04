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

package org.apache.inlong.tubemq.server.master.metamanage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.Server;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStsChgType;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.ConfigObserver;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.inlong.tubemq.server.master.web.handler.BrokerProcessResult;
import org.apache.inlong.tubemq.server.master.web.handler.GroupProcessResult;
import org.apache.inlong.tubemq.server.master.web.handler.TopicProcessResult;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;

public interface MetaDataService extends Server {

    /**
     * Register meta configure change observer
     *
     * @param eventObserver  the event observer
     */
    void regMetaConfigObserver(ConfigObserver eventObserver);

    /**
     * Whether the current node is the Master role
     *
     * @return true if is Master role or slave
     */
    boolean isSelfMaster();

    /**
     * Whether the Master node in active
     *
     * @return  true for Master, false for Slave
     */
    boolean isPrimaryNodeActive();

    /**
     * Transfer master role to other replica nodes
     *
     * @throws Exception
     */
    void transferMaster() throws Exception;

    /**
     * Get current Master address
     *
     * @return  the current Master address
     */
    String getMasterAddress();

    /**
     * Get group address info
     *
     * @return  the group address information
     */
    ClusterGroupVO getGroupAddressStrInfo();

    // //////////////////////////////////////////////////////////////////////

    /**
     * Check if consume target is authorization or not
     *
     * @param consumerId   checked consume id
     * @param groupName    checked group name
     * @param reqTopicSet   consumer request topic set
     * @param reqTopicCondMap   consumer request filter items
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true is authorized, false not
     */
    boolean isConsumeTargetAuthorized(String consumerId, String groupName,
            Set<String> reqTopicSet,
            Map<String, TreeSet<String>> reqTopicCondMap,
            StringBuilder strBuff, ProcessResult result);

    // //////////////////////////////////////////////////////////////////////

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

    // /////////////////////////////////////////////////////////////////////

    /**
     * Add or update broker configure information
     *
     * @param isAddOp        whether add operation
     * @param opEntity       operator information
     * @param brokerId       broker id
     * @param brokerIp       broker ip
     * @param brokerPort     broker port
     * @param brokerTlsPort  broker tls port
     * @param brokerWebPort  broker web port
     * @param regionId       region id
     * @param groupId        group id
     * @param mngStatus      manage status
     * @param topicProps     default topic property information
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return  the return result, include updated information
     */
    BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BaseEntity opEntity,
            int brokerId, String brokerIp, int brokerPort,
            int brokerTlsPort, int brokerWebPort,
            int regionId, int groupId, ManageStatus mngStatus,
            TopicPropGroup topicProps, StringBuilder strBuff,
            ProcessResult result);

    /**
     * Add or update broker configure information
     *
     * @param isAddOp   whether add operation
     * @param entity    need add or update configure information
     * @param strBuff   the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BrokerConfEntity entity,
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
    BrokerProcessResult changeBrokerConfStatus(BaseEntity opEntity,
            int brokerId, ManageStatus newMngStatus,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Sync broker configure to broker node
     *
     * @param opEntity   the operator information
     * @param brokerId   the broker id to reload
     * @param strBuff    the print information string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    BrokerProcessResult reloadBrokerConfInfo(BaseEntity opEntity, int brokerId,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete broker configure information
     *
     * @param operator  operator
     * @param rsvData   if reserve topic's data info
     * @param brokerId  need deleted broker id set
     * @param strBuff   the print information string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    BrokerProcessResult delBrokerConfInfo(String operator, boolean rsvData,
            int brokerId, StringBuilder strBuff,
            ProcessResult result);

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
     * Delete cleaned topic deploy configures
     *
     * @param brokerId      the broker id
     * @param removedTopics  the removed topics
     * @param strBuff        the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    boolean delCleanedTopicDeployInfo(int brokerId, List<String> removedTopics,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get broker's topicName set,
     * if brokerIds is empty, then return all broker's topicNames
     *
     * @param brokerIdSet    the broker id set need to query
     * @return broker's topicName set
     */
    Map<Integer, Set<String>> getBrokerTopicConfigInfo(Set<Integer> brokerIdSet);

    /**
     * Get deployed broker id and ip information for the special topic name set
     *
     * @param topicNameSet   the topic name set need to query
     * @return  the topic - (broker id, broker ip)  map
     */
    Map<String, Map<Integer, String>> getTopicBrokerConfigInfo(Set<String> topicNameSet);

    /**
     * Get deployed topic set
     *
     * @return  the deployed topic set
     */
    Set<String> getDeployedTopicSet();

    /**
     * Get broker configure entity by broker id
     *
     * @param brokerId   the broker id need to query
     * @return  the broker configure
     */
    BrokerConfEntity getBrokerConfByBrokerId(int brokerId);

    /**
     * Get broker configure entity by broker ip
     *
     * @param brokerIp   the broker ip need to query
     * @return  the broker configure
     */
    BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp);

    /**
     * Get the deployed topic information configured on the broker to be queried
     *
     * @param brokerId   the broker id need to query
     * @return  the broker configure
     */
    Map<String, TopicDeployEntity> getBrokerTopicConfEntitySet(int brokerId);

    // ///////////////////////////////////////////////////////////////////////////

    /**
     * Add or update topic deploy information
     *
     * @param isAddOp         whether add operation
     * @param opEntity        the operation information
     * @param brokerId        the broker id
     * @param topicName       the topic name
     * @param deployStatus    the deploy status
     * @param topicPropInfo   the topic property set
     * @param strBuff         the string buffer
     * @param result          the process result
     * @return                true if success otherwise false
     */
    TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp, BaseEntity opEntity,
            int brokerId, String topicName,
            TopicStatus deployStatus,
            TopicPropGroup topicPropInfo,
            StringBuilder strBuff,
            ProcessResult result);

    /**
     * Add or update topic deploy configure info
     *
     * @param isAddOp  whether add operation
     * @param deployEntity   the topic deploy info entity
     * @param strBuff  the print info string buffer
     * @param result   the process result return
     * @return true if success otherwise false
     */
    TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp, TopicDeployEntity deployEntity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Change topic deploy status info
     *
     * @param opEntity   the operation information
     * @param brokerId   the broker id
     * @param topicName  the topic name
     * @param chgType    the topic change type
     * @param strBuff    the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    TopicProcessResult updTopicDeployStatusInfo(BaseEntity opEntity, int brokerId,
            String topicName, TopicStsChgType chgType,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Get broker topic entity, if query entity is null, return all topic entity
     *
     * @param topicNameSet  query by topicNameSet
     * @param brokerIdSet   query by brokerIdSet
     * @param qryEntity     query conditions
     * @return topic deploy information map
     */
    Map<String, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity);

    /**
     * Get topic deploy information, if the result matched is null, then return empty
     *
     * @param topicNameSet  query by topicNameSet
     * @param brokerIdSet   query by brokerIdSet
     * @return topic deploy information map
     */
    Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet);

    /**
     * Get topic deploy information
     *
     * @param topicNameSet  query by topicNameSet
     * @param brokerIdSet   query by brokerIdSet
     * @return topic deploy information map
     */
    Map<String, List<TopicDeployEntity>> getTopicConfMapByTopicAndBrokerIds(Set<String> topicNameSet,
            Set<Integer> brokerIdSet);

    /**
     * Get deployed topic information
     *
     * @param brokerConfEntity  broker configure entity
     * @param strBuff           the string buffer
     * @return   the deployed topic information map
     */
    Map<String, String> getBrokerTopicStrConfigInfo(BrokerConfEntity brokerConfEntity,
            StringBuilder strBuff);

    /**
     * Get removed topic information
     *
     * @param brokerConfEntity  broker configure entity
     * @param strBuff           the string buffer
     * @return   the deployed topic information map
     */
    Map<String, String> getBrokerRemovedTopicStrConfigInfo(
            BrokerConfEntity brokerConfEntity, StringBuilder strBuff);

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Add or Update topic control configure info
     *
     * @param isAddOp        whether add operation
     * @param opEntity       operator information
     * @param topicNameSet   topic name set
     * @param topicNameId    the topic name id
     * @param enableTopicAuth  whether enable topic authentication
     * @param maxMsgSizeInMB   the max message size in MB
     * @param strBuff          the print info string buffer
     * @param result           the process result return
     * @return true if success otherwise false
     */
    List<TopicProcessResult> addOrUpdTopicCtrlConf(boolean isAddOp, BaseEntity opEntity,
            Set<String> topicNameSet, int topicNameId,
            EnableStatus enableTopicAuth, int maxMsgSizeInMB,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Add or Update topic control configure info
     *
     * @param isAddOp    whether add operation
     * @param entityMap  the topic control entity map which will be add
     * @param strBuff    the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    List<TopicProcessResult> addOrUpdTopicCtrlConf(boolean isAddOp,
            Map<String, TopicCtrlEntity> entityMap,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Insert topic control configure info
     *
     * @param opEntity operator information
     * @param topicName topic info
     * @param enableTopicAuth authenticate check status
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    TopicProcessResult insertTopicCtrlConf(BaseEntity opEntity, String topicName,
            EnableStatus enableTopicAuth, StringBuilder strBuff,
            ProcessResult result);

    /**
     * Insert topic control configure info
     *
     * @param entity  operator information
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    TopicProcessResult insertTopicCtrlConf(TopicCtrlEntity entity,
            StringBuilder strBuff,
            ProcessResult result);

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
     * Get topic max message size by topic name
     *
     * @param topicName  the topic name
     * @return    the max message size
     */
    int getTopicMaxMsgSizeInMB(String topicName);

    /**
     * Get topic control information
     *
     * @param topicNameSet  the topic name set need to query
     * @param qryEntity     the query conditions
     * @return    the topic control configures
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
    Map<String, Integer> getMaxMsgSizeInBByTopics(int defMaxMsgSizeInB, Set<String> topicNameSet);

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Add or update group control configure information
     *
     * @param isAddOp        whether add operation
     * @param opEntity       operator information
     * @param groupName      the group name
     * @param resCheckEnable  whether check resource rate
     * @param allowedBClientRate  the allowed broker-client rate
     * @param qryPriorityId  the query priority id
     * @param flowCtrlEnable enable or disable flow control
     * @param flowRuleCnt    the flow control rule count
     * @param flowCtrlInfo   the flow control information
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true if success otherwise false
     */
    GroupProcessResult addOrUpdGroupCtrlConf(boolean isAddOp, BaseEntity opEntity,
            String groupName, EnableStatus resCheckEnable,
            int allowedBClientRate, int qryPriorityId,
            EnableStatus flowCtrlEnable, int flowRuleCnt,
            String flowCtrlInfo, StringBuilder strBuff,
            ProcessResult result);

    /**
     * Add group control configure info
     *
     * @param entity     the group control info entity will be add
     * @param strBuff  the print info string buffer
     * @param result     the process result return
     * @return true if success otherwise false
     */
    GroupProcessResult addOrUpdGroupCtrlConf(boolean isAddOp, GroupResCtrlEntity entity,
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
    GroupProcessResult insertGroupCtrlConf(BaseEntity opEntity, String groupName,
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
    GroupProcessResult insertGroupCtrlConf(BaseEntity opEntity, String groupName,
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
    GroupProcessResult insertGroupCtrlConf(GroupResCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result);

    /**
     * Delete group resource control configure
     *
     * @param operator  operator
     * @param groupName the group will be deleted
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    GroupProcessResult delGroupResCtrlConf(String operator, String groupName,
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

    // //////////////////////////////////////////////////////////////////////////////

    /**
     * Add or update group resource configure information
     *
     * @param isAddOp        whether add operation
     * @param opEntity       operator information
     * @param groupName      the group name
     * @param topicName      the topic name
     * @param enableCsm      enable or disable consume
     * @param disableRsn     the disable reason
     * @param enableFlt      enable or disable filter
     * @param fltCondStr     the filter conditions
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true if success otherwise false
     */
    GroupProcessResult addOrUpdConsumeCtrlInfo(boolean isAddOp, BaseEntity opEntity,
            String groupName, String topicName,
            EnableStatus enableCsm, String disableRsn,
            EnableStatus enableFlt, String fltCondStr,
            StringBuilder strBuff, ProcessResult result);

    /**
     * add or update group's consume control information
     *
     * @param isAddOp   whether add operation
     * @param entity    need add or update group configure info
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    GroupProcessResult addOrUpdConsumeCtrlInfo(boolean isAddOp, GroupConsumeCtrlEntity entity,
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
    GroupProcessResult insertConsumeCtrlInfo(BaseEntity opEntity, String groupName,
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
    GroupProcessResult insertConsumeCtrlInfo(GroupConsumeCtrlEntity entity,
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
     * Get consume control records by group name
     *
     * @param groupName  the queried group name
     * @param topicName  the queried topic name
     * @return  the consume control record list
     */
    GroupConsumeCtrlEntity getConsumeCtrlByGroupAndTopic(String groupName, String topicName);

    /**
     * Get all group consume control record for a specific topic
     *
     * @param topicName  the queried topic name
     * @return group consume control list
     */
    List<GroupConsumeCtrlEntity> getConsumeCtrlByTopic(String topicName);

    /**
     * Get all group consume control records for the specific topic set
     *
     * @param topicSet  the queried topic name set
     * @return group consume control list
     */
    Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByTopic(Set<String> topicSet);

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
     * @return  the consume control record map
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
}
