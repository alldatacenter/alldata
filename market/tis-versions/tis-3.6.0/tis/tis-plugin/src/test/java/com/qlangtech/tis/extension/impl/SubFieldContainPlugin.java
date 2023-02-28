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

package com.qlangtech.tis.extension.impl;

import com.qlangtech.tis.datax.IGroupChildTaskIterator;
import com.qlangtech.tis.datax.impl.DataxReader;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.SubForm;
import com.qlangtech.tis.plugin.annotation.Validator;
import com.qlangtech.tis.plugin.datax.SelectedTab;
import com.qlangtech.tis.plugin.ds.ISelectedTab;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-01 10:45
 **/
public class SubFieldContainPlugin extends DataxReader implements IdentityName {

    public static final String PLUGIN_NAME = "test_subFieldContainPlugin";
    public static final String SUB_PROP_FIELD_NAME = "selectedTabs";

    @FormField(ordinal = 0, identity = true, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String name;

    @FormField(ordinal = 1, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String prop2;

    @FormField(ordinal = 2, type = FormFieldType.INPUTTEXT, validate = {Validator.require})
    public String prop3;

//    @SubForm(desClazz = SubField.class
//            , idListGetScript = "return com.qlangtech.tis.coredefine.module.action.DataxAction.getTablesInDB(filter);", atLeastOne = true)
//    public List<SubField> subProps;

    @SubForm(desClazz = SelectedTab.class
            , idListGetScript = "return com.qlangtech.tis.coredefine.module.action.DataxAction.getTablesInDB(filter);", atLeastOne = true)
    public transient List<SelectedTab> selectedTabs;

    @Override
    public String identityValue() {
        return this.name;
    }

//    @Override
//    public Descriptor<SubFieldContainPlugin> getDescriptor() {
//        Descriptor<SubFieldContainPlugin> descriptor = TIS.get().getDescriptor(this.getClass());
//        return descriptor;
//    }

    @Override
    public <T extends ISelectedTab> List<T> getSelectedTabs() {
        return null;
    }

    @Override
    public IGroupChildTaskIterator getSubTasks() {
        return null;
    }

    @Override
    public String getTemplate() {
        return null;
    }

    @TISExtension()
    public static class DefaultDescriptor extends BaseDataxReaderDescriptor {
        @Override
        public String getDisplayName() {
            return PLUGIN_NAME;
        }

        @Override
        public boolean isSupportIncr() {
            return true;
        }

        @Override
        public EndType getEndType() {
            return EndType.MySQL;
        }

        @Override
        public boolean isRdbms() {
            return true;
        }
    }
}
