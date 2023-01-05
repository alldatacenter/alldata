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
package com.qlangtech.tis.realtime.transfer.ruledriven;

import java.util.Map;
import com.google.common.collect.Maps;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public abstract class TypeCast<T> {

    public static final Map<String, TypeCast<?>> castToType = Maps.newHashMap();

    static {
        castToType.put("string", new TypeCast<String>() {

            @Override
            public String cast(Object val) {
                return val == null ? null : String.valueOf(val);
            }
        });
        castToType.put("int", new TypeCast<Integer>() {

            @Override
            public Integer cast(Object val) {
                if (val == null) {
                    return 0;
                }
                if (val instanceof Integer) {
                    return (Integer) val;
                } else {
                    return Integer.parseInt(String.valueOf(val));
                }
            }
        });
    }

    public abstract T cast(Object val);

    public static TypeCast<?> getTypeCast(String type) {
        TypeCast<?> cast = castToType.get(type);
        if (cast == null) {
            throw new IllegalStateException("type:" + type + " relevant type case have not been defined");
        }
        return cast;
    }
}
