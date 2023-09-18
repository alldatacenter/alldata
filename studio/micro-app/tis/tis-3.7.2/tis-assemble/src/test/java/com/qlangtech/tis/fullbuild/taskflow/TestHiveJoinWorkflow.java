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
package com.qlangtech.tis.fullbuild.taskflow;

// import com.google.common.collect.Maps;
import junit.framework.TestCase;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class TestHiveJoinWorkflow extends TestCase {

    public void test() throws Exception {
    // 执行hiveworkflow工作流
    // InputStream joinStream = this.getClass().getResourceAsStream("join.xml");
    // int taskid = 123;
    // JoinPhaseStatus joinPhaseStatus = new JoinPhaseStatus(taskid);
    // Map<EntityName, ERRules.TabFieldProcessor> dumpNodeExtraMetaMap = Collections.emptyMap();
    // WorkflowTaskConfigParser parser = new WorkflowTaskConfigParser(
    // new HiveTaskFactory(dumpNodeExtraMetaMap), (execContext) -> {
    // try {
    // return IOUtils.toString(joinStream, "utf8");
    // } catch (IOException e) {
    // throw new RuntimeException(e);
    // }
    // }, joinPhaseStatus);
    //
    // DefaultChainContext ccontext = new DefaultChainContext(new TestParamContext());
    // ccontext.setFileSystem(TISHdfsUtils.getFileSystem());
    // //${context.date}
    // ccontext.setPs("20171117142045");
    //
    // Map<IDumpTable, ITabPartition> pts = Maps.newHashMap();
    // pts.put(DumpTable.create("", ""), () -> "pt");
    // ExecChainContextUtils.setDependencyTablesPartitions(ccontext, pts);
    // TemplateContext tplContext = new TemplateContext(ccontext);
    // parser.startJoinSubTables(tplContext);
    }
}
