/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Cross-region replication configuration of a bucket
 *
 */
public class ReplicationConfiguration extends HeaderResponse {

    private String agency;

    private List<Rule> rules;

    public static class Rule {
        private String id;
        private RuleStatusEnum status;
        private String prefix;
        private Destination destination;
        private HistoricalObjectReplicationEnum historicalObjectReplication;

        /**
         * Obtain the rule ID.
         * 
         * @return Rule ID
         */
        public String getId() {
            return id;
        }

        /**
         * Set the rule ID.
         * 
         * @param id
         *            Rule ID
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Obtain the rule status.
         * 
         * @return Rule status
         */
        public RuleStatusEnum getStatus() {
            return status;
        }

        /**
         * Set the rule status.
         * 
         * @param status
         *            Rule status
         */
        public void setStatus(RuleStatusEnum status) {
            this.status = status;
        }

        /**
         * Obtain the replication status of historical objects.
         * 
         * @return Rule status
         */
        public HistoricalObjectReplicationEnum getHistoricalObjectReplication() {
            return historicalObjectReplication;
        }

        /**
         * Set historical object replication rule.
         * 
         * @param historicalObjectReplication
         *            Rule status
         */
        public void setHistoricalObjectReplication(HistoricalObjectReplicationEnum historicalObjectReplication) {
            this.historicalObjectReplication = historicalObjectReplication;
        }

        /**
         * Obtain the object name prefix that identifies objects to which the
         * rule applies.
         * 
         * @return Object name prefix
         */
        public String getPrefix() {
            return prefix;
        }

        /**
         * Set the object name prefix that identifies objects to which the rule
         * applies.
         * 
         * @param prefix
         *            Object name prefix
         */
        public void setPrefix(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Obtain the information about the replication destination.
         * 
         * @return Destination information
         */
        public Destination getDestination() {
            return destination;
        }

        /**
         * Set the information about the replication target.
         * 
         * @param destination
         *            Destination information
         */
        public void setDestination(Destination destination) {
            this.destination = destination;
        }

        @Override
        public String toString() {
            return "Rule [id=" + id + ", status=" + status + ", prefix=" + prefix + ", destination=" + destination
                    + "]";
        }
    }

    public static class Destination {
        private String bucket;
        private StorageClassEnum storageClass;

        /**
         * Obtain the information about the destination bucket.
         * 
         * @return Destination bucket name
         */
        public String getBucket() {
            return bucket;
        }

        /**
         * Set the destination bucket name.
         * 
         * @param bucket
         *            Destination bucket name
         */
        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        /**
         * Obtain the storage class of the object generated after the
         * replication.
         * 
         * @return Object storage class
         */
        public StorageClassEnum getObjectStorageClass() {
            return storageClass;
        }

        /**
         * Set the storage class of the object generated after the replication.
         * 
         * @param storageClass
         *            Object storage class
         */
        public void setObjectStorageClass(StorageClassEnum storageClass) {
            this.storageClass = storageClass;
        }

        @Override
        public String toString() {
            return "Destination [bucket=" + bucket + ", storageClass=" + storageClass + "]";
        }
    }

    /**
     * Obtain the replication rule list.
     * 
     * @return Replication rule list
     */
    public List<Rule> getRules() {
        if (rules == null) {
            rules = new ArrayList<Rule>();
        }
        return rules;
    }

    /**
     * Set the replication rule list.
     * 
     * @param rules
     *            Replication rule list
     */
    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Set the agent name.
     * 
     * @return Agent name
     */
    public String getAgency() {
        return agency;
    }

    /**
     * Obtain the agent name
     * 
     * @param agency
     *            Agent name
     */
    public void setAgency(String agency) {
        this.agency = agency;
    }

    @Override
    public String toString() {
        return "ReplicationConfiguration [agency=" + agency + ", rules=" + rules + "]";
    }
}
