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

package com.qlangtech.tis.trigger;

/**
 * 每次触发的执行操作类型
 *
 * @date 2014年11月4日下午1:21:13
 */
public enum ExecType {

    UPDATE("update"), CREATE("create"), FULLBUILD("fullbuild");

    private final String type;

    public String getType() {
        return type;
    }

    public static ExecType parse(String value) {
        if (UPDATE.type.equals(value)) {
            return UPDATE;
        } else if (CREATE.type.equals(value)) {
            return CREATE;
        } else if (FULLBUILD.type.equals(value)) {
            return FULLBUILD;
        } else {
            throw new IllegalStateException("value is illeal:" + value);
        }
    }

    private ExecType(String type) {
        this.type = type;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

    }

}
