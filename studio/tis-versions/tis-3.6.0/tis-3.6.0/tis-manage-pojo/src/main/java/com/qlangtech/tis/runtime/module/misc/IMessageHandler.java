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
package com.qlangtech.tis.runtime.module.misc;

import com.alibaba.citrus.turbine.Context;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-12-24
 */
public interface IMessageHandler {

    String ACTION_MSG = "action_msg";

    String ACTION_BIZ_RESULT = "action_biz_result";

    String ACTION_ERROR_MSG = "action_error_msg";

    /**
     * 错误信息是否是显示在页面上，而不是消息提示框中
     */
    String ACTION_ERROR_PAGE_SHOW = "action_error_page_show";
    String TSEARCH_PACKAGE = com.qlangtech.tis.Package.class.getPackage().getName();// "com.qlangtech.tis";

    void errorsPageShow(final Context context);

    void addActionMessage(final Context context, String msg);

    void setBizResult(final Context context, Object result);

    /**
     * 添加错误信息
     *
     * @param context
     * @param msg
     */
    void addErrorMessage(final Context context, String msg);

    default String getRequestHeader(String key) {
        throw new UnsupportedOperationException("key:" + key);
    }
}
