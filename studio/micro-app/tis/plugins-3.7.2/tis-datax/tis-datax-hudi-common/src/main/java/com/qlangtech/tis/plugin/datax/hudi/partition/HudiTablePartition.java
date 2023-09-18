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
import com.qlangtech.tis.extension.Describable;
import com.qlangtech.tis.plugin.datax.CreateTableSqlBuilder;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.HudiKeyGenerator;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-05 10:58
 **/
@Public
public abstract class HudiTablePartition implements Describable<HudiTablePartition> {

//    protected transient HudiKeyGenerator keyGenerator;
//
//    public void setKeyGenerator(HudiKeyGenerator keyGenerator) {
//        this.keyGenerator = keyGenerator;
//    }


    /**
     * //@see org.apache.hudi.DataSourceUtils +buildHiveSyncConfig()
     *
     * @param props
     * @param hudiWriter
     * @param partition_extractor_class
     */
    protected static void setHiveSyncPartitionProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter, String partition_extractor_class) {
        props.setProperty(IPropertiesBuilder.KEY_HOODIE_DATASOURCE_HIVE_SYNC_PARTITION_FIELDS, hudiWriter == null ? null : hudiWriter.getPartitionedBy());
        // "org.apache.hudi.hive.MultiPartKeysValueExtractor";
        // partition 分区值抽取类
        props.setProperty(IPropertiesBuilder.KEY_HOODIE_DATASOURCE_HIVE_SYNC_PARTITION_EXTRACTOR_CLASS, partition_extractor_class);
    }

    protected static void appendPartitionsOnSQLDDL(List<String> pts, CreateTableSqlBuilder createTableSqlBuilder) {
        createTableSqlBuilder.script.appendLine("\t,");
        createTableSqlBuilder.appendColName(pts.stream().collect(Collectors.joining(",")));
        createTableSqlBuilder.script
                .append("VARCHAR(30)")
                .returnLine();
    }

//    /**
//     * HoodieWriteConfig.KEYGENERATOR_TYPE
//     * // @see HoodieSparkKeyGeneratorFactory l78
//     *
//     * @param props
//     * @param type
//     */
//    private final void setKeyGeneratorType(IPropertiesBuilder props, String type) {
//        // HoodieWriteConfig.KEYGENERATOR_TYPE
//        props.setProperty(IPropertiesBuilder.KEY_HOODIE_DATASOURCE_WRITE_KEYGENERATOR_TYPE, type);
//    }


    public final void setProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {
        if (StringUtils.isEmpty(hudiWriter.getPartitionedBy())) {
            throw new IllegalStateException("hudiWriter.partitionedBy can not be empty");
        }
//        if (keyGenerator == null) {
//            throw new IllegalStateException("keyGenerator can not be null");
//        }
//        props.setProperty(IPropertiesBuilder.KEY_HOODIE_DATASOURCE_WRITE_KEYGENERATOR_TYPE, keyGenerator.getKeyGeneratorType().name());
        this.setExtraProps(props, hudiWriter);
    }

    public abstract void setExtraProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter);

    // protected abstract String getWriteKeyGeneratorType();


    public boolean isSupportPartition() {
        return true;
    }

    public abstract void addPartitionsOnSQLDDL(List<String> pts, CreateTableSqlBuilder createTableSqlBuilder);
}
