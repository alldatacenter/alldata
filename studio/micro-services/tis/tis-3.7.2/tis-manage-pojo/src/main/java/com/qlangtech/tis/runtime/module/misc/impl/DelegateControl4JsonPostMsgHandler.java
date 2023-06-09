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
package com.qlangtech.tis.runtime.module.misc.impl;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang.StringUtils;

/**
 * 支持json post处理
 *
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-07-24 13:48
 */
public class DelegateControl4JsonPostMsgHandler extends BasicDelegateMsgHandler {

    private final JSONObject postData;

    public DelegateControl4JsonPostMsgHandler(IControlMsgHandler delegate, JSONObject postData) {
        super(delegate);
        this.postData = postData;
    }

    @Override
    public String getString(String key) {
        return postData.getString(key);
    }

    @Override
    public String getString(String key, String dftVal) {
        return StringUtils.defaultIfBlank(postData.getString(key), dftVal);
    }

    @Override
    public boolean getBoolean(String key) {
        return postData.getBoolean(key);
    }
}
