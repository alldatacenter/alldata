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

package org.apache.inlong.tubemq.server.broker.offset;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.server.broker.msgstore.MessageStore;
import org.apache.inlong.tubemq.server.broker.offset.offsetstorage.OffsetStorageInfo;

/**
 * Offset manager service interface.
 */
public interface OffsetService {

    void close(long waitTimeMs);

    OffsetStorageInfo loadOffset(MessageStore store, String group,
                                 String topic, int partitionId,
                                 int readStatus, long reqOffset,
                                 StringBuilder sb);

    long getOffset(MessageStore msgStore, String group,
                   String topic, int partitionId,
                   boolean isManCommit, boolean lastConsumed,
                   StringBuilder sb);

    long getOffset(String group, String topic, int partitionId);

    void bookOffset(String group, String topic, int partitionId,
                    int readDalt, boolean isManCommit, boolean isMsgEmpty,
                    StringBuilder sb);

    long commitOffset(String group, String topic,
                      int partitionId, boolean isConsumed);

    long resetOffset(MessageStore store, String group, String topic,
                     int partitionId, long reSetOffset, String modifier);

    long getTmpOffset(String group, String topic, int partitionId);

    Set<String> getBookedGroups();

    Set<String> getInMemoryGroups();

    Set<String> getUnusedGroupInfo();

    Set<String> getGroupSubInfo(String group);

    Map<String, Map<Integer, Tuple2<Long, Long>>> queryGroupOffset(
            String group, Map<String, Set<Integer>> topicPartMap);

    Map<String, OffsetRecordInfo> getOnlineGroupOffsetInfo();

    boolean modifyGroupOffset(Set<String> groups,
                              List<Tuple3<String, Integer, Long>> topicPartOffsets,
                              String modifier);

    void deleteGroupOffset(boolean onlyMemory,
                           Map<String, Map<String, Set<Integer>>> groupTopicPartMap,
                           String modifier);
}
