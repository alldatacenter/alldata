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
package com.qlangtech.tis;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.qlangtech.tis.collectinfo.api.ICoreStatistics;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年9月11日上午11:47:16
 */
public class CoreStatisticsReportHistory {

    private Integer allCoreCount;

    private final ConcurrentHashMap<Integer, ICoreStatistics> /* appid */
    statis;

    /**
     * @param
     */
    public CoreStatisticsReportHistory() {
        super();
        this.statis = new ConcurrentHashMap<>();
    }

    public Set<java.util.Map.Entry<Integer, ICoreStatistics>> entrySet() {
        return statis.entrySet();
    }

    public Set<java.util.Map.Entry<Integer, ICoreStatistics>> entrySetWithOutValidate() {
        return statis.entrySet();
    }

    /**
     */
    private void checkConsist() {
    // int i = 0;
    // while (true) {
    // final int mapSize = this.size();
    // if (allCoreCount != null && (mapSize + 2) >= allCoreCount) {
    // break;
    // }
    //
    // if (i++ > 5) {
    // throw new IllegalStateException("mapsize:" + mapSize
    // + ",allCoreCount:" + allCoreCount);
    // }
    //
    // try {
    // Thread.sleep(3000);
    // } catch (InterruptedException e) {
    //
    // }
    // }
    }

    public ICoreStatistics get(Integer key) {
        checkConsist();
        return statis.get(key);
    }

    public void put(Integer key, ICoreStatistics value) {
        this.statis.put(key, value);
    }

    public void putIfAbsent(Integer key, ICoreStatistics value) {
        this.statis.putIfAbsent(key, value);
    }

    public Integer getAllCoreCount() {
        return allCoreCount;
    }

    public void setAllCoreCount(Integer allCoreCount) {
        this.allCoreCount = allCoreCount;
    }

    public void clear() {
        this.statis.clear();
        allCoreCount = null;
    }
}
