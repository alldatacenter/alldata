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

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * The broker node abnormal status reporting management class
 */
public class BrokerAbnHolder {
    private static final Logger logger =
            LoggerFactory.getLogger(BrokerAbnHolder.class);
    private final ConcurrentHashMap<Integer/* brokerId */, BrokerAbnInfo> brokerAbnormalMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer/* brokerId */, BrokerFbdInfo> brokerForbiddenMap =
            new ConcurrentHashMap<>();
    private final int maxAutoForbiddenCnt;
    private final MetaDataService metaDataService;
    private final AtomicInteger brokerForbiddenCount =
            new AtomicInteger(0);

    public BrokerAbnHolder(final int maxAutoForbiddenCnt,
                           final MetaDataService metaDataService) {
        this.maxAutoForbiddenCnt = maxAutoForbiddenCnt;
        this.metaDataService = metaDataService;
    }

    /**
     * Update stored broker report status
     *
     * @param brokerId           reported broker id
     * @param reportReadStatus   reported broker read status
     * @param reportWriteStatus  reported broker write status
     */
    public void updateBrokerReportStatus(int brokerId,
                                         int reportReadStatus,
                                         int reportWriteStatus) {
        StringBuilder sBuffer = new StringBuilder(512);
        if (reportReadStatus == 0 && reportWriteStatus == 0) {
            BrokerAbnInfo brokerAbnInfo = brokerAbnormalMap.get(brokerId);
            if (brokerAbnInfo != null) {
                if (brokerForbiddenMap.get(brokerId) == null) {
                    brokerAbnInfo = brokerAbnormalMap.remove(brokerId);
                    if (brokerAbnInfo != null) {
                        MasterSrvStatsHolder.decBrokerAbnormalCnt();
                        logger.warn(sBuffer.append("[Broker AutoForbidden] broker ")
                                .append(brokerId).append(" return to normal!").toString());
                        sBuffer.delete(0, sBuffer.length());
                    }
                }
            }
            return;
        }
        BrokerConfEntity curEntry =
                metaDataService.getBrokerConfByBrokerId(brokerId);
        if (curEntry == null) {
            return;
        }
        ManageStatus reqMngStatus =
                getManageStatus(reportWriteStatus, reportReadStatus);
        if ((curEntry.getManageStatus() == reqMngStatus)
            || ((reqMngStatus == ManageStatus.STATUS_MANAGE_OFFLINE)
            && (curEntry.getManageStatus().getCode() < ManageStatus.STATUS_MANAGE_ONLINE.getCode()))) {
            return;
        }
        BrokerAbnInfo brokerAbnInfo = brokerAbnormalMap.get(brokerId);
        if (brokerAbnInfo == null) {
            if (brokerAbnormalMap.putIfAbsent(brokerId,
                new BrokerAbnInfo(brokerId, reportReadStatus, reportWriteStatus)) == null) {
                MasterSrvStatsHolder.incBrokerAbnormalCnt();
                logger.warn(sBuffer.append("[Broker AutoForbidden] broker report abnormal, ")
                        .append(brokerId).append("'s reportReadStatus=")
                        .append(reportReadStatus).append(", reportWriteStatus=")
                        .append(reportWriteStatus).toString());
                sBuffer.delete(0, sBuffer.length());
            }
        } else {
            brokerAbnInfo.updateLastRepStatus(reportReadStatus, reportWriteStatus);
        }
        BrokerFbdInfo brokerFbdInfo = brokerForbiddenMap.get(brokerId);
        if (brokerFbdInfo == null) {
            BrokerFbdInfo tmpBrokerFbdInfo =
                new BrokerFbdInfo(brokerId, curEntry.getManageStatus(),
                        reqMngStatus, System.currentTimeMillis());
            if (reportReadStatus > 0 || reportWriteStatus > 0) {
                if (updateCurManageStatus(brokerId, reqMngStatus, sBuffer)) {
                    if (brokerForbiddenMap.putIfAbsent(brokerId, tmpBrokerFbdInfo) == null) {
                        brokerForbiddenCount.incrementAndGet();
                        MasterSrvStatsHolder.incBrokerForbiddenCnt();
                        logger.warn(sBuffer
                                .append("[Broker AutoForbidden] master add missing forbidden broker, ")
                                .append(brokerId).append("'s manage status to ")
                                .append(reqMngStatus.getDescription()).toString());
                        sBuffer.delete(0, sBuffer.length());
                    }
                }
            } else {
                if (brokerForbiddenCount.incrementAndGet() > maxAutoForbiddenCnt) {
                    brokerForbiddenCount.decrementAndGet();
                    return;
                }
                if (updateCurManageStatus(brokerId, reqMngStatus, sBuffer)) {
                    if (brokerForbiddenMap.putIfAbsent(brokerId, tmpBrokerFbdInfo) != null) {
                        brokerForbiddenCount.decrementAndGet();
                        return;
                    }
                    MasterSrvStatsHolder.incBrokerForbiddenCnt();
                    logger.warn(sBuffer
                            .append("[Broker AutoForbidden] master auto forbidden broker, ")
                            .append(brokerId).append("'s manage status to ")
                            .append(reqMngStatus.getDescription()).toString());
                    sBuffer.delete(0, sBuffer.length());
                } else {
                    brokerForbiddenCount.decrementAndGet();
                }
            }
        } else {
            if (updateCurManageStatus(brokerId, reqMngStatus, sBuffer)) {
                brokerFbdInfo.updateInfo(curEntry.getManageStatus(), reqMngStatus);
            }
        }
    }

    /**
     * get broker current report status
     *
     * @param brokerId the query broker id
     * @return the broker's current RW status
     */
    public Tuple2<Boolean, Boolean> getBrokerAutoFbdStatus(int brokerId) {
        Tuple2<Boolean, Boolean> retTuple = new Tuple2<>(false, false);
        BrokerFbdInfo brokerFbdInfo = brokerForbiddenMap.get(brokerId);
        if (brokerFbdInfo == null) {
            return retTuple;
        }
        retTuple.setF0AndF1(brokerFbdInfo.newStatus.isAcceptPublish(),
                brokerFbdInfo.newStatus.isAcceptSubscribe());
        return retTuple;
    }

    /**
     * Remove broker info and decrease total broker count and forbidden broker count
     *
     * @param brokerId the deleted broker id
     */
    public void removeBroker(Integer brokerId) {
        BrokerAbnInfo abnInfo = brokerAbnormalMap.remove(brokerId);
        if (abnInfo != null) {
            MasterSrvStatsHolder.decBrokerAbnormalCnt();
        }
        BrokerFbdInfo brokerFbdInfo = brokerForbiddenMap.remove(brokerId);
        if (brokerFbdInfo != null) {
            this.brokerForbiddenCount.decrementAndGet();
            MasterSrvStatsHolder.decBrokerForbiddenCnt();
        }
    }

    public void clear() {
        brokerForbiddenCount.set(0);
        brokerAbnormalMap.clear();
        brokerForbiddenMap.clear();

    }

    public int getCurrentBrokerCount() {
        return this.brokerForbiddenCount.get();
    }

    /**
     * Deduce status according to publish status and subscribe status
     *
     * @param repWriteStatus  broker's write status
     * @param repReadStatus   broker's read status
     * @return the broker's manage status
     */
    private ManageStatus getManageStatus(int repWriteStatus, int repReadStatus) {
        ManageStatus manageStatus;
        if (repWriteStatus == 0 && repReadStatus == 0) {
            manageStatus = ManageStatus.STATUS_MANAGE_ONLINE;
        } else if (repReadStatus != 0) {
            manageStatus = ManageStatus.STATUS_MANAGE_OFFLINE;
        } else {
            manageStatus = ManageStatus.STATUS_MANAGE_ONLINE_NOT_WRITE;
        }
        return manageStatus;
    }

    /**
     * Update broker status, if broker is not online, this operator will fail
     *
     * @param brokerId      reported broker id
     * @param newMngStatus  new manage status
     * @param sBuffer       string process buffer
     * @return true if success otherwise false
     */
    private boolean updateCurManageStatus(int brokerId,
                                          ManageStatus newMngStatus,
                                          StringBuilder sBuffer) {
        ProcessResult result = new ProcessResult();
        BaseEntity opEntity =
                new BaseEntity(TBaseConstants.META_VALUE_UNDEFINED,
                        "Broker AutoReport", new Date());
        metaDataService.changeBrokerConfStatus(opEntity,
                brokerId, newMngStatus, sBuffer, result);
        return result.isSuccess();
    }

    public Map<Integer, BrokerAbnInfo> getBrokerAbnormalMap() {
        return brokerAbnormalMap;
    }

    public BrokerFbdInfo getAutoForbiddenBrokerInfo(int brokerId) {
        return this.brokerForbiddenMap.get(brokerId);
    }

    public Map<Integer, BrokerFbdInfo> getAutoForbiddenBrokerMapInfo() {
        return this.brokerForbiddenMap;
    }

    /**
     * Release forbidden broker info
     *
     * @param brokerIdSet   need released broker set
     * @param reason        release reason
     */
    public void relAutoForbiddenBrokerInfo(Set<Integer> brokerIdSet, String reason) {
        if (brokerIdSet == null || brokerIdSet.isEmpty()) {
            return;
        }
        List<BrokerFbdInfo> brokerFbdInfos = new ArrayList<>();
        for (Integer brokerId : brokerIdSet) {
            BrokerFbdInfo fbdInfo = this.brokerForbiddenMap.remove(brokerId);
            if (fbdInfo != null) {
                brokerFbdInfos.add(fbdInfo);
                BrokerAbnInfo abnInfo = this.brokerAbnormalMap.remove(brokerId);
                if (abnInfo != null) {
                    MasterSrvStatsHolder.decBrokerAbnormalCnt();
                }
                this.brokerForbiddenCount.decrementAndGet();
                MasterSrvStatsHolder.decBrokerForbiddenCnt();
            }
        }
        if (!brokerFbdInfos.isEmpty()) {
            logger.info(new StringBuilder(512)
                    .append("[Broker AutoForbidden] remove forbidden brokers by reason ")
                    .append(reason).append(", release list is ").append(brokerFbdInfos.toString()).toString());
        }
    }

    // broker abnormal information structure
    public static class BrokerAbnInfo {
        private int brokerId;
        private int abnStatus;  // 0 normal , -100 read abnormal, -1 write abnormal, -101 r & w abnormal
        private long firstRepTime;
        private long lastRepTime;

        public BrokerAbnInfo(int brokerId, int reportReadStatus, int reportWriteStatus) {
            this.brokerId = brokerId;
            this.abnStatus = reportReadStatus * 100 + reportWriteStatus;
            this.firstRepTime = System.currentTimeMillis();
            this.lastRepTime = this.firstRepTime;
        }

        public void updateLastRepStatus(int reportReadStatus, int reportWriteStatus) {
            this.abnStatus = reportReadStatus * 100 + reportWriteStatus;
            this.lastRepTime = System.currentTimeMillis();
        }

        public int getAbnStatus() {
            return abnStatus;
        }

        public long getFirstRepTime() {
            return firstRepTime;
        }

        public long getLastRepTime() {
            return lastRepTime;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("brokerId", brokerId)
                    .append("abnStatus", abnStatus)
                    .append("firstRepTime", firstRepTime)
                    .append("lastRepTime", lastRepTime)
                    .toString();
        }
    }

    // broker forbidden information structure
    public static class BrokerFbdInfo {
        private int brokerId;
        private ManageStatus befStatus;
        private ManageStatus newStatus;
        private long forbiddenTime;
        private long lastUpdateTime;

        public BrokerFbdInfo(int brokerId, ManageStatus befStatus,
                             ManageStatus newStatus, long forbiddenTime) {
            this.brokerId = brokerId;
            this.befStatus = befStatus;
            this.newStatus = newStatus;
            this.forbiddenTime = forbiddenTime;
            this.lastUpdateTime = forbiddenTime;
        }

        public void updateInfo(ManageStatus befStatus, ManageStatus newStatus) {
            this.befStatus = befStatus;
            this.newStatus = newStatus;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                .append("brokerId", brokerId)
                .append("befStatus", befStatus)
                .append("newStatus", newStatus)
                .append("forbiddenTime", forbiddenTime)
                .append("lastUpdateTime", lastUpdateTime)
                .toString();
        }
    }

}
