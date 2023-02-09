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

public class Log4j2Logger extends AbstractLog4jLogger implements ILogger {
    private static final Logger ILOG = Logger.getLogger(Log4j2Logger.class.getName());
    
    private static class Log4j2LoggerMethodHolder extends LoggerMethodHolder {
        private static Method isInfo;
        private static Method isDebug;
        private static Method isError;
        private static Method isWarn;
        private static Method isTrace;

        static {
            try {
                if (LoggerBuilder.GetLoggerHolder.loggerClass != null) {
                    isInfo = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isInfoEnabled");
                    isDebug = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isDebugEnabled");
                    isError = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isErrorEnabled");
                    isWarn = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isWarnEnabled");
                    isTrace = LoggerBuilder.GetLoggerHolder.loggerClass.getMethod("isTraceEnabled");
                }
            } catch (NoSuchMethodException | SecurityException e) {
                ILOG.warning(e.getMessage());
            }
        }
    }

    private volatile int isInfoE = -1;
    private volatile int isDebugE = -1;
    private volatile int isErrorE = -1;
    private volatile int isWarnE = -1;
    private volatile int isTraceE = -1;

    Log4j2Logger(Object logger) {
        super(logger);
    }

    public boolean isInfoEnabled() {
        if (isInfoE == -1) {
            try {
                isInfoE = (this.logger != null && Log4j2LoggerMethodHolder.isInfo != null
                        && (Boolean) (Log4j2LoggerMethodHolder.isInfo.invoke(this.logger))) ? 1 : 0;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                isInfoE = 0;
            }
        }
        return isInfoE == 1;
    }

    public boolean isWarnEnabled() {
        if (isWarnE == -1) {
            try {
                isWarnE = (this.logger != null && Log4j2LoggerMethodHolder.isWarn != null
                        && (Boolean) (Log4j2LoggerMethodHolder.isWarn.invoke(this.logger))) ? 1 : 0;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                isWarnE = 0;
            }
        }
        return isWarnE == 1;
    }

    public boolean isErrorEnabled() {
        if (isErrorE == -1) {
            try {
                isErrorE = (this.logger != null && Log4j2LoggerMethodHolder.isError != null
                        && (Boolean) (Log4j2LoggerMethodHolder.isError.invoke(this.logger))) ? 1 : 0;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                isErrorE = 0;
            }
        }
        return isErrorE == 1;
    }

    public boolean isDebugEnabled() {
        if (isDebugE == -1) {
            try {
                isDebugE = (this.logger != null && Log4j2LoggerMethodHolder.isDebug != null
                        && (Boolean) (Log4j2LoggerMethodHolder.isDebug.invoke(this.logger))) ? 1 : 0;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                isDebugE = 0;
            }
        }
        return isDebugE == 1;
    }

    public boolean isTraceEnabled() {
        if (isTraceE == -1) {
            try {
                isTraceE = (this.logger != null && Log4j2LoggerMethodHolder.isTrace != null
                        && (Boolean) (Log4j2LoggerMethodHolder.isTrace.invoke(this.logger))) ? 1 : 0;
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                isTraceE = 0;
            }
        }
        return isTraceE == 1;
    }
}
