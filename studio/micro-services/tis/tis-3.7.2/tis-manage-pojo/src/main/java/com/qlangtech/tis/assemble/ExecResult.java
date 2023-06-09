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
package com.qlangtech.tis.assemble;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2017年6月17日
 */
public enum ExecResult {

    SUCCESS(1, "成功"), FAILD(-1, "失败"), DOING(2, "执行中"), ASYN_DOING(22, "执行中"), CANCEL(3, "终止");

    private final int value;

    private final String literal;

    public static ExecResult parse(int value) {
        for (ExecResult r : values()) {
            if (r.value == value) {
                return r;
            }
        }
        throw new IllegalStateException("vale:" + value + " is illegal");
    }

    /**
     * 任务是否正在执行中
     *
     * @return
     */
    public boolean isProcessing() {
        return this == DOING || this == ASYN_DOING;
    }

    private ExecResult(int value, String literal) {
        this.value = value;
        this.literal = literal;
    }

    public String getLiteral() {
        return this.literal;
    }

    public int getValue() {
        return this.value;
    }
}
