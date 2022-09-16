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

/**
 * The format information for doubles.
 */
public class DoubleFormatInfo implements BasicFormatInfo<Double> {

    private static final long serialVersionUID = 1L;

    public static final DoubleFormatInfo INSTANCE = new DoubleFormatInfo();

    @Override
    public DoubleTypeInfo getTypeInfo() {
        return DoubleTypeInfo.INSTANCE;
    }

    @Override
    public String serialize(Double obj) {
        return obj.toString();
    }

    @Override
    public Double deserialize(String text) {
        return Double.valueOf(text.trim());
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
        return "DoubleFormatInfo";
    }
}
