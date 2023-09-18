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
package com.qlangtech.tis.sql.parser.er;

import org.apache.commons.lang.StringUtils;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 主外键连接
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class JoinerKey {

    private String parentKey;

    private String childKey;

    public JoinerKey(String parentKey, String childKey) {
        if (StringUtils.isBlank(parentKey) || StringUtils.isBlank(childKey)) {
            throw new IllegalArgumentException("param parentKey or childKey can not be null");
        }
        this.parentKey = parentKey;
        this.childKey = childKey;
    }

    public JoinerKey() {
    }

    public static String createListNewLiteria(List<JoinerKey> joinerKeys) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Lists.newArrayList(");
        buffer.append(joinerKeys.stream().map((key) -> {
            StringBuffer b = new StringBuffer();
            return b.append("(\"").append(key.getParentKey()).append("\",\"").append(key.getChildKey()).append("\")");
        }).collect(Collectors.joining(",")));
        buffer.append(")");
        return buffer.toString();
    }

    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    public void setChildKey(String childKey) {
        this.childKey = childKey;
    }

    public String getParentKey() {
        return this.parentKey;
    }

    public String getChildKey() {
        return this.childKey;
    }

    @Override
    public String toString() {
        return "{" + "parentKey='" + parentKey + '\'' + ", childKey='" + childKey + '\'' + '}';
    }
}
