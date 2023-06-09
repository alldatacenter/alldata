package com.qlangtech.tis.datax;

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

import com.qlangtech.tis.plugin.ds.ISelectedTab;
import com.qlangtech.tis.pubhook.common.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-12 09:20
 **/
public class TableAliasMapper {

    public static final TableAliasMapper Null = new NULL();

    private static class NULL extends TableAliasMapper implements Nullable {
        public NULL() {
            super(Collections.emptyMap());
        }

        @Override
        public TableAlias get(String name) {
            //return super.get(name);
            return new TableAlias(name);
        }

        @Override
        public TableAlias getWithCheckNotNull(String name) {
            return this.get(name);
        }
    }

    /**
     * 表映射
     *
     * @return key: fromTabName
     */
    private final Map<String, TableAlias> mapper;

    public TableAliasMapper(Map<String, TableAlias> mapper) {
        this.mapper = mapper;
    }

    public boolean isNull() {
        return this.size() < 1 || this instanceof Nullable;
    }

    public void forEach(BiConsumer<String, TableAlias> action) {
        mapper.forEach(action);
    }

    public TableAlias get(String name) {
        return this.mapper.get(name);
    }

    public TableAlias getWithCheckNotNull(String name) {
        TableAlias alia = this.mapper.get(name);
        Objects.requireNonNull(alia
                , "tab:" + name + " relevant alias can not be null");
        return alia;
    }

    public TableAlias get(ISelectedTab tab) {
        return this.get(tab.getName());
    }

    public Optional<TableAlias> findFirst() {
        return mapper.values().stream().findFirst();
    }

    public boolean isSingle() {
        return this.mapper.size() == 1;
    }

    public Optional<IDataxProcessor.TableMap> getFirstTableMap() {
        Optional<IDataxProcessor.TableMap> first
                = this.mapper.values().stream().filter((t) -> t instanceof IDataxProcessor.TableMap)
                .map((t) -> (IDataxProcessor.TableMap) t).findFirst();
        return first;
    }


    public int size() {
        return this.mapper.size();
    }

    public String getFromTabDesc() {
        return this.mapper.keySet().stream().collect(Collectors.joining(","));
    }
}
