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

package com.qlangtech.tis.plugin.datax.hudi.keygenerator;

import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
import com.qlangtech.plugins.org.apache.hudi.common.config.ConfigProperty;
import com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorOptions;
import com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorType;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.util.OverwriteProps;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.datax.hudi.HudiSelectedTab;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.partition.HudiTablePartition;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-25 07:28
 * @see KeyGeneratorType
 * //@see org.apache.hudi.keygen.factory.HoodieAvroKeyGeneratorFactory
 * @see com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorOptions
 * @see com.qlangtech.plugins.org.apache.hudi.keygen.BaseKeyGenerator
 * case SIMPLE:
 * SimpleAvroKeyGenerator
 * case COMPLEX:
 * return new ComplexAvroKeyGenerator(props);
 * case TIMESTAMP:
 * return new TimestampBasedAvroKeyGenerator(props);
 * case CUSTOM:
 * return new CustomAvroKeyGenerator(props);
 * case NON_PARTITION:
 * return new NonpartitionedAvroKeyGenerator(props);
 * case GLOBAL_DELETE:
 * return new GlobalAvroDeleteKeyGenerator(props);
 **/
@Public
public abstract class HudiKeyGenerator implements Describable<HudiKeyGenerator> {

    /**
     * recordField 和recordFields使用过程中应该2选1
     */
    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
    public String recordField;

    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public List<String> recordFields;

    /**
     * partitionPathField 和 partitionPathFields 使用过程中应该2选一使用
     */
    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require})
    public String partitionPathField;

    @FormField(ordinal = 4, type = FormFieldType.ENUM, validate = {Validator.require})
    public List<String> partitionPathFields;

    @FormField(ordinal = 5, validate = {Validator.require})
    public HudiTablePartition partition;

    @FormField(ordinal = 20, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean encodePartitionPath;

    @FormField(ordinal = 21, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean hiveStylePartitioning;

    @FormField(ordinal = 22, type = FormFieldType.ENUM, advance = true, validate = {Validator.require})
    public Boolean consistentLogicalTimestampEnabled;


    public static final String FIELD_ENCODE_PARTITION_PATH = "encodePartitionPath";

    public static final String FIELD_HIVE_STYLE_PARTITIONING = "hiveStylePartitioning";

    public static final String FIELD_consistentLogicalTimestampEnabled = "consistentLogicalTimestampEnabled";


    public abstract List<String> getRecordFields();

    public abstract List<String> getPartitionPathFields();


    public HudiTablePartition getPartition() {
        return this.partition;
    }

    public void setPartition(HudiTablePartition partition) {
        this.partition = partition;
    }

    public final String getLiteriaRecordFields() {
        if (CollectionUtils.isEmpty(getRecordFields())) {
            throw new IllegalStateException("recordField can not be empty");
        }
        return this.getRecordFields().stream().collect(Collectors.joining(","));
    }

    public final String getLiteriaPartitionPathFields() {
        if (CollectionUtils.isEmpty(getPartitionPathFields())) {
            throw new IllegalStateException("partitionPathField can not be empty");
        }
        return this.getPartitionPathFields().stream().collect(Collectors.joining(","));
    }


    public static List<Option> getPtCandidateFields() {
        return SelectedTab.getContextTableCols((cols) -> cols.stream()
                .filter((col) -> {
                    switch (col.getType().getCollapse()) {
                        // case STRING:
                        case INT:
                        case Long:
                        case Date:
                            return true;
                    }
                    return false;
                }));

    }

    /**
     * 主键候选字段
     *
     * @return
     */
    public static List<Option> getPrimaryKeys() {
        return SelectedTab.getContextTableCols((cols) -> cols.stream().filter((col) -> col.isPk()));
    }

    public static List<String> getDeftRecordKeys() {
        return getPrimaryKeys().stream().map((pk) -> String.valueOf(pk.getValue())).collect(Collectors.toList());
    }


    public abstract KeyGeneratorType getKeyGeneratorType();

    //  public abstract void setProps(IPropertiesBuilder props);

    protected abstract void setKeyGenProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter);

    //@Override
    public final void setProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {
        props.setProperty(IPropertiesBuilder.KEY_HOODIE_DATASOURCE_WRITE_KEYGENERATOR_TYPE, this.getKeyGeneratorType().name());

        List<String> psPathFields = getPartitionPathFields();
        if (CollectionUtils.isNotEmpty(psPathFields)) {
            props.setProperty(IPropertiesBuilder.KEY_HOODIE_PARTITIONPATH_FIELD, this.getLiteriaPartitionPathFields());
        }

        List<String> recordFields = getRecordFields();
        if (CollectionUtils.isEmpty(recordFields)) {
            throw new IllegalStateException("recordFields can not be empty");
        }
        props.setProperty(IPropertiesBuilder.RECORDKEY_FIELD_NAME.key(), this.getLiteriaRecordFields());

        this.setKeyGenProps(props, hudiWriter);
        Objects.requireNonNull(this.partition, "partition can not be null").setProps(props, hudiWriter);
    }


    protected static class BasicHudiKeyGeneratorDescriptor extends Descriptor<HudiKeyGenerator> {
        public BasicHudiKeyGeneratorDescriptor() {
            super();
            this.addFieldDescriptor(FIELD_ENCODE_PARTITION_PATH, KeyGeneratorOptions.URL_ENCODE_PARTITIONING, OverwriteProps.createBooleanEnums());
            this.addFieldDescriptor(FIELD_HIVE_STYLE_PARTITIONING, KeyGeneratorOptions.HIVE_STYLE_PARTITIONING_ENABLE, OverwriteProps.createBooleanEnums());
            this.addFieldDescriptor(FIELD_consistentLogicalTimestampEnabled, KeyGeneratorOptions.KEYGENERATOR_CONSISTENT_LOGICAL_TIMESTAMP_ENABLED, OverwriteProps.createBooleanEnums());
        }

        @Override
        public final PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {
            return super.getPluginFormPropertyTypes(Optional.empty());
        }

        protected void addFieldDescriptor(String fieldName, ConfigProperty<?> configOption, OverwriteProps overwriteProps) {

            String helper = configOption.doc();

            Object dftVal = overwriteProps.processDftVal(configOption.defaultValue());

            StringBuffer helperContent = new StringBuffer(helper);
            if (overwriteProps.appendHelper.isPresent()) {
                helperContent.append("\n\n").append(overwriteProps.appendHelper.get());
            }
            this.addFieldDescriptor(fieldName, dftVal, helperContent.toString(), overwriteProps.opts);
        }
    }
}
