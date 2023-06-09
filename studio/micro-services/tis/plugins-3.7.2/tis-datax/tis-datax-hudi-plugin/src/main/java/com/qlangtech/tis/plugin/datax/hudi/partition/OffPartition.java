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
import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;

import java.util.List;

/**
 * NonPartitionedExtractor
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-05 11:00
 **/
@Public
public class OffPartition extends HudiTablePartition {

    @Override
    public void setExtraProps(IPropertiesBuilder props, IDataXHudiWriter writer) {
        //  props.setProperty("hoodie.datasource.write.partitionpath.field", null);
        // HoodieWriteConfig.KEYGENERATOR_TYPE
        // @see HoodieSparkKeyGeneratorFactory l78
        // setKeyGeneratorType(props, "NON_PARTITION");
        setHiveSyncPartitionProps(props, null, "org.apache.hudi.hive.NonPartitionedExtractor");
    }

//    @Override
//    protected String getWriteKeyGeneratorType() {
//        return KeyGeneratorType.NON_PARTITION.name();
//    }

    @Override
    public void addPartitionsOnSQLDDL(List<String> pts, CreateTableSqlBuilder createTableSqlBuilder) {

    }

    @Override
    public boolean isSupportPartition() {
        return false;
    }

//    @TISExtension
//    public static class DefaultDescriptor extends Descriptor<HudiTablePartition> {
//        public DefaultDescriptor() {
//            super();
//        }
//
//        @Override
//        public PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {
//            return super.getPluginFormPropertyTypes(Optional.empty());
//        }
//
//        @Override
//        public String getDisplayName() {
//            return SWITCH_OFF;
//        }
//    }
}
