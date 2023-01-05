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
package com.qlangtech.tis.solrdao.extend;

import org.apache.commons.lang.StringUtils;

/**
 * 扩展schemaField的
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2016年12月5日
 */
public class ProcessorSchemaField extends BaseExtendConfig {

    // public static final Pattern PATTERN_PARMS =
    // Pattern.compile("(\\w+?)=([^\\s]+)");
    private String processorName;

    // private final Map<String, String> params = new HashMap<String, String>();
    private static final String TARGET_COLUMN = "column";

    private ProcessorSchemaField(String processorName, String processorArgs) {
        super(processorArgs);
        this.processorName = processorName;
    }

    public static ProcessorSchemaField create(String processorName, String processorArgs) {
        ProcessorSchemaField columnProcess = new ProcessorSchemaField(processorName, processorArgs);
        return columnProcess;
    }

    public String getProcessorName() {
        return processorName;
    }

    // public Map<String, String> getParams() {
    // return params;
    // }
    /**
     * 目标列是否为空，該判斷用在處理整個row處理（比如幾個列組合通過一個md5生成一個新列） 還是單個column處理的方式（生成另外一個或者多個列）
     *
     * @return
     */
    public boolean isTargetColumnEmpty() {
        return StringUtils.isBlank(getParams().get(TARGET_COLUMN));
    }

    public String getTargetColumn() {
        String columnName = getParams().get(TARGET_COLUMN);
        if (StringUtils.isBlank(columnName)) {
            throw new IllegalStateException("columnName can not be null");
        }
        return columnName;
    }
}
