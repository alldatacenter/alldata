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
// */
package com.qlangtech.async.message.client.consumer;

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.async.message.client.consumer.IMQListener;
import com.qlangtech.tis.async.message.client.consumer.impl.AbstractAsyncMsgDeserialize;
import com.qlangtech.tis.async.message.client.consumer.impl.MQListenerFactory;
import com.qlangtech.tis.datax.IDataXPluginMeta;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IEndTypeGetter;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.Validator;
import org.apache.commons.lang.StringUtils;
import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.common.admin.ConsumeStats;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;

import java.util.Optional;

/**
 * 基于rockmq的消息监听器，插件实现
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
@Public
public class RocketMQListenerFactory extends MQListenerFactory {

    @FormField(validate = {Validator.require, Validator.identity}, ordinal = 2)
    public String consumeName;

    @FormField(validate = {Validator.require, Validator.identity}, ordinal = 0)
    public String mqTopic;

    @FormField(validate = {Validator.require, Validator.host}, ordinal = 3)
    public String namesrvAddr;

    @FormField(validate = {Validator.require}, ordinal = 1)
    public AbstractAsyncMsgDeserialize deserialize;

    public String getConsumeName() {
        return consumeName;
    }

    private final String consumerHandle = "default";

    public static void main(String[] args) {
        // System.out.println(spec_pattern.matcher("c_otter_binlogorder_solr").matches());
        //
        // System.out.println(host_pattern.matcher("10.1.21.148:9876").matches());
    }

    @Override
    public IMQConsumerStatus createConsumerStatus() {
        //https://github.com/apache/rocketmq/blob/9f95a972e10e0681bc3f2d00e9957aa212e897b5/tools/src/main/java/org/apache/rocketmq/tools/command/consumer/ConsumerProgressSubCommand.java#L48
//        try {
//            if (StringUtils.isBlank(this.namesrvAddr)) {
//                throw new IllegalStateException("prop 'namesrvAddr' can not be blank.");
//            }
//            System.setProperty(MixAll.NAMESRV_ADDR_PROPERTY, this.namesrvAddr);
//            // RPCHook rpcHook = null  ;//AclUtils.getAclRPCHook(rocketmqHome + MixAll.ACL_CONF_TOOLS_FILE);
//
//            DefaultMQAdminExt defaultMQAdminExt = new DefaultMQAdminExt();
//            defaultMQAdminExt.setInstanceName(Long.toString(System.currentTimeMillis()));
//
//            defaultMQAdminExt.start();
//
//            ConsumeStats consumeStats = defaultMQAdminExt.examineConsumeStats(this.consumeName, this.mqTopic);
//            return new RocketMQConsumerStatus(consumeStats, defaultMQAdminExt);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        return null;
    }

    @Override
    public IMQListener create() {
        if (StringUtils.isEmpty(this.consumeName)) {
            throw new IllegalStateException("prop consumeName can not be null");
        }
        if (StringUtils.isEmpty(this.mqTopic)) {
            throw new IllegalStateException("prop mqTopic can not be null");
        }
        if (StringUtils.isEmpty(this.namesrvAddr)) {
            throw new IllegalStateException("prop namesrvAddr can not be null");
        }
        if (deserialize == null) {
            throw new IllegalStateException("prop deserialize can not be null");
        }
        ConsumerListenerForRm rmListener = new ConsumerListenerForRm();
        rmListener.setConsumerGroup(this.consumeName);
        rmListener.setTopic(this.mqTopic);
        rmListener.setNamesrvAddr(this.namesrvAddr);
        rmListener.setDeserialize(deserialize);
        return rmListener;
    }

    public void setConsumeName(String consumeName) {
        this.consumeName = consumeName;
    }

    public String getMqTopic() {
        return mqTopic;
    }

    public void setMqTopic(String mqTopic) {
        this.mqTopic = mqTopic;
    }

    public String getNamesrvAddr() {
        return namesrvAddr;
    }

    public void setNamesrvAddr(String namesrvAddr) {
        this.namesrvAddr = namesrvAddr;
    }

    public AbstractAsyncMsgDeserialize getDeserialize() {
        return deserialize;
    }

    public void setDeserialize(AbstractAsyncMsgDeserialize deserialize) {
        this.deserialize = deserialize;
    }

    @TISExtension(ordinal = 0)
    public static class DefaultDescriptor extends BaseDescriptor {

        @Override
        public String getDisplayName() {
            return "RocketMq";
        }
        @Override
        public PluginVender getVender() {
            return PluginVender.TIS;
        }
        @Override
        public IEndTypeGetter.EndType getEndType() {
            return IEndTypeGetter.EndType.RocketMQ;
        }

        @Override
        public Optional<IEndTypeGetter.EndType> getTargetType() {
            return Optional.empty();
        }

        // private static final Pattern spec_pattern = Pattern.compile("[\\da-z_]+");

        // private static final Pattern host_pattern = Pattern.compile("[\\da-z]{1}[\\da-z.:]+");

        //  public static final String MSG_HOST_IP_ERROR = "必须由IP、HOST及端口号组成";

        // public static final String MSG_DIGITAL_Alpha_CHARACTER_ERROR = "必须由数字、小写字母、下划线组成";

//        public boolean validateNamesrvAddr(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            Matcher matcher = host_pattern.matcher(value);
//            if (!matcher.matches()) {
//                msgHandler.addFieldError(context, fieldName, MSG_HOST_IP_ERROR);
//                return false;
//            }
//            return true;
//        }

//        public boolean validateMqTopic(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            return validateConsumeName(msgHandler, context, fieldName, value);
//        }
//
//        public boolean validateConsumeName(IFieldErrorHandler msgHandler, Context context, String fieldName, String value) {
//            Matcher matcher = spec_pattern.matcher(value);
//            if (!matcher.matches()) {
//                msgHandler.addFieldError(context, fieldName, MSG_DIGITAL_Alpha_CHARACTER_ERROR);
//                return false;
//            }
//            return true;
//        }
    }
}
