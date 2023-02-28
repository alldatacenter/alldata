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

import com.qlangtech.tis.config.hive.IHiveConnGetter;
import com.qlangtech.tis.fs.ITISFileSystem;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-04-10 12:20
 **/
public interface IDataXHudiWriter {

    public String DATAX_NAME = "Hudi";

    public String KEY_FIELD_NAME_SPARK_CONN = "sparkConn";

    public String HUDI_FILESYSTEM_NAME = "hudi_hdfs";

    String getPartitionedBy();

    String getFsName();

    IHiveConnGetter getHiveConnMeta();

    ITISFileSystem getFileSystem();

//    com.qlangtech.tis.plugin.datax.hudi.BatchOpMode getHudiTableType();

    public HudiWriteTabType getHudiTableType();
}
