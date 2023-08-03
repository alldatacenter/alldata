/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.base.util;

import org.apache.inlong.sort.base.security.FlinkSecurityManager;
import org.apache.inlong.sort.base.util.concurrent.ThreadUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.Thread.UncaughtExceptionHandler;

public class FatalExitExceptionHandler implements UncaughtExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(FatalExitExceptionHandler.class);
    public static final FatalExitExceptionHandler INSTANCE = new FatalExitExceptionHandler();
    public static final int EXIT_CODE = -17;

    public FatalExitExceptionHandler() {
    }

    public void uncaughtException(Thread t, Throwable e) {
        try {
            LOG.error("FATAL: Thread '{}' produced an uncaught exception. Stopping the process...", t.getName(), e);
            ThreadUtils.errorLogThreadDump(LOG);
        } finally {
            FlinkSecurityManager.forceProcessExit(-17);
        }

    }
}