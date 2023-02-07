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

package org.apache.inlong.manager.web.config;

import org.apache.inlong.common.util.MaskDataUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.pattern.PatternConverter;

@Plugin(name = "logmasker", category = PatternConverter.CATEGORY)
@ConverterKeys({"mask"})
public class LogMaskerConverter extends LogEventPatternConverter {

    /**
     * Constructs an instance of LoggingEventPatternConverter.
     *
     * @param name name of converter.
     * @param style CSS style for output.
     */
    protected LogMaskerConverter(String name, String style) {
        super(name, style);
    }

    public static LogMaskerConverter newInstance(String[] options) {
        return new LogMaskerConverter("mask", Thread.currentThread().getName());
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        StringBuilder message = new StringBuilder(event.getMessage().getFormattedMessage());
        MaskDataUtils.mask(message);
        toAppendTo.append(message);
    }
}
