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
package com.qlangtech.tis.plugin.ds;

import com.qlangtech.tis.offline.DbScope;
import com.qlangtech.tis.plugin.KeyedPluginStore;

import java.util.Objects;

/**
 * @author: baisui 百岁
 * @create: 2020-11-24 16:24
 */
public class DSKey extends KeyedPluginStore.Key<DataSourceFactory> {
    private final DbScope dbScope;

    public DSKey(String groupName, PostedDSProp dsProp, Class<DataSourceFactory> pluginClass) {
        this(groupName, dsProp.getDbType(), dsProp.getDbname().get(), pluginClass);
    }

    public DSKey(String groupName, DbScope dbScope, String keyVal, Class<DataSourceFactory> pluginClass) {
        super(groupName, keyVal, pluginClass);
        this.dbScope = dbScope;
    }

    @Override
    public String getSerializeFileName() {
        return super.getSerializeFileName() + dbScope.getDBType();
        //  return groupName + File.separator + keyVal + File.separator + pluginClass.getName() + dbScope.getDBType();
    }

    public boolean isFacadeType() {
        return this.dbScope == DbScope.FACADE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.keyVal, this.dbScope, pluginClass);
    }
}
