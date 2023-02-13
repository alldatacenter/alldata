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
package com.qlangtech.tis.extension.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.util.DescribableJSON;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-12 11:05
 */
public class RootFormProperties extends PluginFormProperties {
    public final Map<String, /*** fieldname*/PropertyType> propertiesType;

    public RootFormProperties(Map<String, PropertyType> propertiesType) {
        this.propertiesType = propertiesType;
    }

    @Override
    public Set<Map.Entry<String, PropertyType>> getKVTuples() {
        return this.propertiesType.entrySet();
    }

    @Override
    public JSON getInstancePropsJson(Object instance) {
        JSONObject vals = new JSONObject();
        try {
            Object o = null;
            for (Map.Entry<String, PropertyType> entry : propertiesType.entrySet()) {
                // o = instance.getClass().getField(entry.getKey()).get(instance);
                // instance.getClass().getField(entry.getKey()).get(instance);
                o = entry.getValue().getVal(instance);
                if (o == null) {
                    continue;
                }
                if (entry.getValue().isDescribable()) {
                    DescribableJSON djson = new DescribableJSON((Describable) o);
                    vals.put(entry.getKey(), djson.getItemJson());
                } else {
                    vals.put(entry.getKey(), o);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("fetchKeys:" + propertiesType.keySet().stream().collect(Collectors.joining(","))
                    + "，hasKeys:" + Arrays.stream(instance.getClass().getFields()).map((r) -> r.getName()).collect(Collectors.joining(",")), e);
        }
        return vals;
    }

    @Override
    public <T> T accept(IVisitor visitor) {
        return visitor.visit(this);
    }
}
