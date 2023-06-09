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

import com.google.common.collect.Lists;
import org.apache.flink.types.RowKind;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-29 15:26
 **/
public abstract class BasicRow {
    protected final RowKind kind;

    public BasicRow(RowKind kind) {
        this.kind = kind;
    }

    public List<String> getValsList(List<ColMeta> keys) {
        return getValsList(Optional.empty(), keys);
    }

    public List<String> getValsList(Optional<ExpectRowGetter> updateVal, List<ColMeta> keys) {
        // boolean updateRowGetterPresent = ;
        RowKind rowKind = updateVal.isPresent() ? updateVal.get().expectKind : this.kind;
        List<String> valsEnum = Lists.newArrayList(rowKind.shortString());
        for (ColMeta key : keys) {
            Object val = null;
            if (updateVal.isPresent() && updateVal.get().getUpdateVal) {
                val = getUpdateVal(key);
            }
            if (val == null) {
                val = getSerializeVal(key.getName());
            }
            valsEnum.add(key.getName() + ":" + val);

        }
        return valsEnum;
    }

    public abstract Object getSerializeVal(String key);

    protected Object getUpdateVal(ColMeta key) {
        throw new UnsupportedOperationException();
    }


    public abstract Object getObj(String key);
}
