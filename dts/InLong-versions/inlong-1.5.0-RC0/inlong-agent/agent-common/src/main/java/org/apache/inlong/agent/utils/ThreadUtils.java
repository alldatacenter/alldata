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

package org.apache.inlong.agent.utils;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ThreadUtils, used for handle specified throwable, such as oom, etc.
 */
public class ThreadUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadUtils.class);

    public static void threadThrowableHandler(Thread t, Throwable e) {
        if (AgentUtils.enableOOMExit()) {
            handleOOM(t, e);
        }
    }

    private static void handleOOM(Thread t, Throwable e) {
        if (ExceptionUtils.indexOfThrowable(e, java.lang.OutOfMemoryError.class) != -1) {
            LOGGER.error("Agent exit caused by {} OutOfMemory: ", t.getName(), e);
            forceShutDown();
        }
    }

    private static void forceShutDown() {
        try {
            Runtime.getRuntime().exit(-1);
        } catch (Throwable e) {
            LOGGER.error("exit failed, just halt, exception: ", e);
            Runtime.getRuntime().halt(-2);
        }
    }

}
