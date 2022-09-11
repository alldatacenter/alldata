/*
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

package org.apache.inlong.sort.formats.common;

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The format information for rows.
 */
public class RowFormatInfo implements FormatInfo {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_FIELD_NAMES = "fieldNames";
    private static final String FIELD_FIELD_FORMATS = "fieldFormats";

    public static final RowFormatInfo EMPTY =
            new RowFormatInfo(new String[0], new FormatInfo[0]);

    @JsonProperty(FIELD_FIELD_NAMES)
    @Nonnull
    private final String[] fieldNames;

    @JsonProperty(FIELD_FIELD_FORMATS)
    @Nonnull
    private final FormatInfo[] fieldFormatInfos;

    @JsonCreator
    public RowFormatInfo(
            @JsonProperty(FIELD_FIELD_NAMES) @Nonnull String[] fieldNames,
            @JsonProperty(FIELD_FIELD_FORMATS) @Nonnull FormatInfo[] fieldFormatInfos
    ) {
        checkArity(fieldNames, fieldFormatInfos);
        checkDuplicates(fieldNames);

        this.fieldNames = fieldNames;
        this.fieldFormatInfos = fieldFormatInfos;
    }

    @Nonnull
    public String[] getFieldNames() {
        return fieldNames;
    }

    @Nonnull
    public FormatInfo[] getFieldFormatInfos() {
        return fieldFormatInfos;
    }

    @Override
    public RowTypeInfo getTypeInfo() {
        TypeInfo[] fieldTypeInfos = new TypeInfo[fieldFormatInfos.length];
        for (int i = 0; i < fieldFormatInfos.length; ++i) {
            fieldTypeInfos[i] = fieldFormatInfos[i].getTypeInfo();
        }

        return new RowTypeInfo(fieldNames, fieldTypeInfos);
    }

    private static void checkArity(
            String[] fieldNames,
            FormatInfo[] fieldFormatInfos
    ) {
        if (fieldNames.length != fieldFormatInfos.length) {
            throw new IllegalArgumentException("The number of names and " + "formats is not equal.");
        }
    }

    private static void checkDuplicates(String[] fieldNames) {
        long numFieldNames = fieldNames.length;
        long numDistinctFieldNames =
                Arrays.stream(fieldNames)
                        .collect(Collectors.toSet())
                        .size();

        if (numDistinctFieldNames != numFieldNames) {
            throw new IllegalArgumentException("There exist duplicated " + "field names.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RowFormatInfo that = (RowFormatInfo) o;
        return Arrays.equals(fieldFormatInfos, that.fieldFormatInfos);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fieldFormatInfos);
    }

    @Override
    public String toString() {
        return "RowFormatInfo{" + "fieldFormatInfos=" + Arrays.toString(fieldFormatInfos) + '}';
    }
}
