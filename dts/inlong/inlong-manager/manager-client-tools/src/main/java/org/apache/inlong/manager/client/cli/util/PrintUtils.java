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

package org.apache.inlong.manager.client.cli.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.SimpleGroupStatus;
import org.apache.inlong.manager.common.enums.SimpleSourceStatus;
import org.apache.inlong.manager.common.util.JsonUtils;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Util that prints information to the console.
 */
public class PrintUtils {

    private static final String joint = "+";
    private static final String horizontal = "—";
    private static final String vertical = "|";

    /**
     * Print a list info to console with format.
     */
    public static <T, K> void print(List<T> item, Class<K> clazz) {
        if (item.isEmpty()) {
            return;
        }
        List<K> list = copyObject(item, clazz);
        int[] maxColumnWidth = getColumnWidth(list);
        printTable(list, maxColumnWidth);
    }

    /**
     * Print the given item to the console in JSON format.
     */
    public static <T> void printJson(T item) {
        try {
            System.out.println(JsonUtils.toPrettyJsonString(item));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prints the given list to the console with the specified width.
     */
    private static <K> void printTable(List<K> list, int[] columnWidth) {
        Field[] fields = list.get(0).getClass().getDeclaredFields();
        printLine(columnWidth, fields.length);
        System.out.print(vertical);

        String format = "%s" + vertical;
        for (int i = 0; i < fields.length; i++) {
            System.out.printf(format, StringUtils.center(fields[i].getName(), columnWidth[i]));
        }
        System.out.println();
        printLine(columnWidth, fields.length);

        list.forEach(item -> {
            for (int i = 0; i < fields.length; i++) {
                fields[i].setAccessible(true);
                try {
                    System.out.print(vertical);
                    Object obj = fields[i].get(item);
                    if (obj != null) {
                        int charNum = getSpecialCharNum(obj.toString());
                        if (fields[i].getType().equals(Date.class)) {
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            String date = dateFormat.format(obj);
                            System.out.printf("%s", StringUtils.center(date, columnWidth[i]));
                        } else if (charNum > 0) {
                            System.out.printf("%s", StringUtils.center(obj.toString(), columnWidth[i] - charNum));
                        } else {
                            System.out.printf("%s", StringUtils.center(obj.toString(), columnWidth[i]));
                        }
                    } else {
                        System.out.printf("%s", StringUtils.center("NULL", columnWidth[i]));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(vertical);
        });
        printLine(columnWidth, fields.length);
    }

    /**
     * Copy the objects in the list, converting them to the specified type.
     */
    private static <T, K> List<K> copyObject(List<T> list, Class<K> clazz) {
        List<K> newList = new ArrayList<>();
        list.forEach(item -> {
            try {
                K value = JsonUtils.parseObject(JsonUtils.toJsonString(item), clazz);
                assert value != null;
                parseStatus(value);
                newList.add(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return newList;
    }

    /**
     * Get the max-width for all columns in the given list info.
     */
    private static <K> int[] getColumnWidth(List<K> list) {
        Field[] fields = list.get(0).getClass().getDeclaredFields();
        int[] maxWidth = new int[fields.length];
        for (int i = 0; i < fields.length; i++) {
            maxWidth[i] = Math.max(fields[i].getName().length(), maxWidth[i]);
        }
        list.forEach(item -> {
            try {
                for (int i = 0; i < fields.length; i++) {
                    fields[i].setAccessible(true);
                    if (fields[i].get(item) != null) {
                        int length = fields[i].get(item).toString().getBytes().length;
                        maxWidth[i] = Math.max(length, maxWidth[i]);
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        for (int i = 0; i < maxWidth.length; i++) {
            maxWidth[i] += 4;
        }
        return maxWidth;
    }

    /**
     * Print the format line to the console.
     */
    private static void printLine(int[] columnWidth, int fieldNum) {
        System.out.print(joint);
        for (int i = 0; i < fieldNum; i++) {
            System.out.printf("%s", StringUtils.leftPad(joint, columnWidth[i] + 1, horizontal));
        }
        System.out.println();
    }

    /**
     * Get the char number for special string, such as the Chinese string.
     */
    private static int getSpecialCharNum(String str) {
        int i = str.getBytes().length - str.length();
        return i / 2;
    }

    /**
     * Parse the {@link ParseStatus} annotation, and transfer the status param to 'STATUS (status)',
     * such as 'STARTED (130)'
     */
    private static <T> void parseStatus(T target) {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            if (field.isAnnotationPresent(ParseStatus.class)) {
                try {
                    int code = Integer.parseInt(field.get(target).toString());
                    String name = "";
                    Class<?> clazz = field.getAnnotation(ParseStatus.class).clazz();
                    if (SimpleGroupStatus.class.equals(clazz)) {
                        name = SimpleGroupStatus.parseStatusByCode(code).toString();
                    } else if (SimpleSourceStatus.class.equals(clazz)) {
                        name = SimpleSourceStatus.parseByStatus(code).toString();
                    }
                    field.set(target, String.format("%s (%d)", name, code));
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
