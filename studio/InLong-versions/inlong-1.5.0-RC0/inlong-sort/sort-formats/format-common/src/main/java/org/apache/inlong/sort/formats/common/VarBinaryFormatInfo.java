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

import java.nio.charset.StandardCharsets;

/**
 * The format information for VarBinary.
 */
public class VarBinaryFormatInfo implements BasicFormatInfo<byte[]> {

    private static final long serialVersionUID = 1L;

    public static final VarBinaryFormatInfo INSTANCE = new VarBinaryFormatInfo();

    public static final int MIN_LENGTH = 1;

    public static final int MAX_LENGTH = Integer.MAX_VALUE;

    public static final int DEFAULT_LENGTH = Integer.MAX_VALUE;

    @JsonProperty("length")
    private int length;

    public VarBinaryFormatInfo() {
        this(DEFAULT_LENGTH);
    }

    @JsonCreator
    public VarBinaryFormatInfo(@JsonProperty("length") int length) {
        if (length < MIN_LENGTH) {
            throw new IllegalArgumentException(
                    String.format(
                            "VarBinary string length must be between %d and %d (both inclusive).",
                            MIN_LENGTH, MAX_LENGTH));
        }
        this.length = length;
    }

    /**
     * Serializes the given record.
     *
     * @param record The record to be serialized.
     * @return The text serialized from the given record.
     */
    @Override
    public String serialize(byte[] record) throws Exception {
        return new String(record, StandardCharsets.UTF_8);
    }

    /**
     * Deserializes the given text.
     *
     * @param text The text to be deserialized.
     * @return The record deserialized from the given text.
     */
    @Override
    public byte[] deserialize(String text) throws Exception {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Returns the type represented by this format.
     *
     * @return the type represented by this format.
     */
    @Override
    public TypeInfo getTypeInfo() {
        return VarBinaryTypeInfo.INSTANCE;
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

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
