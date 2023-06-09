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

package com.qlangtech.tis.plugin.datax;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.datax.impl.ESTableAlias;
import com.qlangtech.tis.extension.*;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.extension.impl.SuFormProperties;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.SubForm;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.ds.*;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.tuple.creator.EntityName;
import com.qlangtech.tis.util.impl.AttrVals;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author: baisui 百岁
 * @create: 2021-04-08 13:29
 **/
public class SelectedTab implements Describable<SelectedTab>, ISelectedTab, IdentityName {
    private static final String KEY_TABLE_COLS = "tableRelevantCols";
    public static final String KEY_FIELD_COLS = "cols";
    private static final Logger logger = LoggerFactory.getLogger(SelectedTab.class);

    // 针对增量构建流程中的属性扩展
    private IncrSelectedTabExtend incrSourceProps;
    private IncrSelectedTabExtend incrSinkProps;

    // 表名称
    @FormField(identity = true, ordinal = 0, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String name;

    // 用户可以自己设置where条件
    @FormField(ordinal = 100, type = FormFieldType.INPUTTEXT)
    public String where;

    @FormField(ordinal = 200, type = FormFieldType.MULTI_SELECTABLE, validate = {Validator.require})
    public List<String> cols = Lists.newArrayList();

    private transient List<CMeta> shadowCols = null;

    public SelectedTab(String name) {
        this.name = name;
    }

    public String identityValue() {
        return this.name;
    }

    public <T extends IncrSelectedTabExtend> T getIncrSinkProps() {
        return (T) incrSinkProps;
    }

    public void setIncrSinkProps(IncrSelectedTabExtend incrSinkProps) {
        this.incrSinkProps = incrSinkProps;
    }

    public <T extends IncrSelectedTabExtend> T getIncrSourceProps() {
        return (T) this.incrSourceProps;
    }

    public <T extends IncrSelectedTabExtend> List<T> getIncrExtProp() {
        List<T> result = Lists.newArrayList();
        if (this.getIncrSourceProps() != null) {
            result.add(this.getIncrSourceProps());
        }
        if (this.getIncrSinkProps() != null) {
            result.add(this.getIncrSinkProps());
        }
        return result;
    }

    public void setIncrSourceProps(IncrSelectedTabExtend incrProps) {
        this.incrSourceProps = incrProps;
    }

    public static List<Option> getColsCandidate() {
        return getContextTableCols((cols) -> cols.stream());
    }

    public SelectedTab() {
    }

    /**
     * 取得默认的表名称
     *
     * @return
     */
    public static String getDftTabName() {
        DataxReader dataXReader = DataxReader.getThreadBingDataXReader();
            if (dataXReader == null) {
                return StringUtils.EMPTY;
        }

        try {
            List<ISelectedTab> selectedTabs = dataXReader.getSelectedTabs();
            if (CollectionUtils.isEmpty(selectedTabs)) {
                return StringUtils.EMPTY;
            }
            for (ISelectedTab tab : selectedTabs) {
                return tab.getName();
            }
        } catch (Throwable e) {
            logger.warn(dataXReader.getDescriptor().getDisplayName() + e.getMessage());
        }

        return StringUtils.EMPTY;
    }

    public String getWhere() {
        return this.where;
    }


    public void setWhere(String where) {
        this.where = where;
    }

    public String getName() {
        return this.name;
    }

    public boolean isAllCols() {
        return this.cols.isEmpty();
    }

    public List<CMeta> getCols() {
        if (shadowCols == null) {
            shadowCols = this.cols.stream().map((c) -> {
                CMeta colMeta = new CMeta();
                colMeta.setName(c);
                return colMeta;
            }).collect(Collectors.toList());
        }
        return shadowCols;
    }

    public boolean containCol(String col) {
        return cols != null && this.cols.contains(col);
    }

    public void setCols(List<String> cols) {
        this.cols = cols;
    }


    public static List<Option> getContextTableCols(Function<List<ColumnMetaData>, Stream<ColumnMetaData>> func) {
        SuFormProperties.SuFormGetterContext context = SuFormProperties.subFormGetterProcessThreadLocal.get();
        if (context == null || context.plugin == null) {
            return Collections.emptyList();
        }
        Describable plugin = Objects.requireNonNull(context.plugin, "context.plugin can not be null");
        if (!(plugin instanceof DataSourceMeta)) {
            throw new IllegalStateException("plugin must be type of "
                    + DataSourceMeta.class.getName() + ", now type of " + plugin.getClass().getName());
        }
        DataSourceMeta dsMeta = (DataSourceMeta) plugin;
        List<ColumnMetaData> cols
                = context.getContextAttr(KEY_TABLE_COLS, (key) -> {
            try {
                return dsMeta.getTableMetadata(false, EntityName.parse(context.getSubFormIdentityField()));
            } catch (TableNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
//        return func.apply(cols).map((c) -> new Option(c.getName() + "(" + c.getType().getCollapse().getLiteria() + ")", c.getValue()))
//                .collect(Collectors.toList());

        return func.apply(cols).map((c) -> c)
                .collect(Collectors.toList());


    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<SelectedTab> implements SubForm.ISubFormItemValidate {
        @Override
        protected final boolean validateAll(IControlMsgHandler msgHandler, Context context, PostFormVals postFormVals) {

            ParseDescribable<Describable> plugin = this.newInstance(null, postFormVals.rawFormData, Optional.empty());
            SelectedTab tab = plugin.getInstance();
            if (tab.cols.isEmpty()) {
                msgHandler.addFieldError(context, SelectedTab.KEY_FIELD_COLS, "请选择");
                return false;
            }
            return this.validateAll(msgHandler, context, tab);
        }

        @Override
        public boolean validateSubFormItems(IControlMsgHandler msgHandler, Context context
                , BaseSubFormProperties props, IPropertyType.SubFormFilter filter, AttrVals formData) {

            Integer maxReaderTabCount = Integer.MAX_VALUE;
            try {
                maxReaderTabCount = Integer.parseInt(filter.uploadPluginMeta.getExtraParam(ESTableAlias.MAX_READER_TABLE_SELECT_COUNT));
            } catch (Throwable e) {

            }

            if (formData.size() > maxReaderTabCount) {
                msgHandler.addErrorMessage(context, "导入表不能超过" + maxReaderTabCount + "张");
                return false;
            }

            return true;
        }

        public PluginFormProperties getPluginFormPropertyTypes(Optional<IPropertyType.SubFormFilter> subFormFilter) {

            return super.getPluginFormPropertyTypes(subFormFilter);
        }

        protected boolean validateAll(IControlMsgHandler msgHandler, Context context, SelectedTab postFormVals) {
            return true;
        }
    }


}
