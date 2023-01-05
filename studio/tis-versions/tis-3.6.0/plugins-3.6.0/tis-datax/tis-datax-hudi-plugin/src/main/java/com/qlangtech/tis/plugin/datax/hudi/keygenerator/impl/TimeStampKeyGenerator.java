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

package com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
import com.qlangtech.plugins.org.apache.hudi.keygen.TimestampBasedAvroKeyGenerator;
import com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorType;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.ValidatorCommons;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.HudiKeyGenerator;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-07 15:55
 * TimestampBasedAvroKeyGenerator
 **/
public class TimeStampKeyGenerator extends HudiKeyGenerator {
    private static final KeyGeneratorType genType = KeyGeneratorType.TIMESTAMP;

    private static final Logger logger = LoggerFactory.getLogger(TimeStampKeyGenerator.class);

    public static final String KEY_TIMESTAMP_TYPE = "timestampType";
    public static final String KEY_INPUT_DATE_FORMAT = "inputDateformat";
    public static final String KEY_OUTPUT_DATE_FORMAT = "outputDateformat";

    public KeyGeneratorType getKeyGeneratorType() {
        return KeyGeneratorType.TIMESTAMP;
    }

    @Override
    public List<String> getRecordFields() {
        return Collections.singletonList(this.recordField);
    }

    @Override
    public List<String> getPartitionPathFields() {
        return Collections.singletonList(this.partitionPathField);
    }


    //    // One value from TimestampType above
//    public static final String TIMESTAMP_TYPE_FIELD_PROP = "hoodie.deltastreamer.keygen.timebased.timestamp.type";
    @FormField(ordinal = 6, type = FormFieldType.ENUM, validate = {Validator.require})
    public String timestampType;
//    public static final String INPUT_TIME_UNIT =
//            "hoodie.deltastreamer.keygen.timebased.timestamp.scalar.time.unit";
//    //This prop can now accept list of input date formats.
//    public static final String TIMESTAMP_INPUT_DATE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.input.dateformat";

    // format 可以填写多个，并且用逗号分割
    @FormField(ordinal = 7, type = FormFieldType.INPUTTEXT, advance = true, validate = {})
    public String inputDateformat;

    //    public static final String TIMESTAMP_INPUT_DATE_FORMAT_LIST_DELIMITER_REGEX_PROP = "hoodie.deltastreamer.keygen.timebased.input.dateformat.list.delimiter.regex";
//    public static final String TIMESTAMP_INPUT_TIMEZONE_FORMAT_PROP = "hoodie.deltastreamer.keygen.timebased.input.timezone";
//    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String inputTimezone;

    //    public static final String TIMESTAMP_OUTPUT_DATE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.output.dateformat";
    @FormField(ordinal = 8, type = FormFieldType.INPUTTEXT, advance = true, validate = {})
    public String outputDateformat;
    //    //still keeping this prop for backward compatibility so that functionality for existing users does not break.
//    public static final String TIMESTAMP_TIMEZONE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.timezone";
    @FormField(ordinal = 9, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public String timezone;
//    public static final String TIMESTAMP_OUTPUT_TIMEZONE_FORMAT_PROP = "hoodie.deltastreamer.keygen.timebased.output.timezone";

    //    static final String DATE_TIME_PARSER_PROP = "hoodie.deltastreamer.keygen.datetime.parser.class";
//    @Override
//    public void setProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {
//        super.setProps(props, hudiWriter);
//
//    }

    @Override
    protected void setKeyGenProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {
        props.setProperty(TimestampBasedAvroKeyGenerator.Config.TIMESTAMP_TYPE_FIELD_PROP, this.timestampType);
        props.setProperty(TimestampBasedAvroKeyGenerator.Config.TIMESTAMP_INPUT_DATE_FORMAT_PROP, this.inputDateformat);
        props.setProperty(TimestampBasedAvroKeyGenerator.Config.TIMESTAMP_OUTPUT_DATE_FORMAT_PROP, this.outputDateformat);
        props.setProperty(TimestampBasedAvroKeyGenerator.Config.TIMESTAMP_TIMEZONE_FORMAT_PROP, this.timezone);
    }

    //    @Override
//    public void setProps(IPropertiesBuilder props) {
//
//    }

    private static boolean validateForm(IControlMsgHandler msgHandler, Context context, TimeStampKeyGenerator keyGenerator) {
        TimestampBasedAvroKeyGenerator.TimestampType timestampType
                = TimestampBasedAvroKeyGenerator.TimestampType.valueOf(keyGenerator.timestampType);
        if (timestampType == TimestampBasedAvroKeyGenerator.TimestampType.DATE_STRING) {
            if (StringUtils.isEmpty(keyGenerator.inputDateformat)) {
                msgHandler.addFieldError(context, KEY_INPUT_DATE_FORMAT, ValidatorCommons.MSG_EMPTY_INPUT_ERROR);
                return false;
            }
        }

        if (StringUtils.isNotEmpty(keyGenerator.inputDateformat)) {
            try {
                DateTimeFormatter.ofPattern(keyGenerator.inputDateformat);
            } catch (Throwable e) {
                logger.warn("field:" + KEY_INPUT_DATE_FORMAT, e);
                msgHandler.addFieldError(context, KEY_INPUT_DATE_FORMAT, "日期格式有误");
                return false;
            }
        }

        if (StringUtils.isNotEmpty(keyGenerator.outputDateformat)) {
            try {
                DateTimeFormatter.ofPattern(keyGenerator.outputDateformat);
            } catch (Throwable e) {
                logger.warn("field:" + KEY_OUTPUT_DATE_FORMAT, e);
                msgHandler.addFieldError(context, KEY_OUTPUT_DATE_FORMAT, "日期格式有误");
                return false;
            }
        }


        return true;
    }

    @TISExtension
    public static class DefaultDescriptor extends BasicHudiKeyGeneratorDescriptor {
        public DefaultDescriptor() {
            super();
            addFieldDesc(this);
        }

        private static void addFieldDesc(Descriptor desc) {
            List<Option> timestampTypes
                    = Arrays.stream(TimestampBasedAvroKeyGenerator.TimestampType.values()).map((e) -> new Option(e.name())).collect(Collectors.toList());
            desc.addFieldDescriptor(KEY_TIMESTAMP_TYPE
                    , TimestampBasedAvroKeyGenerator.TimestampType.EPOCHMILLISECONDS.name(), "时间字段类型", Optional.of(timestampTypes));

            List<Option> timeZones = Arrays.stream(TimeZone.getAvailableIDs()).map((zid) -> new Option(zid)).collect(Collectors.toList());
            desc.addFieldDescriptor("timezone", TimeZone.getDefault().getID(), "格式化时间时区", Optional.of(timeZones));
        }

        @Override
        protected boolean verify(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            TimeStampKeyGenerator generator
                    = (TimeStampKeyGenerator) postFormVals.newInstance(this, msgHandler);

            if (!validateForm(msgHandler, context, generator)) {
                return false;
            }

            return super.verify(msgHandler, context, postFormVals);
        }

        @Override
        public String getDisplayName() {
            return genType.name();
        }
    }
}
