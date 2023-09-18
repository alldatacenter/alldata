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
package com.qlangtech.tis.realtime.yarn.rpc;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ConsumeDataKeeper {

    private final long accumulation;

    private final long createTime;

    private static final long SecInMilli = 1000;

    public ConsumeDataKeeper(long accumulation, long createTime) {
        this.accumulation = accumulation;
        this.createTime = createTime;
    }

    public long getAccumulation() {
        return accumulation;
    }

    public long getCreateTime() {
        return createTime;
    }

    public static long getCurrentTimeInSec() {
        return System.currentTimeMillis() / SecInMilli;
    }
}
