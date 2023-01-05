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
package com.qlangtech.tis.plugin.k8s;

import com.qlangtech.tis.TIS;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.k8s.IK8sContext;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.plugin.IdentityName;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-07 18:07
 */
@Public
public abstract class K8sImage implements Describable<K8sImage>, IdentityName {

    protected abstract String getK8SName();

    public abstract String getNamespace();

    public abstract String getImagePath();

    public abstract List<HostAlias> getHostAliases();

//    public static class HostAliases extends ArrayList<HostAlias> {
//    }

    /**
     * ParamsConfig.createConfigInstance(): io.kubernetes.client.openapi.ApiClient
     *
     * @param
     * @return
     */
    public <T> T createApiClient() {
        ParamsConfig cfg = (ParamsConfig) getK8SCfg();
        return cfg.createConfigInstance();
    }

    public IK8sContext getK8SCfg() {
        return ParamsConfig.getItem(this.getK8SName(), IK8sContext.KEY_DISPLAY_NAME);
    }

    @Override
    public final Descriptor<K8sImage> getDescriptor() {
        return TIS.get().getDescriptor(this.getClass());
    }
}
