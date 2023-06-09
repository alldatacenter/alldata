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
package org.apache.drill.exec.schema;

import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class DataRecord {
    private final Map<Integer, Object> dataMap;

    public DataRecord() {
        this.dataMap = Maps.newHashMap();
    }

    public void addData(int fieldId, Object data, boolean isList) {
        //TODO: Rethink lists vs object data handling
        if(!dataMap.containsKey(fieldId)) {
            if(isList) {
                dataMap.put(fieldId, Lists.newArrayList(data));
            } else {
                dataMap.put(fieldId, data);
            }
        } else {
            if(isList) {
                ((List<Object>)dataMap.get(fieldId)).add(data);
            } else {
                throw new IllegalStateException("Overriding field id existing data!");
            }
        }
    }

    public Object getData(int fieldId) {
        Preconditions.checkArgument(dataMap.containsKey(fieldId));
        return dataMap.get(fieldId);
    }
}
