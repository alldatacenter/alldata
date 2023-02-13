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

package com.qlangtech.plugins.incr.flink.common;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.flink.IFlinkCluster;
import com.qlangtech.tis.config.flink.JobManagerAddress;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.lang.TisException;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.flink.client.program.rest.RestClusterClient;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.JobManagerOptions;
import org.apache.flink.configuration.RestOptions;
import org.apache.flink.runtime.client.JobStatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-23 12:10
 **/
@Public
public class FlinkCluster extends ParamsConfig implements IFlinkCluster {

    private static final Logger logger = LoggerFactory.getLogger(FlinkCluster.class);

    public static void main(String[] args) {
        System.out.println(IFlinkCluster.class.isAssignableFrom(FlinkCluster.class));
    }

    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require, Validator.identity})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.host, Validator.require})
    public String jobManagerAddress;

    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.identity, Validator.require})
    public String clusterId;

    @Override
    public JobManagerAddress getJobManagerAddress() {
        return JobManagerAddress.parse(this.jobManagerAddress);
    }

    /**
     * 校验是否可用
     *
     * @throws TisException
     */
    public void checkUseable() throws TisException {
        FlinkCluster cluster = this;
        try {
            try (RestClusterClient restClient = cluster.createFlinkRestClusterClient(Optional.of(1000l))) {
                // restClient.getClusterId();
                CompletableFuture<Collection<JobStatusMessage>> status = restClient.listJobs();
                Collection<JobStatusMessage> jobStatus = status.get();
            }
        } catch (Exception e) {
            throw new TisException("Please check link is valid:" + cluster.getJobManagerAddress().getURL(), e);
        }
    }

    @Override
    public String getClusterId() {
        return clusterId;
    }

    @Override
    public RestClusterClient createConfigInstance() {
        return createFlinkRestClusterClient(Optional.empty());
    }

    /**
     * @param connTimeout The maximum time in ms for the client to establish a TCP connection.
     * @return
     */
    public RestClusterClient createFlinkRestClusterClient(Optional<Long> connTimeout) {


//        String[] address = StringUtils.split(factory.jobManagerAddress, ":");
//        if (address.length != 2) {
//            throw new IllegalArgumentException("illegal jobManagerAddress:" + factory.jobManagerAddress);
//        }
        try {
            JobManagerAddress managerAddress = this.getJobManagerAddress();
            Configuration configuration = new Configuration();
            configuration.setString(JobManagerOptions.ADDRESS, managerAddress.host);
            configuration.setInteger(JobManagerOptions.PORT, managerAddress.port);
            configuration.setInteger(RestOptions.PORT, managerAddress.port);

            if (connTimeout.isPresent()) {
                configuration.setLong(RestOptions.CONNECTION_TIMEOUT, connTimeout.get());
                configuration.setInteger(RestOptions.RETRY_MAX_ATTEMPTS, 0);
                configuration.setLong(RestOptions.RETRY_DELAY, 0l);
            }
            return new RestClusterClient<>(configuration, this.clusterId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String identityValue() {
        return this.name;
    }


    @TISExtension
    public static class DefaultDescriptor extends Descriptor<ParamsConfig> {

        // private List<YarnConfig> installations;
        @Override
        public String getDisplayName() {
            return KEY_DISPLAY_NAME;
        }

        public DefaultDescriptor() {
            super();
            // this.load();
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {
            FlinkCluster flinkCluster = (FlinkCluster) postFormVals.newInstance(this, msgHandler);

//            ParseDescribable<Describable> paramsConfigParseDescribable = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
//            FlinkCluster flinkCluster = paramsConfigParseDescribable.getInstance();

            //try {
            flinkCluster.checkUseable();
//            } catch (TisException e) {
//               // msgHandler.addErrorMessage(context, e.getMessage());
//                return false;
//            }

//            JobManagerAddress jobManagerAddress = flinkCluster.getJobManagerAddress();
//
//            try {
//                final Integer serverStatus = HttpUtils.get(new URL(jobManagerAddress.getURL()), new ConfigFileContext.StreamProcess<Integer>() {
//                    @Override
//                    public Integer p(int status, InputStream stream, Map<String, List<String>> headerFields) {
//                        return status;
//                    }
//                });
//                if (serverStatus != HttpURLConnection.HTTP_OK) {
//                    msgHandler.addErrorMessage(context, "不可用的URL：" + jobManagerAddress.getURL() + ",responseStatus:" + serverStatus);
//                    return false;
//                }
//            } catch (Exception e) {
//                // throw new TisException(jobManagerAddress.getURL(), e);
//                msgHandler.addErrorMessage(context, "不可用的URL：" + jobManagerAddress.getURL() + "，" + e.getMessage());
//                logger.warn(jobManagerAddress.getURL(), e);
//                return false;
//            }


            return true;
        }
    }
}
