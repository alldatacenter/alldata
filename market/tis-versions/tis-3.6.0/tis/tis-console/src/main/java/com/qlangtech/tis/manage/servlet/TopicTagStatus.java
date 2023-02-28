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
package com.qlangtech.tis.manage.servlet;

import com.alibaba.fastjson.annotation.JSONField;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-08-30 09:16
 */
public class TopicTagStatus {

    @JSONField(serialize = true)
    public String getKey() {
        return this.topic + "." + this.tag;
    }

    /**
     * tab名称
     *
     * @return
     */
    public String getTag() {
        return this.tag;
    }

    private final String topic;

    private final String tag;

    private long count;

    private long incr;

    private long lastUpdateTime;

    public TopicTagStatus(String topic, String tag, int count, long lastUpdate) {
        super();
        this.topic = topic;
        this.tag = tag;
        this.count = count;
        this.lastUpdateTime = lastUpdate;
    }

    public void merge(TopicTagStatus n) {
        if (!StringUtils.equals(this.getKey(), n.getKey())) {
            throw new IllegalArgumentException("key1:" + this.getKey() + ",key2:" + n.getKey() + " is not equal");
        }
        // this.setCount(this.count + n.count);
        if (n.lastUpdateTime > this.lastUpdateTime) {
            this.lastUpdateTime = n.lastUpdateTime;
        }
    }

    public void setCount(long count) {
        if (this.count > 0 && count > this.count) {
            this.incr = count - this.count;
        } else {
            this.incr = 0;
        }
        this.count = count;
    }

    @JSONField(serialize = true)
    public long getIncr() {
        return this.incr;
    }

    @JSONField(serialize = true)
    public long getLastUpdateTime() {
        return this.lastUpdateTime;
    }

    void clean() {
        this.count = 0;
        this.incr = 0;
    }

    @Override
    public String toString() {
        return "topic:" + this.topic + ",tag:" + this.tag + ",count:" + this.count + ",incr:" + this.incr + ",lastUpdate:" + this.lastUpdateTime;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdateTime = lastUpdate;
    }
}
