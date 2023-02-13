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

import java.math.BigDecimal;

/**
 * The format information for decimals.
 */
public class DecimalFormatInfo implements BasicFormatInfo<BigDecimal> {

    public static final DecimalFormatInfo INSTANCE = new DecimalFormatInfo();

    public static final int DEFAULT_PRECISION = 10;

    public static final int DEFAULT_SCALE = 0;

    private static final long serialVersionUID = 1L;
    @JsonProperty("precision")
    private int precision;

    @JsonProperty("scale")
    private int scale;

    public DecimalFormatInfo() {
        this(DEFAULT_PRECISION, DEFAULT_SCALE);
    }

    public DecimalFormatInfo(@JsonProperty("scale") int scale) {
        this(DEFAULT_PRECISION, scale);
    }

    @JsonCreator
    public DecimalFormatInfo(@JsonProperty("precision") int precision, @JsonProperty("scale") int scale) {
        this.precision = precision;
        this.scale = scale;
    }

    @Override
    public DecimalTypeInfo getTypeInfo() {
        return DecimalTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(BigDecimal obj) {
        return obj.toString();
    }

    @Override
    public BigDecimal deserialize(String text) {
        return new BigDecimal(text.trim());
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
        return "DecimalFormatInfo";
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }
}
