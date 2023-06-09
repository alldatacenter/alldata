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
package com.qlangtech.tis.runtime.module.misc.impl;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @create: 2020-09-11 15:24
 */
public abstract class BasicDelegateMsgHandler implements IControlMsgHandler {

    private final IControlMsgHandler delegate;

    public BasicDelegateMsgHandler(IControlMsgHandler delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean validateBizLogic(BizLogic logicType, Context context, String fieldName, String value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final void addFieldError(Context context, String fieldName, String msg, Object... params) {
        delegate.addFieldError(context, fieldName, msg, params);
    }

    @Override
    public final void errorsPageShow(Context context) {
        delegate.errorsPageShow(context);
    }

    @Override
    public final void addActionMessage(Context context, String msg) {
        delegate.addActionMessage(context, msg);
    }

    @Override
    public final void setBizResult(Context context, Object result, boolean overwriteable) {
        delegate.setBizResult(context, result, overwriteable);
    }

    @Override
    public final void addErrorMessage(Context context, String msg) {
        delegate.addErrorMessage(context, msg);
    }
}
