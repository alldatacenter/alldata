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
package com.qlangtech.tis.runtime.module.misc.impl;

import com.alibaba.citrus.turbine.Context;
import com.google.common.collect.Lists;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.Callable;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class DefaultFieldErrorHandler implements IFieldErrorHandler {

    public static final String KEY_VALIDATE_FIELDS_STACK = "validate_fields_stack";

    public static final String KEY_VALIDATE_ITEM_INDEX = "validate_item_index";

    public static final String KEY_VALIDATE_PLUGIN_INDEX = "validate_plugin_index";

    @Override
    public boolean validateBizLogic(BizLogic logicType, Context context, String fieldName, String value) {
        throw new UnsupportedOperationException();
    }

    public static void pushFieldStack(Context context, String fieldName, int itemIndex) {
        Stack<FieldIndex> fieldStack = (Stack<FieldIndex>) context.get(KEY_VALIDATE_FIELDS_STACK);
        if (fieldStack == null) {
            fieldStack = new Stack<>();
            context.put(KEY_VALIDATE_FIELDS_STACK, fieldStack);
        }
        fieldStack.push(new FieldIndex(fieldName, itemIndex));
    }

    public static Stack<FieldIndex> getFieldStack(Context context) {
        return (Stack<FieldIndex>) context.get(KEY_VALIDATE_FIELDS_STACK);
    }

    public static void popFieldStack(Context context) {
        Stack<FieldIndex> fieldStack = (Stack<FieldIndex>) context.get(KEY_VALIDATE_FIELDS_STACK);
        if (fieldStack == null) {
            return;
            // fieldStack = new Stack<>();
            // context.put(KEY_VALIDATE_FIELDS_STACK, fieldStack);
        }
        fieldStack.pop();
    }

    @Override
    public final void addFieldError(Context context, String fieldName, String msg, Object... params) {
        Integer pluginIndex = (Integer) context.get(KEY_VALIDATE_PLUGIN_INDEX);
        Integer itemIndex = (Integer) context.get(KEY_VALIDATE_ITEM_INDEX);
        final Stack<FieldIndex> fieldStack = getFieldStack(context);
        itemIndex = (itemIndex == null ? 0 : itemIndex);
        pluginIndex = (pluginIndex == null ? 0 : pluginIndex);
        List<FieldError> fieldsErrorList = getFieldsError(context, fieldStack, pluginIndex, itemIndex);
        fieldsErrorList.add(new FieldError(fieldName, msg));
    }

    private List<FieldError> getFieldsError(Context context, Stack<FieldIndex> fieldStack, Integer pluginIndex, Integer itemIndex) {
        List<List<List<FieldError>>> /**
         * item
         */
                pluginErrorList = (List<List<List<FieldError>>>) context.get(ACTION_ERROR_FIELDS);
        if (pluginErrorList == null) {
            pluginErrorList = Lists.newArrayList();
            context.put(ACTION_ERROR_FIELDS, pluginErrorList);
        }
        List<List<FieldError>> /**
         * item
         */
                itemsErrorList = getFieldErrors(pluginIndex, pluginErrorList, () -> Lists.newArrayList());
        List<FieldError> fieldsErrorList = getFieldErrors(itemIndex, itemsErrorList, () -> Lists.newArrayList());
        if (fieldStack == null || fieldStack.size() < 1) {
            return fieldsErrorList;
        } else {
            for (int index = 0; index < fieldStack.size(); index++) {
                FieldIndex fieldIndex = fieldStack.get(index);
                Optional<FieldError> find = fieldsErrorList.stream().filter((f) -> StringUtils.equals(f.getFieldName(), fieldIndex.filedName)).findFirst();
                FieldError fieldErr = null;
                if (find.isPresent()) {
                    fieldErr = find.get();
                } else {
                    fieldErr = new FieldError(fieldIndex.filedName, null);
                    fieldsErrorList.add(fieldErr);
                }
                if (fieldErr.itemsErrorList == null) {
                    fieldErr.itemsErrorList = Lists.newArrayList();
                }
                fieldsErrorList = getFieldErrors(fieldIndex.itemIndex, fieldErr.itemsErrorList, () -> Lists.newArrayList());
            }
        }
        return fieldsErrorList;
    }

    private <T> T getFieldErrors(Integer itemIndex, List<T> itemsErrorList, Callable<T> newer) {
        try {
            T fieldsErrorList;
            while (true) {
                if (itemIndex >= itemsErrorList.size()) {
                    // Lists.newArrayList();
                    fieldsErrorList = newer.call();
                    itemsErrorList.add(fieldsErrorList);
                } else {
                    fieldsErrorList = itemsErrorList.get(itemIndex);
                    break;
                }
            }
            return fieldsErrorList;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static class FieldError {

        private final String fieldName;

        private final String msg;

        // 当field为插件类型时，并且定义了属性成员，则当字段出错
        public List<List<FieldError>> /**
         * item
         */
                itemsErrorList;

        public FieldError(String fieldName, String msg) {
            this.fieldName = fieldName;
            this.msg = msg;
        }

        public String getFieldName() {
            return this.fieldName;
        }

        public String getMsg() {
            return this.msg;
        }
    }

    public static class FieldIndex {

        // 字段名称
        public final String filedName;

        // 该字段在当前深度中的Item序号
        public final int itemIndex;

        public FieldIndex(String filedName, int itemIndex) {
            this.filedName = filedName;
            this.itemIndex = itemIndex;
        }
    }
}
