/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.qlangtech.plugins.incr.flink.cdc;

import com.google.common.collect.Maps;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-15 15:12
 **/
public class RowVals<T extends Callable<Object>> {
    private final Map<String, T> vals;

    protected RowVals() {
        this(Maps.newTreeMap());
    }

    protected RowVals(Map<String, T> vals) {
        this.vals = vals;
    }

    public List<Map.Entry<String, T>> getCols() {
        return new ArrayList<>(vals.entrySet());
    }

    public Integer getInt(String key) {
        try {
            return (Integer) vals.get(key).call();
        } catch (Exception e) {
            throw new RuntimeException("key:" + key, e);
        }
    }

    public String getString(String key) {
        try {
            return (String) vals.get(key).call();
        } catch (Exception e) {
            throw new RuntimeException("key:" + key, e);
        }
    }

    public BigDecimal getBigDecimal(String key) {
        try {
            return (BigDecimal) vals.get(key).call();
        } catch (Exception e) {
            throw new RuntimeException("key:" + key, e);
        }
    }

    public InputStream getInputStream(String key) {
        try {
            return (InputStream) vals.get(key).call();
        } catch (Exception e) {
            throw new RuntimeException("key:" + key, e);
        }
    }

    public Object getObj(String key) {
        try {
            T t = vals.get(key);
            if (t == null) {
                return null;
            }
            return t.call();
        } catch (Exception e) {
            throw new RuntimeException("key:" + key, e);
        }
    }

    public T getV(String key) {
        return vals.get(key);
    }

    public boolean notNull(String key) {
        return vals.get(key) != null;
    }

    public boolean isEmpty() {
        return vals.isEmpty();
    }


    public void put(String key, T val) {
        this.vals.put(key, val);
    }

    @Override
    public String toString() {
        return "RowVals{" +
                "vals=" + vals +
                '}';
    }
}
