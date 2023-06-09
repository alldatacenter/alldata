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
package com.qlangtech.async.message.client.constants;

/*
 * 消息延迟枚举
 * <p>
 * messageDelayLevel = "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
 * 级别 =       1  2  3   4   5  6  7  8  9  10 11 12 13  14  15  16 17 18
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016 -08-29
 */
public enum NotifyDelayType {

    LEVEL_1(1, 1),
    LEVEL_2(2, 5),
    LEVEL_3(3, 10),
    LEVEL_4(4, 30),
    LEVEL_5(5, 60),
    LEVEL_6(6, 120),
    LEVEL_7(7, 180),
    LEVEL_8(8, 240),
    LEVEL_9(9, 300),
    LEVEL_10(10, 360),
    LEVEL_11(11, 420),
    LEVEL_12(12, 480),
    LEVEL_13(13, 540),
    LEVEL_14(14, 600),
    LEVEL_15(15, 1200),
    LEVEL_16(16, 1800),
    LEVEL_17(17, 3600),
    LEVEL_18(18, 7200);

    /**
     * 延迟级别
     */
    private int level;

    /**
     * 延迟秒
     */
    private int delaySecond;

    NotifyDelayType(int level, int delaySecond) {
        this.level = level;
        this.delaySecond = delaySecond;
    }

    /**
     * Gets level.
     *
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * Sets level.
     *
     * @param level the level
     */
    public void setLevel(int level) {
        this.level = level;
    }

    /**
     * Gets delay second.
     *
     * @return the delay second
     */
    public int getDelaySecond() {
        return delaySecond;
    }

    /**
     * Sets delay second.
     *
     * @param delaySecond the delay second
     */
    public void setDelaySecond(int delaySecond) {
        this.delaySecond = delaySecond;
    }
}
