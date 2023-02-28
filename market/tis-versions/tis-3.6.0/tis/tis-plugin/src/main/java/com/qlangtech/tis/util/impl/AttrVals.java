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

package com.qlangtech.tis.util.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.trigger.util.JsonUtil;
import com.qlangtech.tis.util.AttrValMap;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-12 21:55
 **/
public class AttrVals implements AttrValMap.IAttrVals {
    protected final Map<String, JSON> /*** attrName*/
            attrValMap;

    public AttrVals(Map<String, JSON> attrValMap) {
        this.attrValMap = attrValMap;
    }

    @Override
    public Map<String, JSONObject> asRootFormVals() {
        return this.attrValMap.entrySet()
                .stream().collect(Collectors.toMap((e) -> e.getKey()
                        , (e) -> {
                            JSON j = e.getValue();
                            if (!(j instanceof JSONObject)) {
                                throw new IllegalStateException("type must be a object:\n" + JsonUtil.toString(j));
                            }
                            return (JSONObject) e.getValue();
                        }));
    }

    /**
     * 为了子表单可以同时支持多个descriptor的item提交
     * <pre>
     * vals:{
     *  tableName1:[
     *    {
     *      impl:"impl1"
     *      vals:{
     *         k1:v1,k2:v2
     *      }
     *    },
     *    {
     *      impl:"impl2"
     *      vals:{
     *         k1:v1,k2:v2
     *      }
     *    }
     *  ]
     * }
     *
     * </pre>
     *
     * @return
     */
    @Override
    public Map<String, JSONArray> asSubFormDetails() {
        return this.attrValMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        (e) -> e.getKey()
                        , (e) -> {
                            JSON j = e.getValue();
                            if (!(j instanceof JSONArray)) {
                                throw new IllegalStateException("type must be a array:\n" + JsonUtil.toString(j));
                            }
                            return (JSONArray) e.getValue();
                        }));
    }

    @Override
    public int size() {
        return this.attrValMap.size();
    }
}
