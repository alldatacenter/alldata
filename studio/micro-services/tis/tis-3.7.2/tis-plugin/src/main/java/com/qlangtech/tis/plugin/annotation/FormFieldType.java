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
package com.qlangtech.tis.plugin.annotation;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.extension.IPropertyType;
import com.qlangtech.tis.manage.common.Option;
import com.qlangtech.tis.runtime.module.misc.IFieldErrorHandler;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2019年1月11日
 */
public enum FormFieldType {
    /**
     * 多选字段,目标属性样例：'List<String> cols'
     */
    MULTI_SELECTABLE(8),

    INPUTTEXT(1),
    /**
     * 有多个选项可以选择
     */
    SELECTABLE(6),
    /**
     * 密码
     */
    PASSWORD(7),
    // 支持文件上传
    FILE(9, new IPropValProcessor() {
        @Override
        public Object process(Object instance, Object val) throws Exception {

            String[] filePath = StringUtils.split((String) val, ";");
            if (filePath.length == 2) {
                // 创建/更新
                File tmpPath = new File(filePath[0]);
                if (!tmpPath.exists()) {
                    throw new IllegalStateException("tmp path:" + tmpPath.getAbsolutePath() + " is not exist");
                }
                if (!(instance instanceof ITmpFileStore)) {
                    throw new IllegalStateException("instance of " + instance.getClass() + " must be type of " + ITmpFileStore.class.getName());
                }

                ((ITmpFileStore) instance).setTmpeFile(new ITmpFileStore.TmpFile(tmpPath));
                // org.apache.commons.io.FileUtils.copyFile(tmpPath, new File(xmlStoreFile.getParentFile(), filePath[1]));
                return filePath[1];
            } else if (filePath.length == 1) {
                // 保持不变
                String fileName = filePath[0];
                return fileName;
            } else {
                throw new IllegalArgumentException("filePath.length must be 2: " + val);
            }


        }
    }),
    TEXTAREA(2),
    DATE(3),
    /**
     * 输入一个数字
     */
    INT_NUMBER(4),
    ENUM(5);

    private final int identity;
    public final IPropValProcessor valProcessor;

    FormFieldType(int val) {
        this(val, new IPropValProcessor() {
        });
    }

    FormFieldType(int val, IPropValProcessor valProcessor) {
        this.identity = val;
        this.valProcessor = valProcessor;
    }

    public int getIdentity() {
        return this.identity;
    }

    /**
     * 可对多选控件进行校验
     */
    public interface IMultiSelectValidator {
        /**
         * @param msgHandler
         * @param subFormFilter
         * @param context
         * @param fieldName
         * @param items         多选条目列表
         * @return
         */
        public boolean validate(IFieldErrorHandler msgHandler
                , Optional<IPropertyType.SubFormFilter> subFormFilter, Context context, String fieldName, List<SelectedItem> items);
    }

    public static class SelectedItem extends Option {
        // 是否选中了
        private boolean checked;

        public SelectedItem(String name, String value, boolean checked) {
            super(name, value);
            this.checked = checked;
        }

        public boolean isChecked() {
            return checked;
        }
    }

    public interface IPropValProcessor {
        /**
         * @param val 从json中取出来的值
         * @return
         */
        public default Object process(Object instance, Object val) throws Exception {
            return val;
        }
    }
}
