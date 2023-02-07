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

package org.apache.inlong.sort.formats.common;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The format information for varchar.
 */
public class VarCharFormatInfo implements BasicFormatInfo<String> {

    public static final VarCharFormatInfo INSTANCE = new VarCharFormatInfo();
    private static final long serialVersionUID = 1L;

    @JsonProperty("length")
    private int length;

    @JsonCreator
    public VarCharFormatInfo(@JsonProperty("length") int length) {
        this.length = length;
    }

    public VarCharFormatInfo() {
        this(1);
    }

    @Override
    public StringTypeInfo getTypeInfo() {
        return StringTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(String obj) {
        return obj;
    }

    @Override
    public String deserialize(String text) {
        return text.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "VarCharFormatInfo";
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
