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

import java.util.Map;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2015年10月23日 上午11:28:16
 */
public interface IOnsListenerStatus {

    public long getSolrConsumeIncrease();

    /**
     * 消费执行错误
     *
     * @return
     */
    public long getConsumeErrorCount();

    /**
     * 由于增量发送过来的表的更新并不是都关心的所以部分记录需要抛弃掉
     *
     * @return
     */
    public long getIgnoreRowsCount();

    public void cleanLastAccumulator();

    public String getCollectionName();

    /**
     * 返回的是json结构的
     *
     * @return
     */
    public String getTableUpdateCount();

    public int getBufferQueueUsedSize();

    /**
     * 缓冲区剩余容量
     *
     * @return
     */
    public int getBufferQueueRemainingCapacity();

    public long getConsumeIncreaseCount();

    /**
     * 重新启动消费增量
     */
    public void resumeConsume();

    /**
     * 增量任务是否暂停中
     *
     * @return
     */
    public boolean isPaused();

    /**
     * 停止消费增量日志
     */
    public void pauseConsume();

    public Set<Map.Entry<String, IIncreaseCounter>> getUpdateStatic();

    /**
     * 取得指标统计对象
     *
     * @param metricName
     * @return
     */
    public IIncreaseCounter getMetricCount(String metricName);
}
