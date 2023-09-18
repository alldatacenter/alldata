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

package com.qlangtech.tis.plugin.datax.common;

import com.qlangtech.tis.plugin.ds.DataSourceFactory;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-23 12:36
 **/
public abstract class BasicRdbmsContext<PLUGIN, DS extends DataSourceFactory> {
    protected final PLUGIN plugin;
    protected final DS dsFactory;

    private List<String> cols = new ArrayList<>();

    public BasicRdbmsContext(PLUGIN plugin, DS dsFactory) {
        this.plugin = plugin;
        this.dsFactory = dsFactory;
    }

    public void setCols(List<String> cols) {
        this.cols = cols;
    }

    public String getColsQuotes() {
        return getEntitiesWithQuotation(this.cols);
    }

    public String getCols() {
        return getEntitiesWith(this.cols, (val) -> val);
    }

    protected String colEscapeChar() {
        return "`";
    }

    protected String getEntitiesWithQuotation(List<String> cols) {
        return getEntitiesWith(cols, (val) -> "\"" + val + "\"");
    }

    private String getEntitiesWith(List<String> cols, Function<String, String> wrapper) {
        if (CollectionUtils.isEmpty(cols)) {
            throw new IllegalStateException("cols can not be empty");
        }
        return cols.stream().map(r -> (escapeEntity(r))).map(wrapper).collect(Collectors.joining(","));
    }

    protected String escapeEntity(String e) {
        return colEscapeChar() + e + colEscapeChar();
    }
}
