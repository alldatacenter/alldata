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

package com.qlangtech.plugins.incr.flink.cdc.mongdb;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.datax.IDataXPluginMeta;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;

import java.util.Optional;

/**
 * https://ververica.github.io/flink-cdc-connectors/master/content/connectors/mongodb-cdc.html
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-11-02 11:36
 **/
@Public
public class FlinkCDCMongoDBSourceFactory extends MQListenerFactory {

    @FormField(ordinal = 0, type = FormFieldType.INPUTTEXT)
    public String connectionOptions;

    @FormField(ordinal = 1, type = FormFieldType.ENUM)
    public String errorsTolerance;

    @FormField(ordinal = 2, type = FormFieldType.TEXTAREA)
    public String copyExistingPipeline;

    @FormField(ordinal = 3, type = FormFieldType.ENUM)
    public Boolean copyExisting;

    @FormField(ordinal = 4, type = FormFieldType.ENUM)
    public Boolean errorsLogEnable;

    @FormField(ordinal = 5, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer copyExistingMaxThreads;

    @FormField(ordinal = 6, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer copyExistingQueueSize;

    @FormField(ordinal = 7, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer pollMaxBatchSize;

    @FormField(ordinal = 8, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer pollAwaitTimeMillis;

    @FormField(ordinal = 9, type = FormFieldType.INT_NUMBER, validate = {Validator.integer})
    public Integer heartbeatIntervalMillis;

    private transient IConsumerHandle consumerHandle;

    @Override
    public IMQListener create() {
        FlinkCDCMongoDBSourceFunction sourceFunction = new FlinkCDCMongoDBSourceFunction(this);
        return sourceFunction;
    }

    @Override
    public void setConsumerHandle(IConsumerHandle consumerHandle) {
        this.consumerHandle = consumerHandle;
    }

    public IConsumerHandle getConsumerHander() {
        return this.consumerHandle;
    }

    @TISExtension()
    public static class DefaultDescriptor extends BaseDescriptor {
        @Override
        public String getDisplayName() {
            return "Flink-CDC-MongoDB";
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.FLINK_CDC;
        }
        @Override
        public IEndTypeGetter.EndType getEndType() {
            return IEndTypeGetter.EndType.MongoDB;
        }
    }
}
