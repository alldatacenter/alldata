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

import java.util.Objects;
import javax.annotation.Nonnull;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The format information for arrays.
 */
public class ArrayFormatInfo implements FormatInfo {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_ELEMENT_FORMAT = "elementFormat";

    /**
     * The format information for elements.
     */
    @JsonProperty(FIELD_ELEMENT_FORMAT)
    @Nonnull
    private final FormatInfo elementFormatInfo;

    @JsonCreator
    public ArrayFormatInfo(
            @JsonProperty(FIELD_ELEMENT_FORMAT) @Nonnull FormatInfo elementFormatInfo
    ) {
        this.elementFormatInfo = elementFormatInfo;
    }

    @Nonnull
    public FormatInfo getElementFormatInfo() {
        return elementFormatInfo;
    }

    @Override
    public ArrayTypeInfo getTypeInfo() {
        TypeInfo elementTypeInfo = elementFormatInfo.getTypeInfo();
        return new ArrayTypeInfo(elementTypeInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrayFormatInfo that = (ArrayFormatInfo) o;
        return elementFormatInfo.equals(that.elementFormatInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementFormatInfo);
    }

    @Override
    public String toString() {
        return "ArrayFormatInfo{" + "elementFormatInfo=" + elementFormatInfo + '}';
    }
}
