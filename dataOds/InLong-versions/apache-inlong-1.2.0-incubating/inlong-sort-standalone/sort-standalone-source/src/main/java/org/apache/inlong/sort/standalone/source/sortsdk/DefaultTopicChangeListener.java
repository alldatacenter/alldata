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

package org.apache.inlong.sort.standalone.source.sortsdk;

import org.apache.inlong.sdk.sort.api.InLongTopicChangeListener;
import org.apache.inlong.sdk.sort.entity.InLongTopic;

import java.util.Set;

/**
 * Default implementation of {@link InLongTopicChangeListener}.
 *
 * Do nothing when the topic assignments change.
 *
 */
public class DefaultTopicChangeListener implements InLongTopicChangeListener {

    @Override
    public boolean onAssignmentsChange(
            Set<InLongTopic> allInLongTopics,
            Set<InLongTopic> newInLongTopics,
            Set<InLongTopic> removedInLongTopics) {
        return true;
    }

    @Override
    public boolean requestAck(String sortTaskId) {
        return true;
    }

    @Override
    public String offlineAndGetAckOffset(String msgKey) {
        return "";
    }
}
