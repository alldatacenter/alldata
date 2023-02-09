/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.log;

import java.lang.reflect.Method;

public class LoggerBuilder {
    
    private static final java.util.logging.Logger ILOG = 
            java.util.logging.Logger.getLogger(LoggerBuilder.class.getName());
    
    static class GetLoggerHolder {
        static Class<?> logManagerClass;
        static Class<?> loggerClass;
        static Method getLoggerClass;

        static {
            try {
                logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
                loggerClass = Class.forName("org.apache.logging.log4j.Logger");
                getLoggerClass = GetLoggerHolder.logManagerClass.getMethod("getLogger", String.class);
            } catch (NoSuchMethodException | SecurityException | ClassNotFoundException e) {
                try {
                    loggerClass = Class.forName("org.apache.log4j.Logger");
                    getLoggerClass = GetLoggerHolder.loggerClass.getMethod("getLogger", String.class);
                } catch (NoSuchMethodException | SecurityException | ClassNotFoundException ex) {
                    try {
                        loggerClass = Class.forName("java.util.logging.Logger");
                        getLoggerClass = GetLoggerHolder.loggerClass.getMethod("getLogger", String.class);
                    } catch (NoSuchMethodException | SecurityException | ClassNotFoundException exx) {
                        ILOG.warning(exx.getMessage());
                    }
                }
            }
        }
    }

    public static ILogger getLogger(String name) {
        if (GetLoggerHolder.getLoggerClass != null) {
            try {
                return new Logger(((Method) GetLoggerHolder.getLoggerClass).invoke(null, name));
            } catch (Exception e) {
                ILOG.warning(e.getMessage());
            }
        }
        return new Logger(null);
    }

    public static ILogger getLogger(Class<?> c) {
        return getLogger(c.getName());
    }
}
