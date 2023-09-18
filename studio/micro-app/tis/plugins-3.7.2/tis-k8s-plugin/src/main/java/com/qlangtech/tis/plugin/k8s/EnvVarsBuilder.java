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

import com.google.common.collect.Lists;
import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.pubhook.common.RunEnvironment;
import io.kubernetes.client.openapi.models.V1EnvVar;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-05-06 16:32
 **/
public abstract class EnvVarsBuilder {
    private final String appName;

    public EnvVarsBuilder(String appName) {
        this.appName = appName;
    }

    public List<V1EnvVar> build() {
        List<V1EnvVar> envVars = Lists.newArrayList();
        V1EnvVar var = new V1EnvVar();
        var.setName("JVM_PROPERTY");
        var.setValue("-Ddata.dir=" + getDataDir() + " -D" + Config.KEY_JAVA_RUNTIME_PROP_ENV_PROPS + "=true " + getExtraSysProps());
        envVars.add(var);

        RunEnvironment runtime = RunEnvironment.getSysRuntime();
        var = new V1EnvVar();
        var.setName("JAVA_RUNTIME");
        var.setValue(runtime.getKeyName());
        envVars.add(var);
        var = new V1EnvVar();
        var.setName("APP_OPTIONS");
        var.setValue(getAppOptions());
        envVars.add(var);

        var = new V1EnvVar();
        var.setName("APP_NAME");
        var.setValue(appName);
        envVars.add(var);

        var = new V1EnvVar();
        var.setName(Config.KEY_RUNTIME);
        var.setValue(runtime.getKeyName());
        envVars.add(var);

        var = new V1EnvVar();
        var.setName(Config.KEY_ZK_HOST);
        var.setValue(processHost(Config.getZKHost()));
        envVars.add(var);

        var = new V1EnvVar();
        var.setName(Config.KEY_ASSEMBLE_HOST);
        var.setValue(processHost(Config.getAssembleHost()));
        envVars.add(var);

        var = new V1EnvVar();
        var.setName(Config.KEY_TIS_HOST);
        var.setValue(processHost(Config.getTisHost()));
        envVars.add(var);

        return envVars;

    }

    protected final String getDataDir() {
        return Config.DEFAULT_DATA_DIR;
        //return Config.getDataDir().getAbsolutePath();
    }

    protected String processHost(String address) {
        return address;
    }

//    public abstract String getAppOptions(String indexName, long timestamp) {
//        return indexName + " " + timestamp;
//    }

    public String getExtraSysProps() {
        return StringUtils.EMPTY;
    }

    public abstract String getAppOptions();
//   {
//        return indexName + " " + timestamp;
//    }
}
