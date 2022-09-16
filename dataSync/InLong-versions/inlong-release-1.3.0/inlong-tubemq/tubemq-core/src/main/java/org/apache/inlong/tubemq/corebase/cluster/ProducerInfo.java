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

package org.apache.inlong.tubemq.corebase.cluster;

import java.io.Serializable;
import java.util.Set;
import org.apache.inlong.tubemq.corebase.TokenConstants;

public class ProducerInfo implements Serializable {

    private static final long serialVersionUID = 8324918571047041166L;
    private final Set<String> topicSet;
    private final String host;
    private String producerId;
    private boolean overTLS = false;

    public ProducerInfo(String producerId, Set<String> topicSet, String host, boolean overTLS) {
        this.producerId = producerId;
        this.topicSet = topicSet;
        this.host = host;
        this.overTLS = overTLS;
    }

    public String getProducerId() {
        return producerId;
    }

    public void setProducerId(String producerId) {
        this.producerId = producerId;
    }

    public Set<String> getTopicSet() {
        return topicSet;
    }

    public void appendTopicSet(Set<String> topicSet) {
        this.topicSet.addAll(topicSet);
    }

    public boolean isOverTLS() {
        return overTLS;
    }

    public String getHost() {
        return host;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(512);
        builder.append(producerId);
        builder.append(TokenConstants.SEGMENT_SEP);
        int cnt = 0;
        for (String topic : topicSet) {
            builder.append(topic);
            if (cnt != topicSet.size() - 1) {
                builder.append(TokenConstants.ATTR_SEP);
            }
        }
        builder.append("@overTLS=").append(overTLS);
        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }

        final ProducerInfo other = (ProducerInfo) obj;
        return (this.producerId.equals(other.producerId)
                && this.topicSet.equals(other.topicSet));
    }

}
