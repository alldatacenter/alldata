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

package com.qlangtech.tis.plugins.incr.flink.chunjun.sink;

import com.dtstack.chunjun.connector.jdbc.dialect.JdbcDialect;
import com.dtstack.chunjun.connector.jdbc.dialect.SupportUpdateMode;
import com.dtstack.chunjun.sink.WriteMode;
import com.google.common.collect.Sets;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.IncrSelectedTabExtend;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.ColumnMetaData;
import com.qlangtech.tis.plugin.incr.TISSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.ChunjunSinkFactory;
import com.qlangtech.tis.plugins.incr.flink.connector.UpdateMode;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-18 11:51
 **/
public class SinkTabPropsExtends extends IncrSelectedTabExtend {
    public static final String KEY_UNIQUE_KEY = "uniqueKey";
    @FormField(ordinal = 2, type = FormFieldType.ENUM, validate = {Validator.require})
    public UpdateMode incrMode;

    @FormField(ordinal = 3, type = FormFieldType.ENUM, validate = {Validator.require})
    public List<String> uniqueKey;

//    public UpdateMode getIncrMode() {
//        return Objects.requireNonNull(incrMode, "incrMode can not be null");
//    }

    public void setParams(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(uniqueKey)) {
            throw new IllegalStateException("collection of 'updateKey' can not be null");
        }
        params.put(KEY_UNIQUE_KEY, this.uniqueKey);
        this.incrMode.set(params);
    }

    /**
     * 主键候选字段
     *
     * @return
     */
    public static List<Option> getPrimaryKeys() {
        return buildPrimaryKeys().pks;
    }

    private static PrimaryKeys buildPrimaryKeys() {
        boolean[] containPk = new boolean[1];
        List<Option> pkResult = SelectedTab.getContextTableCols((cols) -> {

            Optional<ColumnMetaData> findPks = cols.stream().filter((c) -> c.isPk()).findFirst();
            if (findPks.isPresent()) {
                containPk[0] = true;
                return cols.stream().filter((c) -> c.isPk());
            } else {
                // 如果不存在主键则全选
                return cols.stream();
            }
        });

        return new PrimaryKeys(containPk[0], pkResult);
    }

    private static class PrimaryKeys {
        final List<Option> pks;
        private final boolean containPk;

        public PrimaryKeys(boolean containPk, List<Option> pks) {
            this.pks = pks;
            this.containPk = containPk;
        }

        public List<String> createPkKeys() {
            if (this.containPk) {
                return pks.stream()
                        .map((pk) -> String.valueOf(pk.getValue())).collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
    }

    public static List<String> getDeftRecordKeys() {
        PrimaryKeys primaryKeys = buildPrimaryKeys();
        return primaryKeys.createPkKeys();
    }

    /**
     * 写入支持的三种方式
     *
     * @see com.dtstack.chunjun.connector.jdbc.sink.JdbcOutputFormat 的方法 #prepareTemplates
     */
    private static Set<WriteMode> insertSupportedWriteMode = Sets.newHashSet(WriteMode.INSERT, WriteMode.REPLACE, WriteMode.UPSERT);

    /**
     * 由于每种 JdbcDialect 支持的写入类型是不同的所以需要在在运行时 更新下拉列表需要进行过滤
     *
     * @param descs
     * @return
     * @see JdbcDialect
     * @see SupportUpdateMode
     */
    public static List<? extends Descriptor> filter(List<? extends Descriptor> descs) {
        if (CollectionUtils.isEmpty(descs)) {
            throw new IllegalArgumentException("param descs can not be null");
        }
        SuFormProperties.SuFormGetterContext context = SuFormProperties.subFormGetterProcessThreadLocal.get();
        Objects.requireNonNull(context, "context can not be null");
        if (context.param == null) {
            return descs;
        }
        //Objects.requireNonNull(context.param, "'context.param' can not be null");
        //String dataXName = context.param.getDataXName();
        ChunjunSinkFactory sinkFactory = (ChunjunSinkFactory) TISSinkFactory.getIncrSinKFactory(context.param.getDataXName());
        Set<WriteMode> writeModes = sinkFactory.supportSinkWriteMode();
        return descs.stream().filter((d) -> {
            WriteMode wmode = ((UpdateMode.BasicDescriptor) d).writeMode;
            return writeModes.contains(wmode) && insertSupportedWriteMode.contains(wmode);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isSource() {
        return false;
    }

    @TISExtension
    public static class DefaultDescriptor extends BaseDescriptor {

//        @Override
//        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, SelectedTab postFormVals) {
//
//            SinkTabPropsExtends tab = (SinkTabPropsExtends) postFormVals;
//            boolean success = true;
////            if (!tab.containCol(tab.sourceOrderingField)) {
////                msgHandler.addFieldError(context, KEY_SOURCE_ORDERING_FIELD, "'"
////                        + tab.sourceOrderingField + "'需要在" + SelectedTab.KEY_FIELD_COLS + "中被选中");
////                success = false;
////            }
////
////            if (tab.keyGenerator != null) {
////                for (String field : tab.keyGenerator.getRecordFields()) {
////                    if (!tab.containCol(field)) {
////                        msgHandler.addFieldError(context, KEY_RECORD_FIELD
////                                , "'" + field + "'需要在" + SelectedTab.KEY_FIELD_COLS + "中被选中");
////                        success = false;
////                        break;
////                    }
////                }
////            }
//            return success;
//        }


    }
}
