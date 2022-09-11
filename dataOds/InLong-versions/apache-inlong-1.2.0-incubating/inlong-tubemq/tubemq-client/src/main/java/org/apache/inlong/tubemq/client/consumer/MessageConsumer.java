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

package org.apache.inlong.tubemq.client.consumer;

import java.util.List;
import java.util.Map;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.client.exception.TubeClientException;
import org.apache.inlong.tubemq.corebase.Shutdownable;

public interface MessageConsumer extends Shutdownable {

    String getClientVersion();

    String getConsumerId();

    boolean isShutdown();

    ConsumerConfig getConsumerConfig();

    boolean isFilterConsume(String topic);

    /**
     * Get consume offset information of the current registered partitions
     *
     * @return  consume offset information
     */
    Map<String, ConsumeOffsetInfo> getCurConsumedPartitions() throws TubeClientException;

    /**
     * freeze partitions, the specified partition will no longer
     * consume data until the partition is unfrozen or
     * rebalanced to other clients in the same group
     *
     * @param partitionKeys  The partitionKey list that needs to be frozen
     */
    void freezePartitions(List<String> partitionKeys) throws TubeClientException;

    /**
     * unfreeze frozen partitions, the specified partition will
     * resume data consumption until the partition is frozen again
     *
     * @param partitionKeys  The partitionKey list that needs to be unfrozen
     */
    void unfreezePartitions(List<String> partitionKeys) throws TubeClientException;

    /**
     * unfreeze all frozen partitions, the unfreeze partition will
     * resume data consumption until the partition is frozen again
     *
     */
    void relAllFrozenPartitions();

    /**
     * get all local frozen partitions, if the frozen partition is on this client,
     * data consumption will only be restored after unfreezing;
     * if other consumers in the same group and other consumers
     * have not frozen the partition, the freezing operation will
     * not affect the consumption of other consumers
     *
     * @return local frozen partitions
     */
    Map<String, Long> getFrozenPartInfo();

    /**
     * Start consume messages with default setting
     */
    void completeSubscribe() throws TubeClientException;

    /**
     * Start consumption with the precise Offset settings
     *
     * The parameter sessionKey is specified by the caller, similar to the JobID in Flink,
     * which is used to identify the unrelated offset reset consumption activities before and after.
     * Each reset operation needs to ensure that it is different from the last reset carried sessionKey;
     *
     * The parameter sourceCount is used to inform the server how many consumers will consume
     * in this round of consumer group activation, and the client will not consume data until
     * the consumer group has not reached the specified number of consumers.
     *
     * The parameter isSelectBig is used to inform the server that if multiple clients reset
     * the offset to the same partition, the server will use the largest offset
     * or the smallest offset as the standard;
     *
     * The parameter partOffsetMap is used to inform the server that this consumption expects
     * the partitions in the Map to be consumed according to the specified offset value.
     * The offset in the Map comes from the consumer's query from the server, or the content
     * returned when the consumer successfully consumes the data before, including push
     * the PearInfo object returned by the callback function during consumption and
     * the PearInfo object in the ConsumerResult class during Pull consumption.
     * The Key in the Map is the partitionKey carried in PearInfo, and the value is
     * the currOffset value carried in PearInfo.
     */
    void completeSubscribe(String sessionKey,
                           int sourceCount,
                           boolean isSelectBig,
                           Map<String, Long> partOffsetMap) throws TubeClientException;

}
