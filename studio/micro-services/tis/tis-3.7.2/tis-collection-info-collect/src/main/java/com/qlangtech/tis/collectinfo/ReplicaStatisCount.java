/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.collectinfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import com.qlangtech.tis.collectinfo.ReplicaStatisCount.ReplicaNode;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年1月27日 下午5:20:58
 */
public class ReplicaStatisCount extends HashMap<ReplicaNode, AtomicLong> {

    private static final long serialVersionUID = 1L;

    private long count;

    public void add(ReplicaNode replica, Long add) {
        if (add == null) {
            return;
        }
        this.count += add;
        AtomicLong count = this.get(replica);
        if (count == null) {
            this.put(replica, new AtomicLong(add));
            return;
        }
        count.addAndGet(add);
    }

    public long getCount() {
        return count;
    }

    /**
     * 取得前后两次取样之间的增量值
     *
     * @param
     * @return
     */
    public long getIncreasement(ReplicaStatisCount newStatisCount) {
        long result = 0;
        AtomicLong preReplicValue = null;
        long increase = 0;
        for (Map.Entry<ReplicaNode, AtomicLong> entry : newStatisCount.entrySet()) {
            preReplicValue = this.get(entry.getKey());
            if (preReplicValue == null || (increase = (entry.getValue().get() - preReplicValue.get())) < 0) {
                result += entry.getValue().get();
            } else {
                result += increase;
            }
        }
        return result;
    }

    public static class ReplicaNode {

        private final Integer groupIndex;

        private final String host;

        public String getHost() {
            return host;
        }

        public ReplicaNode(Integer groupIndex, String host) {
            super();
            this.groupIndex = groupIndex;
            this.host = host;
        }

        @Override
        public int hashCode() {
            return (host + groupIndex).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return this.hashCode() == obj.hashCode();
        }
    }
}
