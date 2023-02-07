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

package org.apache.inlong.sort.cdc.mongodb.table.filter;

public enum MongoRowKind {

    INSERT("+I", (byte) 0),
    UPDATE_BEFORE("-U", (byte) 1),
    UPDATE_AFTER("+U", (byte) 2),
    DELETE("-D", (byte) 3),
    DROP("-T", (byte) 4),
    DROP_DATABASE("-K", (byte) 5),
    RENAME("+R", (byte) 6);

    private final String shortString;
    private final byte value;

    private MongoRowKind(String shortString, byte value) {
        this.shortString = shortString;
        this.value = value;
    }

    public String shortString() {
        return this.shortString;
    }

    public byte toByteValue() {
        return this.value;
    }

    public static MongoRowKind fromByteValue(byte value) {
        switch (value) {
            case 0:
                return INSERT;
            case 1:
                return UPDATE_BEFORE;
            case 2:
                return UPDATE_AFTER;
            case 3:
                return DELETE;
            case 4:
                return DROP;
            case 5:
                return DROP_DATABASE;
            case 6:
                return RENAME;
            default:
                throw new UnsupportedOperationException("Unsupported byte value '" + value + "' for row kind.");
        }
    }
}