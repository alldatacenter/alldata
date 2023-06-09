package com.qlangtech.tis.datax;

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

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-02-23 09:59
 **/
public interface IDataXTaskRelevant {


    /**
     * 保证一个批次执行的DataX任务的每个子任务都有一个唯一的序列号，例如在ODPS数据导入的场景中
     * ，MySQL中有多个分库的表需要导入到ODPS中采用pt+pmod（该值通过唯一序列号）的分区组合来避免不同分库数据导入相同分区的冲突
     *
     * @return
     */
    public int getTaskSerializeNum();

    public String getFormatTime(TimeFormat format);


}
