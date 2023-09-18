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

package com.qlangtech.plugins.incr.flink.cdc.postgresql;

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


import java.util.Objects;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-09-27 15:15
 **/
@Public
public class FlinkCDCPostreSQLSourceFactory extends MQListenerFactory {
    private transient IConsumerHandle consumerHandle;

//    @FormField(ordinal = 0, type = FormFieldType.ENUM, validate = {Validator.require})
//    public String startupOptions;

//    StartupOptions getStartupOptions() {
//        switch (startupOptions) {
//            case "latest":
//                return StartupOptions.latest();
//            case "earliest":
//                return StartupOptions.earliest();
//            case "initial":
//                return StartupOptions.initial();
//            default:
//                throw new IllegalStateException("illegal startupOptions:" + startupOptions);
//        }
//    }

    @Override
    public IMQListener create() {
        FlinkCDCPostgreSQLSourceFunction sourceFunctionCreator = new FlinkCDCPostgreSQLSourceFunction(this);
        return sourceFunctionCreator;
    }

    public IConsumerHandle getConsumerHander() {
        Objects.requireNonNull(this.consumerHandle, "prop consumerHandle can not be null");
        return this.consumerHandle;
    }

    @Override
    public void setConsumerHandle(IConsumerHandle consumerHandle) {
        this.consumerHandle = consumerHandle;
    }

    @TISExtension()
    public static class DefaultDescriptor extends BaseDescriptor {
        @Override
        public String getDisplayName() {
            return "Flink-CDC-PostgreSQL";
        }

        @Override
        public PluginVender getVender() {
            return PluginVender.FLINK_CDC;
        }
        @Override
        public IEndTypeGetter.EndType getEndType() {
            return IEndTypeGetter.EndType.Postgres;
        }

    }
}
