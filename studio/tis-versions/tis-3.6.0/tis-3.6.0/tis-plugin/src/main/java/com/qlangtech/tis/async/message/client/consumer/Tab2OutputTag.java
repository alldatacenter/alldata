package com.qlangtech.tis.async.message.client.consumer;

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

import com.qlangtech.tis.datax.TableAlias;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-06 13:18
 **/
public class Tab2OutputTag<DTOStream> implements Serializable {

    private final Map<TableAlias, DTOStream> mapper;
    private transient Map<String, DTOStream> sinkMapper;
    private transient Map<String, DTOStream> sourceMapper;

    public Tab2OutputTag(Map<TableAlias, DTOStream> mapper) {
        this.mapper = mapper;
    }

    public DTOStream get(ISelectedTab tab) {
        return this.getSourceMapper().get(tab.getName());
    }

    public Set<Map.Entry<TableAlias, DTOStream>> entrySet() {
        return this.mapper.entrySet();
    }

    public Map<String, DTOStream> getSinkMapper() {
        if (sinkMapper == null) {
            sinkMapper = getMapper(false);
        }
        return sinkMapper;
    }

    private Map<String, DTOStream> getMapper(boolean from) {
        Set<Map.Entry<TableAlias, DTOStream>> entries = this.mapper.entrySet();
        return entries.stream().collect(Collectors.toMap((e) -> from ? e.getKey().getFrom() : e.getKey().getTo(), (e) -> e.getValue()));
    }

    public Map<String, DTOStream> getSourceMapper() {
        if (sourceMapper == null) {
            sourceMapper = getMapper(true);
        }
        return sourceMapper;
    }
}
