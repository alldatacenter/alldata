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

package org.apache.inlong.manager.common.tool.excel;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public enum ExcelCellDataTransfer {

    DATE {

        final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        String parse2Text(Object cellValue) {
            if (cellValue == null) {
                return null;
            } else if (cellValue instanceof Date) {
                Date date = (Date) cellValue;
                return this.simpleDateFormat.format(date);
            } else {
                return String.valueOf(cellValue);
            }
        }

        Object parseFromText(String cellValue) {
            if (StringUtils.isNotBlank(cellValue)) {
                Date date = null;
                try {
                    date = this.simpleDateFormat.parse(cellValue);
                } catch (ParseException e) {
                    LOGGER.error("Can not properly parse value in Date type: " + cellValue, e);
                }
                return date;
            } else {
                return cellValue;
            }
        }
    },
    NONE {

        String parse2Text(Object o) {
            return String.valueOf(o);
        }

        Object parseFromText(String s) {
            return s;
        }
    };
    protected static final Logger LOGGER = LoggerFactory.getLogger(ExcelCellDataTransfer.class);

    private ExcelCellDataTransfer() {
    }

    /**
     * Parse the given object to text.
     */

    abstract String parse2Text(Object o);

    /**
     * Parse the given text to object.
     */
    abstract Object parseFromText(String s);
}
