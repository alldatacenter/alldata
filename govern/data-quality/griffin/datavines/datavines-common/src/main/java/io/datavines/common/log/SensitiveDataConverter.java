/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.log;

import io.datavines.common.utils.PasswordFilterUtils;

import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.MessageConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * sensitive data log converter
 */
public class SensitiveDataConverter extends MessageConverter {

    /**
     * dataSource sensitive param
     */
    public static final String DATASOURCE_PASSWORD_REGEX = "(?<=(\\\\\"password\\\\\":\\\\\")).*?(?=(\\\\\"))";

    /**
     * dataSource sensitive param
     */
    public static final String DATASOURCE_PASSWORD_REGEX_1 = "(?<=(\"password\":\")).*?(?=(\"))";

    /**
     * password pattern
     */
    public static final Pattern PWD_PATTERN = Pattern.compile(DATASOURCE_PASSWORD_REGEX);

    /**
     * password pattern
     */
    public static final Pattern PWD_PATTERN_1 = Pattern.compile(DATASOURCE_PASSWORD_REGEX_1);

    @Override
    public String convert(ILoggingEvent event) {

        // get original log
        String requestLogMsg = event.getFormattedMessage();
        // desensitization log
        return PasswordFilterUtils.convertPassword(PWD_PATTERN, requestLogMsg);
    }

}
