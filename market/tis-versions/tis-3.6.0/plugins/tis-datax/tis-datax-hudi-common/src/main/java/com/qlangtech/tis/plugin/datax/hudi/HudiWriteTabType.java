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

package com.qlangtech.tis.plugin.datax.hudi;

/**
 * https://hudi.apache.org/docs/table_types
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-02-04 11:45
 **/
public enum HudiWriteTabType {
    COW("COPY_ON_WRITE"), MOR("MERGE_ON_READ");

    private final String value;

    private HudiWriteTabType(String literia) {
        this.value = literia;
    }

    public String getValue() {
        return this.value;
    }

    public static HudiWriteTabType parse(String value) {
        for (HudiWriteTabType v : HudiWriteTabType.values()) {
            if (v.value.equalsIgnoreCase(value)) {
                return v;
            }
        }
        throw new IllegalStateException("token value is invalid:" + value);
    }
}
