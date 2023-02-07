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

package org.apache.inlong.common.enums;

/**
 * Row kind provided by flink
 */
public enum RowKindEnum {

    /**
     * Insertion operation.
     */
    INSERT("+I", (byte) 0),

    /**
     * Update operation with the previous content of the updated row.
     */
    UPDATE_BEFORE("-U", (byte) 1),

    /**
     * Update operation with new content of the updated row.
     */
    UPDATE_AFTER("+U", (byte) 2),

    /**
     * Deletion operation.
     */
    DELETE("-D", (byte) 3);

    private final String shortString;

    private final byte value;

    /**
     * Creates a {@link RowKindEnum} enum with the given short string and byte value representation of
     * the {@link RowKindEnum}.
     */
    RowKindEnum(String shortString, byte value) {
        this.shortString = shortString;
        this.value = value;
    }

    /**
     * Creates a {@link RowKindEnum} from the given byte value. Each {@link RowKindEnum} has a byte value
     * representation.
     *
     * @see #toByteValue() for mapping of byte value and {@link RowKindEnum}.
     */
    public static RowKindEnum fromByteValue(byte value) {
        switch (value) {
            case 0:
                return INSERT;
            case 1:
                return UPDATE_BEFORE;
            case 2:
                return UPDATE_AFTER;
            case 3:
                return DELETE;
            default:
                throw new UnsupportedOperationException("Unsupported byte value '" + value + "' for row kind.");
        }
    }

    /**
     * Returns a short string representation of this {@link RowKindEnum}.
     *
     * <ul>
     *   <li>"+I" represents {@link #INSERT}.
     *   <li>"-U" represents {@link #UPDATE_BEFORE}.
     *   <li>"+U" represents {@link #UPDATE_AFTER}.
     *   <li>"-D" represents {@link #DELETE}.
     * </ul>
     */
    public String shortString() {
        return shortString;
    }

    /**
     * Returns the byte value representation of this {@link RowKindEnum}. The byte value is used for
     * serialization and deserialization.
     *
     * <p>
     *
     * <ul>
     *   <li>"0" represents {@link #INSERT}.
     *   <li>"1" represents {@link #UPDATE_BEFORE}.
     *   <li>"2" represents {@link #UPDATE_AFTER}.
     *   <li>"3" represents {@link #DELETE}.
     * </ul>
     */
    public byte toByteValue() {
        return value;
    }

}
