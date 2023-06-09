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

package com.qlangtech.tis.utils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-25 13:32
 **/
public class IntegerUtils {

    public static int intFromByteArray(byte[] buffer, int offset) {
        byte var2 = 0;
        int var3 = var2 | unsignedPromote(buffer[offset + 0]) << 24;
        var3 |= unsignedPromote(buffer[offset + 1]) << 16;
        var3 |= unsignedPromote(buffer[offset + 2]) << 8;
        var3 |= unsignedPromote(buffer[offset + 3]) << 0;
        return var3;
    }

    public static int unsignedPromote(byte b) {
        return b & 255;
    }
}
