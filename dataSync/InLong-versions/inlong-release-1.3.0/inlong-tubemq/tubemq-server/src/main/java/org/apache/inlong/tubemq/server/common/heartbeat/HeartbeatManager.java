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

package org.apache.inlong.tubemq.server.common.heartbeat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.exception.HeartbeatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartbeatManager {

    private static final Logger logger = LoggerFactory.getLogger(HeartbeatManager.class);

    private final ConcurrentHashMap<String, TimeoutInfo> brokerRegMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TimeoutInfo> producerRegMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TimeoutInfo> consumerRegMap =
            new ConcurrentHashMap<>();
    private final ExecutorService timeoutScanService = Executors.newCachedThreadPool();
    private long brokerTimeoutDlt = 0;
    private long producerTimeoutDlt = 0;
    private long consumerTimeoutDlt = 0;
    private boolean isStopped = false;

    public HeartbeatManager() {

    }

    /**
     * Get the map of broker which consists of the key of nodes and the timeout info of nodes
     *
     * @return the map of timeout info of brokers' nodes.
     */
    public ConcurrentHashMap<String, TimeoutInfo> getBrokerRegMap() {
        return brokerRegMap;
    }

    /**
     * Get the map of producers which consists of the key of nodes and the timeout info of nodes
     *
     * @return the map of timeout info of producers' nodes
     */
    public ConcurrentHashMap<String, TimeoutInfo> getProducerRegMap() {
        return producerRegMap;
    }

    /**
     * Get the map of consumers which consists of the key of nodes and the timeout info of nodes
     *
     * @return the map of timeout info of consumers' nodes
     */
    public ConcurrentHashMap<String, TimeoutInfo> getConsumerRegMap() {
        return consumerRegMap;
    }

    /**
     * Get the delta of the consumer's timeout
     *
     * @return the timeout delta of a consumer
     */
    public long getConsumerTimeoutDlt() {
        return consumerTimeoutDlt;
    }

    /**
     * Register the check business for broker.
     *
     * @param timeout  the timeout to be registered for the broker.
     * @param listener the listener used in the broker for timeout business
     */
    public void regBrokerCheckBusiness(final long timeout, final TimeoutListener listener) {
        this.brokerTimeoutDlt = timeout;
        this.registerCheckBusiness("Broker Node", this.brokerRegMap, listener);
    }

    /**
     * Register the check business for producer.
     *
     * @param timeout  the timeout to be registered for the producer.
     * @param listener the listener used in the producer for timeout business
     */
    public void regProducerCheckBusiness(final long timeout, final TimeoutListener listener) {
        this.producerTimeoutDlt = timeout;
        this.registerCheckBusiness("Producer Node", this.producerRegMap, listener);
    }

    /**
     * Register the check business for consumer.
     *
     * @param timeout  the timeout to be registered for the consumer.
     * @param listener the listener used in the consumer for timeout business
     */
    public void regConsumerCheckBusiness(final long timeout, final TimeoutListener listener) {
        this.consumerTimeoutDlt = timeout;
        this.registerCheckBusiness("Consumer Node", this.consumerRegMap, listener);
    }

    private void registerCheckBusiness(final String businessType,
                                       final Map<String, TimeoutInfo> nodeMap,
                                       final TimeoutListener listener) {

        timeoutScanService.submit(new Runnable() {
            @Override
            public void run() {
                while (!isStopped) {
                    try {
                        long currentTime = System.currentTimeMillis();
                        Set<String> removedNodeKey = new HashSet<>();
                        for (Map.Entry<String, TimeoutInfo> entry : nodeMap.entrySet()) {
                            if (TStringUtils.isBlank(entry.getKey()) || entry.getValue() == null) {
                                continue;
                            }
                            if (currentTime >= entry.getValue().getTimeoutTime()) {
                                removedNodeKey.add(entry.getKey());
                            }
                        }
                        if (!removedNodeKey.isEmpty()) {
                            for (String nodeKey : removedNodeKey) {
                                TimeoutInfo timeoutInfo = nodeMap.get(nodeKey);
                                if (timeoutInfo == null) {
                                    continue;
                                }
                                if (currentTime >= timeoutInfo.getTimeoutTime()) {
                                    nodeMap.remove(nodeKey);
                                    listener.onTimeout(nodeKey, timeoutInfo);
                                }
                            }
                        }
                        Thread.sleep(1000 * 1);
                    } catch (Throwable t) {
                        logger.error(new StringBuilder(256)
                                .append(businessType).append(" heartbeat scan error!").toString(), t);
                    }
                }
            }
        });
    }

    /**
     * Register a node as broker.
     *
     * @param nodeId the id of a node to be registered.
     * @param createId  broker run-info block id
     * @return the timeout info for the registered node
     */
    public TimeoutInfo regBrokerNode(String nodeId, String createId) {
        return this.brokerRegMap.put(nodeId,
                new TimeoutInfo(createId, this.brokerTimeoutDlt));
    }

    /**
     * Register a node as producer.
     *
     * @param nodeId the id of a node to be registered.
     * @return the timeout info of the registered node
     */
    public TimeoutInfo regProducerNode(final String nodeId) {
        return this.producerRegMap.put(nodeId, new TimeoutInfo(this.producerTimeoutDlt));
    }

    /**
     * Register a node as consumer.
     *
     * @param nodeId the id of the node to be registered.
     * @return the timeout info of the registered node
     */
    public TimeoutInfo regConsumerNode(final String nodeId) {
        return this.consumerRegMap.put(nodeId, new TimeoutInfo(this.consumerTimeoutDlt));
    }

    /**
     * Register a node as consumer.
     *
     * @param nodeId     the id of the node to be registered
     * @param consumerId the second key to be used for the timeout
     * @param partStr    the third key to be used for the timeout
     * @return the timeout info of the registered consumer
     */
    public TimeoutInfo regConsumerNode(final String nodeId,
                                       final String consumerId,
                                       final String partStr) {
        return this.consumerRegMap.put(nodeId,
                new TimeoutInfo(consumerId, partStr, this.consumerTimeoutDlt));
    }

    /**
     * Unregister a node from the broker
     *
     * @param nodeId the id of node to be unregistered
     * @return if the timeout delete, true: success, false: failure
     */
    public boolean unRegBrokerNode(String nodeId, String createId) {
        TimeoutInfo timeoutInfo = brokerRegMap.get(nodeId);
        if (timeoutInfo == null) {
            return true;
        }
        if (!createId.equals(timeoutInfo.getSecondKey())) {
            return false;
        }
        timeoutInfo = brokerRegMap.remove(nodeId);
        return true;
    }

    /**
     * Unregister a node from the producer
     *
     * @param nodeId the id of node to be unregistered
     * @return the timeout of the node
     */
    public TimeoutInfo unRegProducerNode(final String nodeId) {
        return producerRegMap.remove(nodeId);
    }

    /**
     * Unregister a node from the consumer
     *
     * @param nodeId the id of node to be unregistered
     * @return the timeout of the node
     */
    public TimeoutInfo unRegConsumerNode(final String nodeId) {
        return consumerRegMap.remove(nodeId);
    }

    /**
     * Update a broker node.
     *
     * @param nodeId the id of node to be updated
     * @throws HeartbeatException if the timeout info of the node is not found
     */
    public boolean updBrokerNode(String nodeId, String createId,
                                 StringBuilder sBuffer, ProcessResult result) {
        TimeoutInfo timeoutInfo = brokerRegMap.get(nodeId);
        if (timeoutInfo == null) {
            result.setFailResult(TErrCodeConstants.HB_NO_NODE,
                    sBuffer.append("Invalid node id:").append(nodeId)
                            .append(", you have to append node first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (!createId.equals(timeoutInfo.getSecondKey())) {
            result.setFailResult(TErrCodeConstants.HB_NO_NODE,
                    sBuffer.append("Invalid node block id:").append(nodeId)
                            .append(", you have to append node first!").toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        timeoutInfo.updTimeoutTime(this.brokerTimeoutDlt);
        result.setSuccResult(null);
        return result.isSuccess();
    }

    /**
     * Update a producer node.
     *
     * @param nodeId the id of the node to be updated
     * @throws HeartbeatException if the timeout of the node is not found.
     */
    public void updProducerNode(final String nodeId) throws HeartbeatException {
        TimeoutInfo timeoutInfo = producerRegMap.get(nodeId);
        if (timeoutInfo == null) {
            throw new HeartbeatException(new StringBuilder(512)
                    .append("Invalid node id:").append(nodeId)
                    .append(", you have to append node first!").toString());
        }
        timeoutInfo.updTimeoutTime(this.producerTimeoutDlt);
    }

    /**
     * Update a consumer node.
     *
     * @param nodeId the id of the node to be updated
     * @throws HeartbeatException if the timeout of node is not found
     */
    public void updConsumerNode(final String nodeId) throws HeartbeatException {
        TimeoutInfo timeoutInfo = consumerRegMap.get(nodeId);
        if (timeoutInfo == null) {
            throw new HeartbeatException(new StringBuilder(512)
                    .append("Invalid node id:").append(nodeId)
                    .append(", you have to append node first!").toString());
        }
        timeoutInfo.updTimeoutTime(this.consumerTimeoutDlt);
    }

    /**
     * Stop the heartbeat.
     */
    public void stop() {
        isStopped = true;
    }

    /**
     * Clear all registered heartbeat business.
     */
    public void clearAllHeartbeat() {
        brokerRegMap.clear();
        producerRegMap.clear();
        consumerRegMap.clear();
    }
}
