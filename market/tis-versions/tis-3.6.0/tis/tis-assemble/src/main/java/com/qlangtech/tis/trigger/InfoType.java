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

/**
 *
 */
package com.qlangtech.tis.trigger;

public enum InfoType {
    INFO(1), WARN(2), ERROR(3), FATAL(4);

    private final int type;

    private InfoType(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public static InfoType getType(int type) {

        for (InfoType t : InfoType.values()) {
            if (type == t.type) {
                return t;
            }
        }

        throw new IllegalArgumentException("type:" + type + " is invalid");
    }
}
