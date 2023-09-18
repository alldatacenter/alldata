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

package com.qlangtech.plugins.incr.flink.cdc;

import com.google.common.collect.Lists;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.BasicDataSourceFactory;
import com.qlangtech.tis.plugins.incr.flink.chunjun.offset.LatestLocation;
import com.qlangtech.tis.plugins.incr.flink.chunjun.offset.StartLocation;
import com.qlangtech.tis.plugins.incr.flink.chunjun.poll.RunInterval;
import com.qlangtech.tis.plugins.incr.flink.chunjun.source.SelectedTabPropsExtends;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.types.RowKind;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-08-29 10:40
 **/
public class CDCTestSuitParams {
    final String tabName;
    final boolean shallTestDeleteProcess;
    //
    /**
     * 一条更新基于Binlog则会生成两条一条（U-）一条（U+），如果是 基于chunjun的则只会生成一条（I+）
     */
    final List<RowKind> updateRowKind;//UPDATE_AFTER
    public OverwriteSelectedTab overwriteSelectedTab;

//    private TestRow.ValProcessor rewriteExpectValProcessor;
//    private TestRow.ValProcessor rewriteActualValProcessor;

//    public TestRow.ValProcessor getRewriteExpectValProcessor() {
//        return rewriteExpectValProcessor;
//    }
//
//    public TestRow.ValProcessor getRewriteActualValProcessor() {
//        return rewriteActualValProcessor;
//    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public static Builder.ChunjunSuitParamsBuilder chunjunBuilder() {
        Builder.ChunjunSuitParamsBuilder builder = new Builder.ChunjunSuitParamsBuilder();
        builder.notTestDeleteProcess();
        builder.setUpdateRowKind(RowKind.INSERT);
        return builder;
    }

    public static class Builder {
        protected String tabName;
        protected boolean shallTestDeleteProcess = true;
        /**
         * 更新流程下RowKind 类型，Chunjun使用polling查询情况下，更新的rowkind 为insert（I+）类型
         */
        protected List<RowKind> updateRowKind = Lists.newArrayList(RowKind.UPDATE_BEFORE, RowKind.UPDATE_AFTER);
//        private TestRow.ValProcessor rewriteExpectValProcessor;
//        private TestRow.ValProcessor rewriteActualValProcessor;

        public Builder setUpdateRowKind(RowKind... updateRowKind) {
            this.updateRowKind = Lists.newArrayList(updateRowKind);
            return this;
        }

        public Builder setTabName(String tabName) {
            this.tabName = tabName;
            return this;
        }

        public Builder notTestDeleteProcess() {
            this.shallTestDeleteProcess = false;
            return this;
        }

//        public Builder setRewriteExpectValProcessor(TestRow.ValProcessor rewriteExpectValProcessor) {
//            this.rewriteExpectValProcessor = rewriteExpectValProcessor;
//            return this;
//        }
//
//        public Builder setRewriteActualValProcessor(TestRow.ValProcessor rewriteActualValProcessor) {
//            this.rewriteActualValProcessor = rewriteActualValProcessor;
//            return this;
//        }

        public CDCTestSuitParams build() {
            CDCTestSuitParams suitParams = createParams();
//            suitParams.rewriteExpectValProcessor = this.rewriteExpectValProcessor;
//            suitParams.rewriteActualValProcessor = this.rewriteActualValProcessor;
            return suitParams;
        }

        protected CDCTestSuitParams createParams() {
            return new CDCTestSuitParams(this.tabName, this.shallTestDeleteProcess, this.updateRowKind);
        }

        public static class ChunjunSuitParamsBuilder extends Builder {
            private String incrColumn = CUDCDCTestSuit.key_update_time;

            private StartLocation startLocation = new LatestLocation();

            public ChunjunSuitParamsBuilder setIncrColumn(String incrColumn) {
                if (StringUtils.isEmpty(incrColumn)) {
                    throw new IllegalArgumentException("param incrColumn can not be empty");
                }
                this.incrColumn = incrColumn;
                return this;
            }

            public ChunjunSuitParamsBuilder setStartLocation(StartLocation startLocation) {
                this.startLocation = startLocation;
                return this;
            }

            //        @Override
            //        public CDCTestSuitParams build() {
            //            return super.build();
            //        }
            @Override
            protected CDCTestSuitParams createParams() {
                CDCTestSuitParams params = super.createParams();
                params.overwriteSelectedTab = (cdcTestSuit, tabName, dataSourceFactory, tab) -> {
                    SelectedTabPropsExtends incrTabExtend = new SelectedTabPropsExtends();
                    RunInterval polling = new RunInterval();
                    polling.useMaxFunc = true;
                    polling.incrColumn = this.incrColumn;//CUDCDCTestSuit.key_update_time; //cdcTestSuit.getPrimaryKeyName(tab);
                    polling.pollingInterval = 4999;
                    polling.startLocation = this.startLocation;
                    incrTabExtend.polling = polling;
                    tab.setIncrSourceProps(incrTabExtend);
                };
                return params;
            }
        }
    }

    protected CDCTestSuitParams(String tabName, boolean shallTestDeleteProcess, List<RowKind> updateRowKind) {
        this.tabName = tabName;
        this.shallTestDeleteProcess = shallTestDeleteProcess;
        this.updateRowKind = updateRowKind;
    }

    public String getTabName() {
        return tabName;
    }

    public void setOverwriteSelectedTab(OverwriteSelectedTab overwriteSelectedTab) {
        this.overwriteSelectedTab = overwriteSelectedTab;
    }

    public interface OverwriteSelectedTab {
        void apply(CUDCDCTestSuit cdcTestSuit, String tabName, BasicDataSourceFactory dataSourceFactory, SelectedTab tab);
    }


//    public interface RewriteExpectValProcessor {
//        TestRow.ValProcessor apply(TestRow.ValProcessor valProcess);
//    }
//
//
//    public interface RewriteActualValProcessor {
//        TestRow.ValProcessor apply(String tabName, TestRow.ValProcessor valProcess);
//    }


}
