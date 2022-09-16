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

import lombok.Getter;

public enum ComponentTypeEnum {

    Agent("AGENT"),

    DataProxy("DATAPROXY"),

    Cache("CACHE"),

    Sort("SORT"),

    SDK("SDK");

    @Getter
    private final String name;

    ComponentTypeEnum(String name) {
        this.name = name;
    }

    public static ComponentTypeEnum forName(String name) {
        for (ComponentTypeEnum componentType : values()) {
            if (componentType.getName().equals(name)) {
                return componentType;
            }
        }
        throw new IllegalArgumentException(String.format("Unsupport componentName for Inlong:%s", name));
    }
}
