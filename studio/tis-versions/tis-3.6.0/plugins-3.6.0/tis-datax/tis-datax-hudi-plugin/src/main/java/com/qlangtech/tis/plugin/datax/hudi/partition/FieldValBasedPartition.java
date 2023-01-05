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

import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
import com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorOptions;
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.extension.PluginFormProperties;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.hudi.HudiSelectedTab;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;
import java.util.Optional;

/**
 * //@see org.apache.hudi.hive.MultiPartKeysValueExtractor
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-05 11:12
 **/
@Public
public class FieldValBasedPartition extends HudiTablePartition {

//    @FormField(ordinal = 1, type = FormFieldType.ENUM, validate = {Validator.require})
//    public String partitionPathField;

    @Override
    public void setExtraProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {

//        if (CollectionUtils.isEmpty(keyGenerator.getPartitionPathFields())) {
//            throw new IllegalStateException("partitionPathField can not be empty");
//        }

        // props.setProperty(IPropertiesBuilder.KEY_HOODIE_PARTITIONPATH_FIELD, keyGenerator.getLiteriaPartitionPathFields());
        //props.setProperty(KeyGeneratorOptions.PARTITIONPATH_FIELD_NAME.key(), keyGenerator.getLiteriaPartitionPathFields());
        setHiveSyncPartitionProps(props
                , hudiWriter
                , "org.apache.hudi.hive.MultiPartKeysValueExtractor");

    }

    @Override
    public void addPartitionsOnSQLDDL(List<String> pts, CreateTableSqlBuilder createTableSqlBuilder) {
        appendPartitionsOnSQLDDL(pts, createTableSqlBuilder);
    }

//    @Override
//    protected String getWriteKeyGeneratorType() {
//        return KeyGeneratorType.SIMPLE.name();
//    }

    public static List<Option> getPtCandidateFields() {
        return HudiSelectedTab.getPartitionKeys();
    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<HudiTablePartition> {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {
            return super.getPluginFormPropertyTypes(Optional.empty());
        }

        @Override
        public String getDisplayName() {
            return "fieldValBased";
        }
    }
}
