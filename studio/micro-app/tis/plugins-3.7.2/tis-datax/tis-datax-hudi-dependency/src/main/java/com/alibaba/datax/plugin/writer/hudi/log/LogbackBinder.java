/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.datax.plugin.writer.hudi.log;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-21 19:20
 **/

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.classic.util.ContextSelectorStaticBinder;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import com.qlangtech.tis.manage.common.Config;
import org.slf4j.ILoggerFactory;
import org.slf4j.helpers.Util;
import org.slf4j.spi.LoggerFactoryBinder;

import java.net.URL;
import java.util.Objects;

public class LogbackBinder implements LoggerFactoryBinder {
    public static String REQUESTED_API_VERSION = "1.7.16";
    static final String NULL_CS_URL = "http://logback.qos.ch/codes.html#null_CS";
    private static LogbackBinder SINGLETON = new LogbackBinder();
    private static Object KEY = new Object();
    private boolean initialized = false;
    private LoggerContext defaultLoggerContext = new LoggerContext();
    private final ContextSelectorStaticBinder contextSelectorBinder = ContextSelectorStaticBinder.getSingleton();

    static {
        SINGLETON.init();
    }

    private LogbackBinder() {
        this.defaultLoggerContext.setName("default");
    }

    public static LogbackBinder getSingleton() {
        return SINGLETON;
    }

    static void reset() {
        SINGLETON = new LogbackBinder();
        SINGLETON.init();
    }

    void init() {
        try {
            try {
                (new ContextInitializer(this.defaultLoggerContext) {
                    public URL findURLOfDefaultConfigurationFile(boolean updateStatus) {
                        // super.findURLOfDefaultConfigurationFile(updateStatus);
                        return Objects.requireNonNull(LogbackBinder.this.getClass().getResource("/" + Config.SYSTEM_KEY__LOGBACK_HUDI)
                                , "resource can not be null:" + Config.SYSTEM_KEY__LOGBACK_HUDI);
                    }
                }).autoConfig();
            } catch (JoranException var2) {
                Util.report("Failed to auto configure default logger context", var2);
            }

            if (!StatusUtil.contextHasStatusListener(this.defaultLoggerContext)) {
                StatusPrinter.printInCaseOfErrorsOrWarnings(this.defaultLoggerContext);
            }

            this.contextSelectorBinder.init(this.defaultLoggerContext, KEY);
            this.initialized = true;
        } catch (Exception var3) {
            Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", var3);
        }

    }

    public ILoggerFactory getLoggerFactory() {
        if (!this.initialized) {
            return this.defaultLoggerContext;
        } else if (this.contextSelectorBinder.getContextSelector() == null) {
            throw new IllegalStateException("contextSelector cannot be null. See also http://logback.qos.ch/codes.html#null_CS");
        } else {
            return this.contextSelectorBinder.getContextSelector().getLoggerContext();
        }
    }

    public String getLoggerFactoryClassStr() {
        return this.contextSelectorBinder.getClass().getName();
    }
}
