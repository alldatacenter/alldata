/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.utils;

import static com.aliyun.oss.common.utils.LogUtils.getLog;

public class RangeSpec {

    private static final String RANGE_PREFIX = "bytes=";

    public enum Type {
        NORMAL_RANGE, // a-b
        START_TO, // a-
        TO_END // -b
    }

    private long start;
    private long end;
    private Type type;

    public RangeSpec() {
        this(0, 0, Type.NORMAL_RANGE);
    }

    public RangeSpec(long start, long end, Type type) {
        this.start = start;
        this.end = end;
        this.type = type;
    }

    public long getStart() {
        return start;
    }

    public long getEnd() {
        return end;
    }

    public Type getType() {
        return type;
    }

    public static RangeSpec parse(long[] range) {
        if (range == null || range.length != 2) {
            getLog().warn("Invalid range value " + range + ", ignore it and just get entire object");
        }

        long start = range[0];
        long end = range[1];

        if (start < 0 && end < 0 || (start > 0 && end > 0 && start > end)) {
            getLog().warn("Invalid range value [" + start + ", " + end + "], ignore it and just get entire object");
        }

        RangeSpec rs;
        if (start < 0) {
            rs = new RangeSpec(-1, end, Type.TO_END);
        } else if (end < 0) {
            rs = new RangeSpec(start, -1, Type.START_TO);
        } else {
            rs = new RangeSpec(start, end, Type.NORMAL_RANGE);
        }

        return rs;
    }

    @Override
    public String toString() {
        String formatted = null;

        switch (type) {
        case NORMAL_RANGE:
            formatted = String.format("%s%d-%d", RANGE_PREFIX, start, end);
            break;

        case START_TO:
            formatted = String.format("%s%d-", RANGE_PREFIX, start);
            break;

        case TO_END:
            formatted = String.format("%s-%d", RANGE_PREFIX, end);
            break;
        }

        return formatted;
    }
}
