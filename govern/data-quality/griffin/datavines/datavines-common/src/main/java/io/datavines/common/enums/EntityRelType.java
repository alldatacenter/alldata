/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;

public enum EntityRelType {

    /**
     * 0 upstream
     * 1 downstream
     * 2 child
     * 3 parent
     */
    UPSTREAM(0, "upstream"),
    DOWNSTREAM(1, "downstream"),
    CHILD(2, "child"),
    PARENT(3, "parent");

    EntityRelType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @EnumValue
    private final int code;
    private final String description;

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    private static final HashMap<String, EntityRelType> ENTITY_REL_TYPE_MAP = new HashMap<>();

    static {
        for (EntityRelType relType: EntityRelType.values()){
            ENTITY_REL_TYPE_MAP.put(relType.description, relType);
        }
    }

    public static EntityRelType of(String relType){
        if(ENTITY_REL_TYPE_MAP.containsKey(relType)){
            return ENTITY_REL_TYPE_MAP.get(relType);
        }
        throw new IllegalArgumentException("invalid entity rel type : " + relType);
    }
}
