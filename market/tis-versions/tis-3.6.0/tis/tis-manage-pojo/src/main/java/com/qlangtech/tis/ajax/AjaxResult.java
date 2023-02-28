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
package com.qlangtech.tis.ajax;

import java.util.List;

/**
 * 当服务端发送一个新的Ajax请求，之后反馈的消息
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月20日
 */
public class AjaxResult<T> {

    private boolean success;

    private List<String> errormsg;

    private List<String> msg;

    private T bizresult;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public List<String> getErrormsg() {
        return errormsg;
    }

    public void setErrormsg(List<String> errormsg) {
        this.errormsg = errormsg;
    }

    public List<String> getMsg() {
        return msg;
    }

    public void setMsg(List<String> msg) {
        this.msg = msg;
    }

    public T getBizresult() {
        return bizresult;
    }

    public void setBizresult(T bizresult) {
        this.bizresult = bizresult;
    }
}
