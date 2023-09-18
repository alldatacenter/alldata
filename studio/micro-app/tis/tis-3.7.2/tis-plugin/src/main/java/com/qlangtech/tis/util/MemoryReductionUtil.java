/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.qlangtech.tis.util;


import java.util.HashMap;
import java.util.Map;

/**
 * Utilities to reduce memory footprint
 * @author Sam Van Oort
 */
public class MemoryReductionUtil {
    /** Returns the capacity we need to allocate for a HashMap so it will hold all elements without needing to resize. */
    public static int preallocatedHashmapCapacity(int elementsToHold) {
        if (elementsToHold <= 0) {
            return 0;
        } else if (elementsToHold < 3) {
            return elementsToHold + 1;
        } else {
            return elementsToHold + elementsToHold / 3; // Default load factor is 0.75, so we want to fill that much.
        }
    }

    /** Returns a mutable HashMap presized to hold the given number of elements without needing to resize. */
    public static Map getPresizedMutableMap(int elementCount) {
        return new HashMap(preallocatedHashmapCapacity(elementCount));
    }

    /** Empty string array, exactly what it says on the tin. Avoids repeatedly created empty array when calling "toArray." */
    public static final String[] EMPTY_STRING_ARRAY = new String[0];

    /** Returns the input strings, but with all values interned. */
    public static String[] internInPlace(String[] input) {
        if (input == null) {
            return null;
        } else if (input.length == 0) {
            return EMPTY_STRING_ARRAY;
        }
        for (int i = 0; i < input.length; i++) {
            input[i] = Util.intern(input[i]);
        }
        return input;
    }

}
