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
package com.qlangtech.tis.manage.biz.dal.pojo;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年5月31日
 */
public class ClusterSnapshot implements Serializable {

    private static final long serialVersionUID = 1L;

    private Date fromTime;

    private Date toTime;

    private Long requestCount;

    private Long updateCount;

    public ThreadLocal<SimpleDateFormat> dateformat;

    @JSONField(serialize = false)
    public Date getFromTime() {
        return this.fromTime;
    }

    public String getLabel() {
        if (this.dateformat == null) {
            throw new IllegalStateException("dateformat can not be null");
        }
        return this.dateformat.get().format(this.getToTime());
    }

    public void setFromTime(Date fromTime) {
        this.fromTime = fromTime;
    }

    @JSONField(serialize = false)
    public Date getToTime() {
        return this.toTime;
    }

    public void setToTime(Date toTime) {
        this.toTime = toTime;
    }

    public Long getRequestCount() {
        return this.requestCount;
    }

    public void setRequestCount(Long requestCount) {
        this.requestCount = requestCount;
    }

    public Long getUpdateCount() {
        return updateCount;
    }

    public void setUpdateCount(Long updateCount) {
        this.updateCount = updateCount;
    }

    /**
     * 统计一段时间内的指标总量
     */
    public static class Summary {

        private long updateCount;

        private long queryCount;

        public Summary() {
        }

        public void add(ClusterSnapshot snapshot) {
            this.updateCount += snapshot.updateCount;
            this.queryCount += snapshot.requestCount;
        }

        public long getUpdateCount() {
            return updateCount;
        }

        public long getQueryCount() {
            return queryCount;
        }
    }
}
