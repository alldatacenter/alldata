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

package com.qlangtech.tis.plugin.datax.hudi;

import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.writer.hdfswriter.HdfsColMeta;
import com.alibaba.datax.plugin.writer.hudi.HudiConfig;
import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.config.spark.ISparkConnGetter;
import com.qlangtech.tis.config.yarn.IYarnConfig;
import com.qlangtech.tis.datax.IDataxProcessor;
import com.qlangtech.tis.extension.impl.IOUtils;
import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
import com.qlangtech.tis.offline.FileSystemFactory;
import com.qlangtech.tis.plugin.KeyedPluginStore;
import com.qlangtech.tis.plugin.common.WriterTemplate;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl.SimpleKeyGenerator;
import com.qlangtech.tis.plugin.datax.hudi.partition.HudiTablePartition;
import com.qlangtech.tis.plugin.datax.hudi.partition.OffPartition;
import com.qlangtech.tis.plugin.datax.hudi.spark.SparkSubmitParams;
import com.qlangtech.tis.plugin.ds.CMeta;
import com.qlangtech.tis.plugin.ds.IColMetaGetter;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-03-25 09:31
 **/
public class HudiTest {
    public static final String hudi_datax_writer_assert_without_optional = "hudi-datax-writer-assert-without-optional.json";
    public static final String cfgPathParameter = "parameter";
    public final DataXHudiWriter writer;
    public final IDataxProcessor.TableMap tableMap;
    public final HudiSelectedTab tab;

    public HudiTest(DataXHudiWriter writer, IDataxProcessor.TableMap tableMap, HudiSelectedTab tab) {
        this.writer = writer;
        this.tableMap = tableMap;
        this.tab = tab;
    }

    public static HudiTest createDataXWriter() {
        return createDataXWriter(Optional.empty());
    }

    public static HudiTest createDataXWriter(Optional<FileSystemFactory> fsFactory) {
        return createDataXWriter(fsFactory, new OffPartition());
    }

    public static HudiTest createDataXWriter(Optional<FileSystemFactory> fsFactory, HudiTablePartition partition) {

        DataXHudiWriter writer = createDataXHudiWriter(fsFactory);
        // HdfsColMeta
        // IColMetaGetter
        List<IColMetaGetter> colsMeta
                = HdfsColMeta.getColsMeta(Configuration.from(IOUtils.loadResourceFromClasspath(writer.getClass()
                , hudi_datax_writer_assert_without_optional)).getConfiguration(cfgPathParameter));
        HudiSelectedTab tab = new HudiSelectedTab() {
            @Override
            public List<CMeta> getCols() {
                return colsMeta.stream().map((cc) -> {
                    HdfsColMeta c = (HdfsColMeta) cc;
                    CMeta col = new CMeta();
                    col.setName(c.getName());
                    col.setPk(c.pk);
                    col.setType(c.type);
                    col.setNullable(c.nullable);
                    return col;
                }).collect(Collectors.toList());
            }
        };
        tab.name = WriterTemplate.TAB_customer_order_relation;

        SimpleKeyGenerator simpleKeyGenerator = new SimpleKeyGenerator();
        simpleKeyGenerator.setPartition(partition);
        simpleKeyGenerator.recordField = "customerregister_id";
        // simpleKeyGenerator.partitionPathField =
        tab.keyGenerator = simpleKeyGenerator;
        // tab.partition = partition;
        tab.sourceOrderingField = "last_ver";
        //tab.recordField =


        return new HudiTest(writer, WriterTemplate.createCustomer_order_relationTableMap(Optional.of(tab)), tab);
    }


    public static DataXHudiWriter createDataXHudiWriter(Optional<FileSystemFactory> fsFactory) {
        //ISparkConnGetter
//        final ISparkConnGetter sparkConnGetter = new ISparkConnGetter() {
//            @Override
//            public String getSparkMaster(File cfgDir) {
//                return "spark://sparkmaster:7077";
//            }
//
//            @Override
//            public String identityValue() {
//                return "default";
//            }
//        };

        final ISparkConnGetter sparkConnGetter = new ISparkConnGetter() {
            @Override
            public String getSparkMaster(File cfgDir) {

                File sparkHome = HudiConfig.getSparkHome();
                File sparkCfgDir = new File(sparkHome, "conf");

                com.qlangtech.tis.config.Utils.setHadoopConfig2Local(sparkCfgDir, IYarnConfig.FILE_NAME_YARN_SITE
                        , IOUtils.loadResourceFromClasspath(HudiTest.class, IYarnConfig.FILE_NAME_YARN_SITE));
                return IYarnConfig.KEY_DISPLAY_NAME;
            }

            @Override
            public String identityValue() {
                return "default";
            }
        };

//        sparkConnGetter.name = "default";
//        sparkConnGetter.master = "spark://sparkmaster:7077";

        DataXHudiWriter writer = new DataXHudiWriter() {
            @Override
            public Class<?> getOwnerClass() {
                return DataXHudiWriter.class;
            }

            @Override
            public IHiveConnGetter getHiveConnMeta() {
                return HdfsFileSystemFactoryTestUtils.createHiveConnGetter();
            }

            @Override
            public ISparkConnGetter getSparkConnGetter() {
                return sparkConnGetter;
            }

            @Override
            public FileSystemFactory getFs() {
                return fsFactory.isPresent() ? fsFactory.get() : HdfsFileSystemFactoryTestUtils.getFileSystemFactory();
            }
        };
        writer.template = DataXHudiWriter.getDftTemplate();
        writer.fsName = HdfsFileSystemFactoryTestUtils.FS_NAME;
        writer.setKey(new KeyedPluginStore.Key(null, HdfsFileSystemFactoryTestUtils.testDataXName.getName(), null));
        writer.tabType = HudiWriteTabType.COW.getValue();
        writer.batchOp = BatchOpMode.BULK_INSERT.getValue();
        writer.shuffleParallelism = 3;
        writer.partitionedBy = "pt";
        SparkSubmitParams sparkSubmitParams = new SparkSubmitParams();
        sparkSubmitParams.driverMemory = "1G";
        sparkSubmitParams.executorMemory = "2G";
        sparkSubmitParams.executorCores = 2;
        sparkSubmitParams.deployMode = "cluster";
        writer.sparkSubmitParam = sparkSubmitParams;


//        writer.batchByteSize = 3456;
//        writer.batchSize = 9527;
//        writer.dbName = dbName;
        writer.writeMode = "append";
        // writer.autoCreateTable = true;
//        writer.postSql = "drop table @table";
//        writer.preSql = "drop table @table";

        // writer.dataXName = HdfsFileSystemFactoryTestUtils.testDataXName.getName();
        //  writer.dbName = dbName;

//        HudiSelectedTab hudiTab = new HudiSelectedTab() {
//            @Override
//            public List<ColMeta> getCols() {
//                return WriterTemplate.createColMetas();
//            }
//        };
//        //hudiTab.partitionPathField = WriterTemplate.kind;
//        hudiTab.recordField = WriterTemplate.customerregisterId;
//        hudiTab.sourceOrderingField = WriterTemplate.lastVer;
//        hudiTab.setWhere("1=1");
//        hudiTab.name = WriterTemplate.TAB_customer_order_relation;
        return writer;
    }
}
