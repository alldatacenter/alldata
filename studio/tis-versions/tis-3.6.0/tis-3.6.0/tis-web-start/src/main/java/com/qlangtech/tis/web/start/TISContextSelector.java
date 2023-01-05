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
package com.qlangtech.tis.web.start;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.selector.DefaultContextSelector;

/**
 * 为web容器中实现多app 日志隔离功能，<br/>
 * 参考：http://logback.qos.ch/manual/loggingSeparation.html
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-05-12 09:24
 */
public class TISContextSelector extends DefaultContextSelector {

    public TISContextSelector(LoggerContext context) {
        super(context);
    }

    @Override
    public LoggerContext getLoggerContext() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return super.getLoggerContext();
    }
}
