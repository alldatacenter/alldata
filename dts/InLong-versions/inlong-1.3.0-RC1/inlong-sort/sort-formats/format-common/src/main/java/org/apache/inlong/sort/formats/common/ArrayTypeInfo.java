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
 * The type information for arrays.
 */
public class ArrayTypeInfo implements TypeInfo {

    private static final long serialVersionUID = 1L;

    private static final String FIELD_ELEMENT_TYPE = "elementType";

    /**
     * The type information for elements.
     */
    @JsonProperty(FIELD_ELEMENT_TYPE)
    @Nonnull
    private final TypeInfo elementTypeInfo;

    @JsonCreator
    public ArrayTypeInfo(
            @JsonProperty(FIELD_ELEMENT_TYPE) @Nonnull TypeInfo elementTypeInfo
    ) {
        this.elementTypeInfo = elementTypeInfo;
    }

    @Nonnull
    public TypeInfo getElementTypeInfo() {
        return elementTypeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ArrayTypeInfo that = (ArrayTypeInfo) o;
        return elementTypeInfo.equals(that.elementTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementTypeInfo);
    }

    @Override
    public String toString() {
        return "ArrayTypeInfo{" + "elementTypeInfo=" + elementTypeInfo + '}';
    }
}
