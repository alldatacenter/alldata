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
package com.qlangtech.tis.runtime.pojo;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import com.qlangtech.tis.manage.biz.dal.pojo.UploadResource;
import com.qlangtech.tis.manage.common.PropteryGetter;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2013-1-16
 */
public class ResSyn {

    private final String name;

    private final UploadResource daily;

    private final UploadResource online;

    private final PropteryGetter getter;

    public UploadResource getDaily() {
        return daily;
    }

    public UploadResource getOnline() {
        return online;
    }

    public ResSyn(String name, UploadResource daily, UploadResource online, PropteryGetter getter) {
        super();
        Assert.assertNotNull("getter can not be null", getter);
        Assert.assertNotNull("daily resource " + getter.getFileName() + " can not be null", daily);
        this.name = name;
        this.daily = daily;
        this.online = online;
        this.getter = getter;
    }

    public PropteryGetter getGetter() {
        return getter;
    }

    public String getName() {
        return name;
    }

    public boolean isSame() {
        if (online == null) {
            return false;
        }
        return StringUtils.equals(daily.getMd5Code(), online.getMd5Code());
    }
}
