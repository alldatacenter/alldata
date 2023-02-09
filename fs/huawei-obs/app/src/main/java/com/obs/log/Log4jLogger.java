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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class Log4jLogger extends AbstractLog4jLogger implements ILogger {
    private static final Logger ILOG = Logger.getLogger(Log4jLogger.class.getName());
    
    private static class Log4jLoggerMethodHolder extends LoggerMethodHolder {
        private static Method isEnabledFor;

        private static Class<?> priority;
        private static Class<?> level;

        private static Object infoLevel;
        private static Object debugLevel;
        private static Object errorLevel;
        private static Object warnLevel;
        private static Object traceLevel;

        static {
            try {
                if (LoggerBuilder.GetLoggerHolder.loggerClass != null) {
                    priority = Class.forName("org.apache.log4j.Priority");
                    isEnabledFor = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isEnabledFor", priority);

                    level = Class.forName("org.apache.log4j.Level");
                    infoLevel = level.getField("INFO").get(level);
                    debugLevel = level.getField("DEBUG").get(level);
                    errorLevel = level.getField("ERROR").get(level);
                    warnLevel = level.getField("WARN").get(level);
                    traceLevel = level.getField("TRACE").get(level);
                }
            } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalArgumentException
                    | IllegalAccessException | NoSuchFieldException e) {
                ILOG.warning(e.getMessage());
            }
        }
    }

    Log4jLogger(Object logger) {
        super(logger);
    }

    public boolean isInfoEnabled() {
        try {
            return this.logger != null && Log4jLoggerMethodHolder.infoLevel != null
                    && (Boolean) (Log4jLoggerMethodHolder.isEnabledFor.invoke(this.logger, 
                            Log4jLoggerMethodHolder.infoLevel));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }
    }

    public boolean isWarnEnabled() {
        try {
            return this.logger != null && Log4jLoggerMethodHolder.warnLevel != null
                    && (Boolean) (Log4jLoggerMethodHolder.isEnabledFor.invoke(this.logger, 
                            Log4jLoggerMethodHolder.warnLevel));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }
    }

    public boolean isErrorEnabled() {
        try {
            return this.logger != null && Log4jLoggerMethodHolder.errorLevel != null
                    && (Boolean) (Log4jLoggerMethodHolder.isEnabledFor.invoke(this.logger, 
                            Log4jLoggerMethodHolder.errorLevel));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }
    }

    public boolean isDebugEnabled() {
        try {
            return this.logger != null && Log4jLoggerMethodHolder.debugLevel != null
                    && (Boolean) (Log4jLoggerMethodHolder.isEnabledFor.invoke(this.logger, 
                            Log4jLoggerMethodHolder.debugLevel));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }
    }

    public boolean isTraceEnabled() {
        try {
            return this.logger != null && Log4jLoggerMethodHolder.traceLevel != null
                    && (Boolean) (Log4jLoggerMethodHolder.isEnabledFor.invoke(this.logger, 
                            Log4jLoggerMethodHolder.traceLevel));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            return false;
        }
    }
}
