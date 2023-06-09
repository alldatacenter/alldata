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

package com.qlangtech.tis.plugin.datax.hudi.partition;

import com.alibaba.citrus.turbine.Context;
import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;

import java.util.List;
import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-26 14:31
 **/
public abstract class BaseSlashEncodedTimeUnitPartition extends HudiTablePartition {


//    @FormField(ordinal = 2, validate = {Validator.require})
//    public HudiKeyGenerator keyGenerator;


//    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
//    public String timestampType;
//    public static final String INPUT_TIME_UNIT =
//            "hoodie.deltastreamer.keygen.timebased.timestamp.scalar.time.unit";
//    //This prop can now accept list of input date formats.
//    public static final String TIMESTAMP_INPUT_DATE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.input.dateformat";

    // format 可以填写多个，并且用逗号分割
//    @FormField(ordinal = 3, type = FormFieldType.INPUTTEXT, advance = true, validate = {})
//    public String inputDateformat;

    //    public static final String TIMESTAMP_INPUT_DATE_FORMAT_LIST_DELIMITER_REGEX_PROP = "hoodie.deltastreamer.keygen.timebased.input.dateformat.list.delimiter.regex";
//    public static final String TIMESTAMP_INPUT_TIMEZONE_FORMAT_PROP = "hoodie.deltastreamer.keygen.timebased.input.timezone";
//    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String inputTimezone;

    //    public static final String TIMESTAMP_OUTPUT_DATE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.output.dateformat";
//    @FormField(ordinal = 4, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
//    public String outputDateformat;
    //    //still keeping this prop for backward compatibility so that functionality for existing users does not break.
//    public static final String TIMESTAMP_TIMEZONE_FORMAT_PROP =
//            "hoodie.deltastreamer.keygen.timebased.timezone";
//    @FormField(ordinal = 5, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
//    public String timezone;


    @Override
    public void setExtraProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {

        //props.setProperty(IPropertiesBuilder.KEY_HOODIE_PARTITIONPATH_FIELD, this.keyGenerator.getLiteriaPartitionPathFields());
        setHiveSyncPartitionProps(props, hudiWriter, getPartitionExtractorClass());
//        if (keyGenerator == null) {
//            throw new IllegalStateException("keyGenerator can not be null");
//        }

        // HudiTimestampBasedKeyGenerator keyGenerator = getHudiTimestampBasedKeyGenerator();
        //  this.setKeyGeneratorType(props, keyGenerator.getKeyGeneratorType().name());
        // keyGenerator.setProps(props);
    }

//    @Override
//    protected String getWriteKeyGeneratorType() {
//        return getHudiTimestampBasedKeyGenerator().getKeyGeneratorType().name();
//    }

    protected abstract String getPartitionExtractorClass();

//    private HudiTimestampBasedKeyGenerator getHudiTimestampBasedKeyGenerator() {
//        HudiTimestampBasedKeyGenerator keyGenerator = new HudiTimestampBasedKeyGenerator();
//        keyGenerator.timestampType = this.timestampType;
//        keyGenerator.outputDateformat = getKeyGeneratorOutputDateformat();
//        keyGenerator.timezone = this.timezone;
//        keyGenerator.inputDateformat = this.inputDateformat;
//        return keyGenerator;
//    }


    protected abstract String getKeyGeneratorOutputDateformat();

    @Override
    public void addPartitionsOnSQLDDL(List<String> pts, CreateTableSqlBuilder createTableSqlBuilder) {
        appendPartitionsOnSQLDDL(pts, createTableSqlBuilder);
    }


    protected static abstract class BaseDescriptor extends Descriptor<HudiTablePartition> {
        public BaseDescriptor() {
            super();
            // HudiTimestampBasedKeyGenerator.addFieldDesc(this);
        }

        @Override
        public final PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {
            return super.getPluginFormPropertyTypes(Optional.empty());
        }

        @Override
        protected final boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

//            ParseDescribable<Describable> i = this.newInstance((IPluginContext) msgHandler, postFormVals.rawFormData, Optional.empty());
//            BaseSlashEncodedTimeUnitPartition dayPartition = i.getInstance();
//            HudiTimestampBasedKeyGenerator timestampBasedKeyGenerator = dayPartition.getHudiTimestampBasedKeyGenerator();
//            return HudiTimestampBasedKeyGenerator.validateForm(msgHandler, context, timestampBasedKeyGenerator);
            return true;
        }

    }

}
