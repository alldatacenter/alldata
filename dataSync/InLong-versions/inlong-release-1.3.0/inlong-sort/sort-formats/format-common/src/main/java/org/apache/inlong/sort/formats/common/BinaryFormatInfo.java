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

import java.nio.charset.StandardCharsets;

public class BinaryFormatInfo implements BasicFormatInfo<byte[]> {

    private static final long serialVersionUID = 8379022656220694709L;

    public static final BinaryFormatInfo INSTANCE = new BinaryFormatInfo();

    @Override
    public String serialize(byte[] record) throws Exception {
        return new String(record, StandardCharsets.UTF_8);
    }

    @Override
    public byte[] deserialize(String text) throws Exception {
        return text.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public TypeInfo getTypeInfo() {
        return BinaryTypeInfo.INSTANCE;
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
        return "BinaryFormatInfo";
    }

}
