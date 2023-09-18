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
package com.qlangtech.tis.tair.imp;

import java.io.Serializable;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2014年9月4日下午3:25:52
 */
public interface ITSearchCache {

    public abstract boolean put(Serializable key, Serializable obj);

    /**
     * @param key
     * @param obj
     * @param expir 单位：秒
     * @return
     */
    public abstract boolean put(Serializable key, Serializable obj, int expir);

    public abstract boolean invalid(Serializable key);

    @SuppressWarnings("all")
    public <T> T getObj(Serializable key);
}
