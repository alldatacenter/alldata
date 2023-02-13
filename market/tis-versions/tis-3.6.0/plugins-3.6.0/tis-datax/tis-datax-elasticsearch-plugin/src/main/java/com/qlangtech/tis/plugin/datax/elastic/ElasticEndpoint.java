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

package com.qlangtech.tis.plugin.datax.elastic;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.plugin.writer.elasticsearchwriter.ESClient;
import com.alibaba.datax.plugin.writer.elasticsearchwriter.ESInitialization;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.AuthToken;
import com.qlangtech.tis.plugin.HttpEndpoint;
import com.qlangtech.tis.plugin.aliyun.NoneToken;
import com.qlangtech.tis.plugin.aliyun.UsernamePassword;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import io.searchbox.client.JestResult;
import io.searchbox.cluster.Health;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-31 09:43
 **/
public class ElasticEndpoint extends HttpEndpoint {

    public static final String KEY_DISPLAY_NAME = "elasticToken";

    public final ESInitialization createESInitialization() {
        UsernamePassword auth = this.accept(new AuthToken.Visitor<UsernamePassword>() {
            @Override
            public UsernamePassword visit(NoneToken noneToken) {
                return new UsernamePassword();
            }

            @Override
            public UsernamePassword visit(UsernamePassword accessKey) {
                return accessKey;
            }
        });
        return (ESInitialization.create(this.getEndpoint(), auth.userName, auth.password,
                false,
                300000,
                false,
                false));
    }

    public final ESClient createESClient() {
        return new ESClient(createESInitialization());
    }

    public static List<? extends Descriptor> filter(List<? extends Descriptor> descs) {
        return descs.stream().filter((desc) -> {
            return desc instanceof UsernamePassword.DefaultDescriptor || desc instanceof NoneToken.DefaultDescriptor;
        }).collect(Collectors.toList());
    }

    @TISExtension()
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            ElasticEndpoint cfg = (ElasticEndpoint) postFormVals.newInstance(this, msgHandler);
            ESInitialization es = cfg.createESInitialization();
            // Health
            Health.Builder hbuild = new Health.Builder();
            hbuild.timeout(5);
            try {
                JestResult result = es.jestClient.execute(hbuild.build());
                if (!result.isSucceeded()) {
                    msgHandler.addErrorMessage(context, result.getErrorMessage());
                } else {
                    msgHandler.addActionMessage(context
                            , "cluster '" + result.getValue("cluster_name") + "' is working,status:'" + result.getValue("status") + "'");
                }
                return result.isSucceeded();
            } catch (IOException e) {
                msgHandler.addErrorMessage(context, e.getMessage());
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            return KEY_DISPLAY_NAME;
        }
    }

}
