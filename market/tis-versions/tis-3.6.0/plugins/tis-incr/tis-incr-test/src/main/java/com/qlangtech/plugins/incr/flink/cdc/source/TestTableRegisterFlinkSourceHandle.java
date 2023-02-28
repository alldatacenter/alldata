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

package com.qlangtech.plugins.incr.flink.cdc.source;

import com.google.common.collect.Maps;
import com.qlangtech.plugins.incr.flink.cdc.ColMeta;
import com.qlangtech.plugins.incr.flink.cdc.FlinkCol;
import com.qlangtech.plugins.incr.flink.cdc.IResultRows;
import com.qlangtech.tis.async.message.client.consumer.IConsumerHandle;
import com.qlangtech.tis.coredefine.module.action.TargetResName;
import com.qlangtech.tis.realtime.dto.DTOStream;
import com.qlangtech.tis.realtime.TISTableEnvironment;
import com.qlangtech.tis.realtime.TableRegisterFlinkSourceHandle;
import com.qlangtech.tis.sql.parser.tuple.creator.IStreamIncrGenerateStrategy;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableResult;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-28 17:45
 **/
public class TestTableRegisterFlinkSourceHandle extends TableRegisterFlinkSourceHandle implements IResultRows, Serializable {
    private final Map<String, TableResult> sourceTableQueryResult = Maps.newHashMap();
    private final String tabName;
    int tableCount;
    private List<ColMeta> cols;

    Map<String, FlinkCol> flinkColMap;

    public TestTableRegisterFlinkSourceHandle(String tabName, List<ColMeta> cols) {
        this.tabName = tabName;
        this.cols = Objects.requireNonNull(cols, "param cols can not be null");
    }

    @Override
    public Object deColFormat(String tableName, String colName, Object val) {
        Assert.assertEquals(tableName, this.tabName);
        FlinkCol flinkCol = flinkColMap.get(colName);
        if (flinkCol == null) {
            throw new IllegalStateException("table:" + tableName + ",colName:" + colName + " relevant flinkCol can not be null");
        }
        return flinkCol.rowDataProcess.deApply(val);
    }
//    @Override
//    protected List<FlinkCol> getTabColMetas(TargetResName dataxName, String tabName) {
//        Assert.assertEquals(this.tabName, tabName);
//        return cols;
//    }


    @Override
    protected List<FlinkCol> getTabColMetas(TargetResName dataxName, String tabName) {
        List<FlinkCol> flinkCols = super.getTabColMetas(dataxName, tabName);
        this.flinkColMap = flinkCols.stream().collect(Collectors.toMap((c) -> c.name, (c) -> c));
        return flinkCols;
    }

    @Override
    protected String getSinkTypeName() {
       throw new UnsupportedOperationException();
    }


    @Override
    protected StreamExecutionEnvironment getFlinkExecutionEnvironment() {
        return StreamExecutionEnvironment.getExecutionEnvironment();
    }

    @Override
    public IConsumerHandle getConsumerHandle() {
        return this;
    }

    @Override
    public Map<String, TableResult> getSourceTableQueryResult() {
        return this.sourceTableQueryResult;
    }

//    @Override
//    public TISSinkFactory getSinkFuncFactory() {
//        super.getSinkFuncFactory();
//        return new TISSinkFactory() {
//            @Override
//            public <SinkFunc> Map<TableAlias, SinkFunc> createSinkFunction(IDataxProcessor dataxProcessor) {
//                return Collections.emptyMap();
//            }
//
//            @Override
//            public ICompileAndPackage getCompileAndPackageManager() {
//                throw new UnsupportedOperationException();
//            }
//        };
//    }

    @Override
    protected void registerSourceTable(StreamTableEnvironment tabEnv, String tabName, DTOStream sourceStream) {
        super.registerSourceTable(tabEnv, tabName, sourceStream);
        if (tableCount++ > 1) {
            throw new IllegalStateException("testCase just test 1 table,pre:" + this.tabName + ",new:" + tabName);
        }
        Assert.assertEquals(this.tabName, tabName);

    }

    @Override
    protected void executeSql(TISTableEnvironment tabEnv) {
        // IStreamIncrGenerateStrategy.IStreamTemplateData.KEY_STREAM_SOURCE_TABLE_SUFFIX
        if (StringUtils.isEmpty(this.tabName)) {
            throw new IllegalStateException("prop tabName can not be null");
        }
        sourceTableQueryResult.put(this.tabName, tabEnv.tabEnv.executeSql("SELECT * FROM "
                + tabName + IStreamIncrGenerateStrategy.IStreamTemplateData.KEY_STREAM_SOURCE_TABLE_SUFFIX));
    }

    @Override
    protected JobExecutionResult executeFlinkJob(TargetResName dataxName, StreamExecutionEnvironment env) throws Exception {
        // return super.executeFlinkJob(dataxName, env);
        // 测试环境下不能执行 return env.execute(dataxName.getName()); 不然单元测试不会自动退出了
        return null;
    }
}
