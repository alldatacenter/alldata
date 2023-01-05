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
package com.alibaba.datax.plugin.writer.hudi;

//
//import com.qlangtech.tis.hdfs.test.HdfsFileSystemFactoryTestUtils;
//import org.apache.hudi.utilities.UtilHelpers;
////import org.apache.hudi.utilities.deltastreamer.HoodieDeltaStreamer;
////import org.apache.hudi.utilities.deltastreamer.SchedulerConfGenerator;
////import org.apache.spark.api.java.JavaSparkContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.util.Map;
//
///**
// * @author: 百岁（baisui@qlangtech.com）
// * @create: 2022-01-21 13:09
// **/
public class HudiWriter {
    //    //    public static void main(String[] args) {
////   //     HoodieDeltaStreamer;
////
//   //     HoodieDeltaStreamer.Config cfg = TestHelpers.makeConfig(tableBasePath, WriteOperationType.BULK_INSERT);
////    }
//    private static final Logger logger = LoggerFactory.getLogger(HudiWriter.class);
//
////    public static final String hdfs_propsFilePath = HdfsFileSystemFactoryTestUtils.DEFAULT_HDFS_ADDRESS
////            + "/user/admin/customer_order_relation-source.properties";
////    public static final String hdfs_source_schemaFilePath = HdfsFileSystemFactoryTestUtils.DEFAULT_HDFS_ADDRESS + "/user/admin/schema.avsc";
    public static final String targetTableName = "customer_order_relation";
    public final static long timestamp = 20220311135455l;
//
////    /**
////     * https://hudi.apache.org/docs/next/docker_demo
////     *
////     * @param args
////     * @throws Exception
////     */
////    public static void main(String[] args) throws Exception {
////
//////        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("io/netty/buffer/PooledByteBufAllocator.class");
//////        //  System.out.println(HudiWriter.class.getResource());
//////        while (resources.hasMoreElements()) {
//////            System.out.println(resources.nextElement());
//////        }
////
////        final HoodieDeltaStreamer.Config cfg = new HoodieDeltaStreamer.Config();
////        //https://blog.csdn.net/android_unity3d/article/details/38502173
////        cfg.sparkMaster = "spark://192.168.28.201:7077";
////        cfg.tableType = "COPY_ON_WRITE";
////        cfg.sourceClassName = "org.apache.hudi.utilities.sources.CsvDFSSource";
////        cfg.propsFilePath = hdfs_propsFilePath;
////        cfg.sourceOrderingField = "last_ver";
////        cfg.targetBasePath = "/user/hive/warehouse/" + targetTableName;
////        cfg.targetTableName = targetTableName;
////        // cfg.propsFilePath = "/var/demo/config/kafka-source.properties";
////        cfg.schemaProviderClassName = "org.apache.hudi.utilities.schema.FilebasedSchemaProvider";
////        Map<String, String> additionalSparkConfigs = SchedulerConfGenerator.getSparkSchedulingConfigs(cfg);
////        additionalSparkConfigs.put("spark.ui.enabled", String.valueOf(false));
//        JavaSparkContext jssc =
//                UtilHelpers.buildSparkContext("delta-streamer-" + cfg.targetTableName, cfg.sparkMaster, additionalSparkConfigs);
////
////        if (cfg.enableHiveSync) {
////            logger.warn("--enable-hive-sync will be deprecated in a future release; please use --enable-sync instead for Hive syncing");
////        }
////
////        try {
    //         new HoodieDeltaStreamer(cfg, jssc).sync();
////        } finally {
////            jssc.stop();
////        }
////    }
}
