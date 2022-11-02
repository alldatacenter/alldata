/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles logging of performance measurements.
 */
public final class AtlasPerfTracer {
    protected final Logger logger;
    protected final String tag;
    private   final long   startTimeMs;

    private static long reportingThresholdMs = 0L;

    public static Logger getPerfLogger(String name) {
        return LoggerFactory.getLogger("org.apache.atlas.perf." + name);
    }

    public static Logger getPerfLogger(Class<?> cls) {
        return AtlasPerfTracer.getPerfLogger(cls.getName());
    }

    public static boolean isPerfTraceEnabled(Logger logger) {
        return logger.isDebugEnabled();
    }

    public static AtlasPerfTracer getPerfTracer(Logger logger, String tag) {
        return new AtlasPerfTracer(logger, tag);
    }

    public static void log(AtlasPerfTracer tracer) {
        if (tracer != null) {
            tracer.log();
        }
    }

    private AtlasPerfTracer(Logger logger, String tag) {
        this.logger = logger;
        this.tag    = tag;
        startTimeMs = System.currentTimeMillis();
    }

    public String getTag() {
        return tag;
    }

    public long getStartTime() {
        return startTimeMs;
    }

    public long getElapsedTime() {
        return System.currentTimeMillis() - startTimeMs;
    }

    public void log() {
        long elapsedTime = getElapsedTime();
        if (elapsedTime > reportingThresholdMs) {
            logger.debug("PERF|{}|{}", tag, elapsedTime);
        }
    }
}
