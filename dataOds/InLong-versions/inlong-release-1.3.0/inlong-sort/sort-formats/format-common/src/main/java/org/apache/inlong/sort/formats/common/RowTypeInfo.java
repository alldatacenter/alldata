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
 * The type information for rows.
 */
public class RowTypeInfo implements TypeInfo {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_FIELD_NAMES = "fieldNames";
    private static final String FIELD_FIELD_TYPES = "fieldTypes";

    public static final RowTypeInfo EMPTY =
            new RowTypeInfo(new String[0], new TypeInfo[0]);

    @JsonProperty(FIELD_FIELD_NAMES)
    @Nonnull
    private final String[] fieldNames;

    @JsonProperty(FIELD_FIELD_TYPES)
    @Nonnull
    private final TypeInfo[] fieldTypeInfos;

    @JsonCreator
    public RowTypeInfo(
            @JsonProperty(FIELD_FIELD_NAMES) @Nonnull String[] fieldNames,
            @JsonProperty(FIELD_FIELD_TYPES) @Nonnull TypeInfo[] fieldTypeInfos
    ) {
        checkArity(fieldNames, fieldTypeInfos);
        checkDuplicates(fieldNames);

        this.fieldNames = fieldNames;
        this.fieldTypeInfos = fieldTypeInfos;
    }

    private static void checkArity(
            String[] fieldNames,
            TypeInfo[] fieldTypeInfos
    ) {
        if (fieldNames.length != fieldTypeInfos.length) {
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

    @Nonnull
    public String[] getFieldNames() {
        return fieldNames;
    }

    @Nonnull
    public TypeInfo[] getFieldTypeInfos() {
        return fieldTypeInfos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RowTypeInfo that = (RowTypeInfo) o;
        return Arrays.equals(fieldTypeInfos, that.fieldTypeInfos);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(fieldTypeInfos);
    }

    @Override
    public String toString() {
        return "RowTypeInfo{" + "fieldTypeInfos=" + Arrays.toString(fieldTypeInfos) + '}';
    }
}
