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

package org.apache.inlong.sort.protocol.ddl.Utils;

import org.apache.inlong.sort.protocol.ddl.enums.PositionType;
import org.apache.inlong.sort.protocol.ddl.expressions.Column;
import org.apache.inlong.sort.protocol.ddl.expressions.Column.ColumnBuilder;
import org.apache.inlong.sort.protocol.ddl.expressions.Position;

import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utils for parse from statement in sqlParser to a column object.
 */
public class ColumnUtils {

    public static final String DEFAULT = "default";
    public static final String NULL = "null";
    public static final String NOT = "not";
    public static final String COMMENT = "comment";
    public static final String AFTER = "after";

    /**
     * parse column definition to a Column object
     * this method is used for alter operation where a first flag is passed
     * to determine whether the column is in the first position of one table.
     */
    public static Column parseColumnWithPosition(boolean isFirst,
            Map<String, Integer> sqlType,
            ColumnDefinition columnDefinition) {

        ColDataType colDataType = columnDefinition.getColDataType();

        List<String> definitions = new ArrayList<>();
        if (colDataType.getArgumentsStringList() != null) {
            definitions.addAll(colDataType.getArgumentsStringList());
        }

        List<String> columnSpecs = columnDefinition.getColumnSpecs();

        ColumnBuilder columnBuilder = Column.builder();
        String columnName = reformatName(columnDefinition.getColumnName());
        columnBuilder.name(columnName)
                .definition(definitions).isNullable(parseNullable(columnSpecs))
                .defaultValue(parseDefaultValue(columnSpecs))
                .jdbcType(sqlType.get(columnName))
                .comment(parseComment(columnSpecs));

        if (isFirst) {
            // the column is in the first position of one table
            columnBuilder.position(new Position(PositionType.FIRST, null));
        } else {
            columnBuilder.position(parsePosition(columnSpecs));
        }

        return columnBuilder.build();
    }

    /**
     * parse column definitions to Column list.
     * this method is used for createTable operation.
     * @param sqlType the sql type map
     * @param columnDefinitions the column definition list
     * @return the column list
     */
    public static List<Column> parseColumns(Map<String, Integer> sqlType,
            List<ColumnDefinition> columnDefinitions) {
        List<Column> columns = new ArrayList<>();
        columnDefinitions.forEach(columnDefinition -> {
            columns.add(parseColumnWithPosition(false, sqlType, columnDefinition));
        });
        return columns;
    }

    public static String parseDefaultValue(List<String> specs) {
        return removeContinuousQuotes(parseAdjacentString(specs, DEFAULT, false));
    }

    public static boolean parseNullable(List<String> specs) {
        return !parseAdjacentString(specs, NULL, true).equalsIgnoreCase(NOT);
    }

    public static String parseComment(List<String> specs) {
        return removeContinuousQuotes(parseAdjacentString(specs, COMMENT, false));
    }

    public static Position parsePosition(List<String> specs) {
        String afterColumn = reformatName(parseAdjacentString(specs, AFTER, false));
        if (!afterColumn.isEmpty()) {
            return new Position(PositionType.AFTER, afterColumn);
        }
        return null;
    }

    /**
     * get the string before or after the specific string in a list
     * @param stringList the string list
     * @param specificString the specific string
     * @param front is front of the specific string
     * @return the string before or after the specific string
     */
    public static String parseAdjacentString(List<String> stringList,
            String specificString, boolean front) {

        if (stringList == null || stringList.isEmpty()) {
            return "";
        }

        for (int i = 0; i < stringList.size(); i++) {
            if (stringList.get(i).equalsIgnoreCase(specificString)) {
                if (front && i > 0) {
                    return stringList.get(i - 1);
                } else if (i < stringList.size() - 1) {
                    return stringList.get(i + 1);
                }
            }
        }
        return "";

    }

    /**
     * remove the continuous char in the string from both sides.
     * @param str the input string, target the char to be removed
     * @return the string without continuous chars from both sides
     */
    public static String removeContinuousChar(String str, char target) {
        if (str == null || str.length() < 2) {
            return str;
        }
        int start = 0;
        int end = str.length() - 1;
        while (start <= end && str.charAt(start) == target) {
            start++;
        }
        while (end >= start && str.charAt(end) == target) {
            end--;
        }
        return str.substring(start, end + 1);
    }

    public static String removeContinuousQuotes(String str) {
        return removeContinuousChar(str, '\'');
    }

    public static String reformatName(String str) {
        return removeContinuousChar(str, '`');
    }

}
