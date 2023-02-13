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
package com.qlangtech.tis;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class App {

    private static final String DPT_SEPARATE = "/";

    private String dpt;

    public String getBu() {
        String[] dpts = StringUtils.split(dpt, DPT_SEPARATE);
        for (int i = 0; i < dpts.length; i++) {
            if (StringUtils.isNotBlank(dpts[i])) {
                return dpts[i];
            }
        }
        throw new IllegalStateException("is not a illeal dpt name:" + dpt);
    }

    private String serviceName;

    private Integer appid;

    public String getDpt() {
        String[] dpts = StringUtils.split(dpt, DPT_SEPARATE);
        // if (dpts.length > 2) {
        // return dpts[dpts.length - 2] + DPT_SEPARATE + dpts[dpts.length - 1];
        // }
        // return StringUtils.substringAfterLast(dpt, DPT_SEPARATE);
        StringBuffer dptName = new StringBuffer();
        boolean hasFindBu = false;
        for (int i = 0; i < dpts.length; i++) {
            if (!hasFindBu && StringUtils.isNotBlank(dpts[i])) {
                hasFindBu = true;
                continue;
            }
            if (StringUtils.isNotBlank(dpts[i])) {
                dptName.append(dpts[i]);
                if ((i + 1) < dpts.length) {
                    dptName.append(DPT_SEPARATE);
                }
            }
        }
        return dptName.toString();
    }

    public Integer getAppid() {
        return appid;
    }

    public void setAppid(Integer appid) {
        this.appid = appid;
    }

    public void setDpt(String dpt) {
        this.dpt = dpt;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
