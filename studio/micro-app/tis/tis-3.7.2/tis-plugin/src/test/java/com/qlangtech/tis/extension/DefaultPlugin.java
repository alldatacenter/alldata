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

package com.qlangtech.tis.extension;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-06-11 09:57
 **/
public class DefaultPlugin implements Describable<DefaultPlugin> {

    public static final String DFT_NAME_VALUE = "baisui";
    public static final String FILED_NAME_DESCRIPTION = "yours name descripion";

    @FormField(identity = false, type = FormFieldType.INPUTTEXT)
    public String name;

    @FormField(type = FormFieldType.TEXTAREA)
    public String cols;

    static int turn;

    public static String getColsDefaultVal() {

        JSONArray cols = new JSONArray();
        JSONObject col = new JSONObject();
        col.put("name", "baisui" + (turn++));
        cols.add(col);
        return cols.toJSONString(); // "cols_default_val_turn_" + turn++;
    }

//    @Override
//    public Descriptor<DefaultPlugin> getDescriptor() {
//        Descriptor<DefaultPlugin> descriptor = TIS.get().getDescriptor(this.getClass());
//        return descriptor;
//    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<DefaultPlugin> {
        public DefaultDescriptor() {
            this.addFieldDescriptor("name", DFT_NAME_VALUE, FILED_NAME_DESCRIPTION);
           // this.addFieldDescriptor("xxx", DFT_NAME_VALUE, FILED_NAME_DESCRIPTION);
        }

        public void addFieldDescriptor(String fieldName, Object dftVal, String helperContent) {
            super.addFieldDescriptor(fieldName, dftVal, helperContent);
        }
    }
}
