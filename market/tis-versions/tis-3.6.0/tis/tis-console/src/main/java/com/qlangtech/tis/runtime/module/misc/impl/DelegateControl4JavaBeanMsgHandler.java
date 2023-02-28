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

import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-09-11 15:27
 */
public class DelegateControl4JavaBeanMsgHandler extends BasicDelegateMsgHandler {

    private final Object bean;

    public DelegateControl4JavaBeanMsgHandler(IControlMsgHandler delegate, Object bean) {
        super(delegate);
        this.bean = bean;
    }

    @Override
    public String getString(String key) {
        try {
            return BeanUtils.getProperty(bean, key);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getString(String key, String dftVal) {
        String result = this.getString(key);
        return StringUtils.defaultIfEmpty(result, dftVal);
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(this.getString(key));
    }
}
