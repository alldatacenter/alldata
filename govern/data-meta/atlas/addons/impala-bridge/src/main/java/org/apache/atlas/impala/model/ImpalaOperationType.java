/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.impala.model;

public enum ImpalaOperationType{
    // main operation type
    CREATEVIEW ("CREATEVIEW"),
    CREATETABLE_AS_SELECT ("CREATETABLE_AS_SELECT"),
    ALTERVIEW_AS ("ALTERVIEW_AS"),
    QUERY ("QUERY"),

    // sub operation type, which is associated with output
    INSERT ("INSERT"),
    INSERT_OVERWRITE ("INSERT_OVERWRITE"),

    // default type
    UNKNOWN ("UNKNOWN");

    private final String name;

    ImpalaOperationType(String s) {
        name = s;
    }

    public boolean equalsName(String otherName) {
        return name.equals(otherName);
    }

    public String toString() {
        return this.name;
    }
}