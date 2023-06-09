///**
// *   Licensed to the Apache Software Foundation (ASF) under one
// *   or more contributor license agreements.  See the NOTICE file
// *   distributed with this work for additional information
// *   regarding copyright ownership.  The ASF licenses this file
// *   to you under the Apache License, Version 2.0 (the
// *   "License"); you may not use this file except in compliance
// *   with the License.  You may obtain a copy of the License at
// *
// *       http://www.apache.org/licenses/LICENSE-2.0
// *
// *   Unless required by applicable law or agreed to in writing, software
// *   distributed under the License is distributed on an "AS IS" BASIS,
// *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// *   See the License for the specific language governing permissions and
// *   limitations under the License.
// */
//package com.qlangtech.tis.fs;
//
//import com.qlangtech.tis.dump.INameWithPathGetter;
//import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
//import java.util.Set;
//
///**
// * FS持久文件映射到Table表相关执行内容 <br>
// * 等文件传输到FS之后,作操作
// * <ol>
// * <li>需要将FS中的文件和hive中的表进行映射</li>
// * <li>将历史的pt表删除</li>
// * <li>检查是否有添加字段，有则需要将历史表删除后新建</li>
// * </ol>
// *
// * @author 百岁（baisui@qlangtech.com）
// * @date 2020/04/13
// */
//public interface IFs2Table {
//
//    public ITISFileSystem getFileSystem();
//
//    /**
//     * 执行映射
//     *
//     * @param hiveTables
//     * @param timestamp
//     */
//    public void bindTables(Set<EntityName> hiveTables, String timestamp, ITaskContext context);
//
//    public void deleteHistoryFile(EntityName dumpTable, ITaskContext taskContext);
//
//    /**
//     * 删除特定timestamp下的文件，前一次用户已经导入了文件，后一次想立即重新导入一遍
//     *
//     * @param
//     * @throws Exception
//     */
//    public void deleteHistoryFile(EntityName dumpTable, ITaskContext taskContext, String timestamp);
//
//    /**
//     * 删除hive中的历史表
//     */
//    public void dropHistoryTable(EntityName dumpTable, ITaskContext taskContext);
//
//    //public String getJoinTableStorePath(INameWithPathGetter pathGetter);
//    // public void dropHistoryHiveTable(DumpTable dumpTable, Connection conn, PartitionFilter filter, int maxPartitionSave);
//}
