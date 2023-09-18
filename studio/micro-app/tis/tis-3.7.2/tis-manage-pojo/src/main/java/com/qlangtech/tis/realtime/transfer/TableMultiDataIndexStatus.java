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
package com.qlangtech.tis.realtime.transfer;

import com.qlangtech.tis.realtime.yarn.rpc.ConsumeDataKeeper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TableMultiDataIndexStatus extends ListenerStatusKeeper {

    // 单位s
    private static final int TAB_DATA_KEYYPER_EXPIRE_TIME = 30;

    private HashMap<String, LinkedList<ConsumeDataKeeper>> /*tableName*/
    tableConsumeData;

    private String fromAddress;

    private long updateTime;

    // 20s过期时间，收不到report过来的数据就删除该索引对应的uuid
    private static final long EXPIRE_TIME = 20;

    public TableMultiDataIndexStatus() {
        tableConsumeData = new HashMap<>();
    }

    public void put(String tableName, ConsumeDataKeeper consumeDataKeeper) {
        LinkedList<ConsumeDataKeeper> consumeDataKeeperList = tableConsumeData.computeIfAbsent(tableName, k -> new LinkedList<>());
        long createTime = consumeDataKeeper.getCreateTime();
        if (consumeDataKeeperList.size() > 0 && consumeDataKeeperList.getLast().getCreateTime() > createTime) {
            createTime = consumeDataKeeperList.getLast().getCreateTime();
        } else {
            consumeDataKeeperList.add(consumeDataKeeper);
        }
        // 剔除过期的数据
        Iterator<ConsumeDataKeeper> it = consumeDataKeeperList.iterator();
        while (it.hasNext()) {
            ConsumeDataKeeper dataKeeper = it.next();
            if (dataKeeper.getCreateTime() < createTime - TAB_DATA_KEYYPER_EXPIRE_TIME) {
                it.remove();
            } else {
                break;
            }
        }
    // // 剔除过期的数据
    // while (consumeDataKeeperList.size() > 0) {
    // if (consumeDataKeeperList.peek().getCreateTime() < createTime - TAB_DATA_KEYYPER_EXPIRE_TIME) {
    // consumeDataKeeperList.pop();
    // } else {
    // break;
    // }
    // }
    }

    /**
     * 取得最后一个更新时间(秒),用于监控上的
     *
     * @return
     */
    public long getLastUpdateSec() {
        long max = 0;
        ConsumeDataKeeper last = null;
        for (LinkedList<ConsumeDataKeeper> stateQueue : tableConsumeData.values()) {
            last = stateQueue.getLast();
            if (max < last.getCreateTime()) {
                max = last.getCreateTime();
            }
        }
        return max;
    }

    public LinkedList<ConsumeDataKeeper> getConsumeDataKeepList(String tableName) {
        return tableConsumeData.get(tableName);
    }

    public Set<String> getTableNames() {
        return tableConsumeData.keySet();
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }

    public boolean isExpire(long currentTime) {
        return (currentTime - EXPIRE_TIME) > updateTime;
    }
}
