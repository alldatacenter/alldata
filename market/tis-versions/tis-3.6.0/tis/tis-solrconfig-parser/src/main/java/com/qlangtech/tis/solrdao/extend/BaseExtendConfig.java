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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 基础配置
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2018年10月12日
 */
public class BaseExtendConfig {

    public static final Pattern PATTERN_PARMS = Pattern.compile("(\\w+?)=([^\\s]+)");

    protected final Map<String, String> params = new HashMap<String, String>();

    public BaseExtendConfig(String args) {
        Matcher matcher = PATTERN_PARMS.matcher(args);
        while (matcher.find()) {
            params.put(matcher.group(1), matcher.group(2));
        }
    }

    public final Map<String, String> getParams() {
        return this.params;
    }

    public final void putParam(String key, String value) {
        params.put(key, value);
    }

    public String getParam(String key) {
        return params.get(key);
    }
}
