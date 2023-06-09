/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.expr.fn.impl;

import io.netty.util.internal.PlatformDependent;

/**
 * The base class of hash classes used in Drill.
 */
public class DrillHash {

    public static final long getLongLittleEndian(long offset) {
        //return PlatformDependent.getLong(offset);
        return     ((long) PlatformDependent.getByte(offset+7)    << 56)
                | ((PlatformDependent.getByte(offset+6) & 0xffL) << 48)
                | ((PlatformDependent.getByte(offset+5) & 0xffL) << 40)
                | ((PlatformDependent.getByte(offset+4) & 0xffL) << 32)
                | ((PlatformDependent.getByte(offset+3) & 0xffL) << 24)
                | ((PlatformDependent.getByte(offset+2) & 0xffL) << 16)
                | ((PlatformDependent.getByte(offset+1) & 0xffL) << 8)
                | ((PlatformDependent.getByte(offset) & 0xffL));
    }

    public static final long getIntLittleEndian(long offset) {
        long retl = 0;
        retl = ((PlatformDependent.getByte(offset+3) &0xffL) << 24)
                | ((PlatformDependent.getByte(offset+2) & 0xffL) << 16)
                | ((PlatformDependent.getByte(offset+1) & 0xffL) << 8)
                | ((PlatformDependent.getByte(offset) & 0xffL));
        return retl;
    }

}