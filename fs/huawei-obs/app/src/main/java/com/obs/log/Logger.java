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

public class Logger implements ILogger {
    private final ILogger delegate;

    Logger(Object logger) {
        Class<?> loggerCls = LoggerBuilder.GetLoggerHolder.loggerClass;
        if (logger instanceof java.util.logging.Logger) {
            this.delegate = new BasicLogger((java.util.logging.Logger) logger);
        } else if (loggerCls != null && loggerCls.getName().equals("org.apache.log4j.Logger")) {
            this.delegate = new Log4jLogger(logger);
        } else {
            this.delegate = new Log4j2Logger(logger);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return this.delegate.isInfoEnabled();
    }

    @Override
    public void info(CharSequence msg) {
        this.delegate.info(msg);
    }

    @Override
    public void info(Object obj) {
        this.delegate.info(obj);
    }

    @Override
    public void info(Object obj, Throwable e) {
        this.delegate.info(obj, e);
    }

    @Override
    public boolean isWarnEnabled() {
        return this.delegate.isWarnEnabled();
    }

    @Override
    public void warn(CharSequence msg) {
        this.delegate.warn(msg);
    }

    @Override
    public void warn(Object obj) {
        this.delegate.warn(obj);
    }

    @Override
    public void warn(Object obj, Throwable e) {
        this.delegate.warn(obj, e);
    }

    @Override
    public boolean isErrorEnabled() {
        return this.delegate.isErrorEnabled();
    }

    @Override
    public void error(CharSequence msg) {
        this.delegate.error(msg);
    }

    @Override
    public void error(Object obj) {
        this.delegate.error(obj);
    }

    @Override
    public void error(Object obj, Throwable e) {
        this.delegate.error(obj, e);
    }

    @Override
    public boolean isDebugEnabled() {
        return this.delegate.isDebugEnabled();
    }

    @Override
    public void debug(CharSequence msg) {
        this.delegate.debug(msg);
    }

    @Override
    public void debug(Object obj) {
        this.delegate.debug(obj);
    }

    @Override
    public void debug(Object obj, Throwable e) {
        this.delegate.debug(obj, e);
    }

    @Override
    public boolean isTraceEnabled() {
        return this.delegate.isTraceEnabled();
    }

    @Override
    public void trace(CharSequence msg) {
        this.delegate.trace(msg);
    }

    @Override
    public void trace(Object obj) {
        this.delegate.trace(obj);
    }

    @Override
    public void trace(Object obj, Throwable e) {
        this.delegate.trace(obj, e);
    }

    @Override
    public void accessRecord(Object obj) {
        this.delegate.accessRecord(obj);
    }

}
