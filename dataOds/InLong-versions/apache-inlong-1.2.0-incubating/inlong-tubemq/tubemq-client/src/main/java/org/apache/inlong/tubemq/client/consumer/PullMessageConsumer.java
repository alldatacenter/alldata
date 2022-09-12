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

import java.util.TreeSet;
import org.apache.inlong.tubemq.client.exception.TubeClientException;

public interface PullMessageConsumer extends MessageConsumer {

    boolean isPartitionsReady(long maxWaitTime);

    PullMessageConsumer subscribe(String topic,
                                  TreeSet<String> filterConds) throws TubeClientException;

    // getMessage() use note:
    // This getMessage have a blocking situation: when the current
    // consumer consumption situation is not satisfied (including
    // without partitions to consumption, or allocated partitions but
    // the partitions do not meet the consumption situation),
    // the call will sleep at intervals of ConsumerConfig.getPullConsumeReadyChkSliceMs(),
    // until the total time of ConsumerConfig.getPullConsumeReadyWaitPeriodMs
    ConsumerResult getMessage() throws TubeClientException;

    ConsumerResult confirmConsume(String confirmContext,
                                  boolean isConsumed) throws TubeClientException;
}
