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

package org.apache.inlong.sort.standalone.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper to unescape string.
 */
public class UnescapeHelper {

    /**
     * Unescape String format field values to String List format by separator.
     *
     * @param  fieldValues FiledValues in String format.
     * @param  separator Separator.
     * @return FiledValues in String List format.
     */
    public static List<String> toFiledList(String fieldValues, char separator) {
        List<String> fields = new ArrayList<String>();
        if (fieldValues.length() <= 0) {
            return fields;
        }

        int fieldLen = fieldValues.length();
        StringBuilder builder = new StringBuilder();
        int i = 0;
        for (; i < fieldLen - 1; i++) {
            char value = fieldValues.charAt(i);
            if (value == '\\') {
                char nextValue = fieldValues.charAt(i + 1);
                switch (nextValue) {
                    case '0':
                        builder.append(0x00);
                        i++;
                        break;
                    case 'n':
                        builder.append('\n');
                        i++;
                        break;
                    case 'r':
                        builder.append('\r');
                        i++;
                        break;
                    case '\\':
                        builder.append('\\');
                        i++;
                        break;
                    default:
                        if (nextValue == separator) {
                            builder.append(separator);
                            i++;
                        } else {
                            builder.append(value);
                        }
                        break;
                }
                if (i == fieldLen - 1) {
                    fields.add(builder.toString());
                }
            } else {
                if (value == separator) {
                    fields.add(builder.toString());
                    builder.delete(0, builder.length());
                } else {
                    builder.append(value);
                }
            }
        }

        if (i == fieldLen - 1) {
            char value = fieldValues.charAt(i);
            if (value == separator) {
                fields.add(builder.toString());
                fields.add("");
            } else {
                builder.append(value);
                fields.add(builder.toString());
            }
        }
        return fields;
    }

}
