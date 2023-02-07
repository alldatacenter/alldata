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

package org.apache.inlong.manager.pojo.queue.tubemq;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Broker view of TubeMQ, includes different status brokers list.
 */
@Data
public class TubeBrokerInfo {

    private static final String IDLE = "idle";
    private static final String RUNNING = "running";
    private static final String ONLINE = "online";
    private static final String ONLY_READ = "only-read";
    private static final String ONLY_WRITE = "only-write";

    private int errCode;
    private String errMsg;

    // total broker info list of brokers
    private List<BrokerInfo> data;
    // configurable list of brokers.
    private List<BrokerInfo> configurableList;
    // working state list of brokers
    private List<BrokerInfo> workingList;
    // idle broker list
    private List<BrokerInfo> idleList;
    // need reload broker list
    private List<Integer> needReloadList;

    /**
     * Divide broker list into different list by broker status.
     */
    public void divideBrokerListByStatus() {
        if (data != null) {
            configurableList = new ArrayList<>();
            workingList = new ArrayList<>();
            idleList = new ArrayList<>();
            needReloadList = new ArrayList<>();
            for (BrokerInfo brokerInfo : data) {
                if (brokerInfo.isConfigurable()) {
                    configurableList.add(brokerInfo);
                }
                if (brokerInfo.isWorking()) {
                    workingList.add(brokerInfo);
                }
                if (brokerInfo.isIdle()) {
                    idleList.add(brokerInfo);
                }
                if (brokerInfo.isConfChanged) {
                    needReloadList.add(brokerInfo.getBrokerId());
                }
            }
        }
    }

    /**
     * Get all configurable broker id list.
     */
    public List<Integer> getConfigurableBrokerIdList() {
        List<Integer> tmpBrokerIdList = new ArrayList<>();
        if (configurableList != null) {
            for (BrokerInfo brokerInfo : configurableList) {
                tmpBrokerIdList.add(brokerInfo.getBrokerId());
            }
        }
        return tmpBrokerIdList;
    }

    /**
     * Get all working broker id list.
     */
    public List<Integer> getWorkingBrokerIdList() {
        List<Integer> tmpBrokerIdList = new ArrayList<>();
        if (workingList != null) {
            for (BrokerInfo brokerInfo : workingList) {
                tmpBrokerIdList.add(brokerInfo.getBrokerId());
            }
        }
        return tmpBrokerIdList;
    }

    /**
     * Get all broker id list.
     */
    public List<Integer> getAllBrokerIdList() {
        List<Integer> allIdList = new ArrayList<>();
        if (data != null) {
            for (BrokerInfo brokerInfo : data) {
                allIdList.add(brokerInfo.getBrokerId());
            }
        }
        return allIdList;
    }

    /**
     * Broker info
     */
    @Data
    public static class BrokerInfo {

        private int brokerId;
        private String brokerIp;
        private int brokerPort;
        private String manageStatus;
        private String runStatus;
        private String subStatus;
        private int stepOp;
        private boolean isConfChanged;
        private boolean isConfLoaded;
        private boolean isBrokerOnline;
        private String brokerVersion;
        private boolean acceptPublish;
        private boolean acceptSubscribe;

        private boolean isIdle() {
            return IDLE.equals(subStatus);
        }

        private boolean isWorking() {
            return RUNNING.equals(runStatus) && (ONLINE.equals(manageStatus) || ONLY_READ.equals(manageStatus)
                    || ONLY_WRITE.equals(manageStatus));
        }

        private boolean isConfigurable() {
            return stepOp == 0 || stepOp == -2 || stepOp == 31 || stepOp == 32;
        }

        @Override
        public int hashCode() {
            return brokerId;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this) {
                return true;
            }
            if (!(o instanceof BrokerInfo)) {
                return false;
            }

            BrokerInfo brokerInfo = (BrokerInfo) o;
            return brokerId == brokerInfo.brokerId;
        }
    }

}
