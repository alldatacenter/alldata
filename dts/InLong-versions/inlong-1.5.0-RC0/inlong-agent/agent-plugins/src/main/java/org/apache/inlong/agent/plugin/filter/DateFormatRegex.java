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

package org.apache.inlong.agent.plugin.filter;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * date format with regex string for absolute file path.
 */
public class DateFormatRegex {

    private static final Logger LOGGER = LoggerFactory.getLogger(DateFormatRegex.class);

    private static final String YEAR = "YYYY";
    private static final String YEAR_LOWERCASE = "yyyy";
    private static final String MONTH = "MM";
    private static final String DAY = "DD";
    private static final String DAY_LOWERCASE = "dd";
    private static final String HOUR = "HH";
    private static final String MINUTE = "mm";

    private static final String NORMAL_FORMATTER = "yyyyMMddHHmm";

    private static final String OFFSET_DAY = "d";
    private static final String OFFSET_MIN = "m";
    private static final String OFFSET_HOUR = "h";

    private int dayOffset = 0;
    private int hourOffset = 0;
    private int minuteOffset = 0;

    private String originRegex;
    private String formattedTime = "";
    private String formattedRegex;

    /**
     * set regex with current time
     */
    public static DateFormatRegex ofRegex(String regex) {
        DateFormatRegex dateFormatRegex = new DateFormatRegex(regex);
        dateFormatRegex.setRegexWithCurrentTime();
        return dateFormatRegex;
    }

    private DateFormatRegex(String originRegex) {
        this.originRegex = originRegex;
    }

    public boolean match(File file) {
        // TODO: check with more regex
        if (file.isFile()) {
            return PathUtils.antPathMatch(file.getAbsolutePath(), formattedRegex);
        } else if (file.isDirectory()) {
            return PathUtils.antPathIncluded(file.getAbsolutePath(), formattedRegex);
        }
        return false;
    }

    /**
     * set timeOffset
     */
    public DateFormatRegex withOffset(String timeOffset) {
        String number = StringUtils.substring(timeOffset, 0, timeOffset.length() - 1);
        String mark = StringUtils.substring(timeOffset, timeOffset.length() - 1);
        switch (mark) {
            case OFFSET_DAY:
                dayOffset = Integer.parseInt(number);
                break;
            case OFFSET_HOUR:
                hourOffset = Integer.parseInt(number);
                break;
            case OFFSET_MIN:
                minuteOffset = Integer.parseInt(number);
                break;
            default:
                LOGGER.warn("time offset is invalid, please check {}", timeOffset);
                break;
        }
        return this;
    }

    /**
     * set regex with given time, replace the YYYYDDMM pattern with given time
     */
    public void setRegexWithTime(String time) {
        String[] regexList = StringUtils.splitByWholeSeparatorPreserveAllTokens(originRegex,
                File.separator, 0);
        List<String> formattedList = new ArrayList<>();
        for (String regexStr : regexList) {
            if (regexStr.contains(YEAR) || regexStr.contains(YEAR_LOWERCASE)) {
                String tmpRegexStr = regexStr.replace(YEAR, time.substring(0, 4))
                        .replace(YEAR_LOWERCASE, time.substring(0, 4))
                        .replace(MONTH, time.substring(4, 6))
                        .replace(DAY, time.substring(6, 8))
                        .replace(DAY_LOWERCASE, time.substring(6, 8))
                        .replace(HOUR, time.substring(8, 10))
                        .replace(MINUTE, time.substring(10));
                formattedList.add(tmpRegexStr);
                formattedTime = time;
            } else {
                formattedList.add(regexStr);
            }
        }
        this.formattedRegex = StringUtils.join(formattedList, File.separatorChar);
        LOGGER.info("updated formatted regex is {}", this.formattedRegex);
    }

    /**
     * set regex with current time, replace the YYYYDDMM pattern with current time
     */
    public void setRegexWithCurrentTime() {
        String currentTime = AgentUtils.formatCurrentTimeWithOffset(NORMAL_FORMATTER,
                dayOffset, hourOffset, minuteOffset);
        setRegexWithTime(currentTime);
    }

    public String getFormattedRegex() {
        return formattedRegex;
    }

    public String getFormattedTime() {
        return formattedTime;
    }
}
