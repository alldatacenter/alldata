/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.hook;


import org.apache.atlas.notification.LogConfigUtils;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import java.io.File;
import java.io.IOException;

/**
 * A logger wrapper that can be used to write messages that failed to be sent to a log file.
 */
public class FailedMessagesLogger {

    public static final String PATTERN_SPEC_TIMESTAMP_MESSAGE_NEWLINE = "%d{ISO8601} %m%n";
    public static final String DATE_PATTERN = ".yyyy-MM-dd";

    private final Logger logger = Logger.getLogger("org.apache.atlas.hook.FailedMessagesLogger");
    private String failedMessageFile;

    public FailedMessagesLogger(String failedMessageFile) {
        this.failedMessageFile = failedMessageFile;
    }

    void init() {
        String rootLoggerDirectory = LogConfigUtils.getRootDir();
        if (rootLoggerDirectory == null) {
            return;
        }
        String absolutePath = new File(rootLoggerDirectory, failedMessageFile).getAbsolutePath();
        try {
            DailyRollingFileAppender failedLogFilesAppender = new DailyRollingFileAppender(
                    new PatternLayout(PATTERN_SPEC_TIMESTAMP_MESSAGE_NEWLINE), absolutePath, DATE_PATTERN);
            logger.addAppender(failedLogFilesAppender);
            logger.setLevel(Level.ERROR);
            logger.setAdditivity(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void log(String message) {
        logger.error(message);
    }
}
