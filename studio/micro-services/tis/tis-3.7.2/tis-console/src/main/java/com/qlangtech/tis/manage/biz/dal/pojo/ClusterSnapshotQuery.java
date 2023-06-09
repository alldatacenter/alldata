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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-5-13
 */
public class ClusterSnapshotQuery {

    private final Date fromTime;

    private final Date toTime;

    // private final int interval;
    private final String sqlmapSuffix;

    public static void main(String[] args) {
        Calendar calendar = createToday();
        SimpleDateFormat f = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        System.out.println(f.format(calendar.getTime()));
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        System.out.println(f.format(calendar.getTime()));
    }

    // 索引ID
    private Integer appId;

    public Integer getAppId() {
        return appId;
    }

    public void setAppId(Integer appId) {
        this.appId = appId;
    }

    public String getSqlmapSuffix() {
        return this.sqlmapSuffix;
    }

    public static ClusterSnapshotQuery hour() {
        Calendar calendar = Calendar.getInstance();
        final Date to = calendar.getTime();
        calendar.add(Calendar.HOUR, -1);
        return new ClusterSnapshotQuery(calendar.getTime(), to, "last1Hour");
    }

    //
    // 五小时
    public static ClusterSnapshotQuery fiveHour() {
        Calendar calendar = Calendar.getInstance();
        Date to = calendar.getTime();
        calendar.add(Calendar.HOUR, -5);
        Date from = calendar.getTime();
        return new ClusterSnapshotQuery(from, to, "Last5hours");
    }

    // 一天
    public static ClusterSnapshotQuery hour24() {
        Calendar calendar = createToday();
        Date from = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date to = calendar.getTime();
        return new ClusterSnapshotQuery(from, to, "CurrentDay");
    }

    // 15天
    public static ClusterSnapshotQuery days15() {
        Calendar calendar = createToday();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date to = calendar.getTime();
        // 刚好显示24个柱子
        calendar.add(Calendar.DAY_OF_YEAR, -15);
        Date from = calendar.getTime();
        return new ClusterSnapshotQuery(from, to, "Last15day");
    }

    // 最近一个月
    public static ClusterSnapshotQuery last1Month() {
        Calendar calendar = createToday();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        Date to = calendar.getTime();
        // 刚好显示24个柱子
        calendar.add(Calendar.DAY_OF_YEAR, -30);
        Date from = calendar.getTime();
        return new ClusterSnapshotQuery(from, to, "Last1month");
    }

    private ClusterSnapshotQuery(Date fromDate, Date toDate, String sqlmapSuffix) {
        this.fromTime = fromDate;
        this.toTime = toDate;
        this.sqlmapSuffix = sqlmapSuffix;
    }

    public Date getFromTime() {
        return this.fromTime;
    }

    public Date getToTime() {
        return this.toTime;
    }

    private static Calendar createToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }
    // public int getInterval() {
    // return interval;
    // }
}
