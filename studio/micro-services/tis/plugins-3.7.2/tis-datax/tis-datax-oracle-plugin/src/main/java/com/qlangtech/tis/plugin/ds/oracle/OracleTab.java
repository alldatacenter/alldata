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

package com.qlangtech.tis.plugin.ds.oracle;

import org.apache.commons.lang.StringUtils;

import java.util.Optional;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-10-04 20:42
 **/
public class OracleTab {
    public static final String ESCAPE_CHAR = "\"";
    public final Optional<String> owner;
    public final String tabName;

    public static OracleTab create(String table) {
        OracleTab otab = null;
        if (StringUtils.indexOf(table, ".") > -1) {
            String[] tab = StringUtils.split(table, ".");
            otab = new OracleTab(tab[0], tab[1]);
        } else {
            otab = new OracleTab(Optional.empty(), table);
        }
        return otab;
    }

    private OracleTab(String owner, String tabName) {
        this(Optional.of(StringUtils.upperCase(StringUtils.remove(owner, ESCAPE_CHAR))), tabName);
    }

    private OracleTab(Optional<String> owner, String tabName) {
        if (StringUtils.isEmpty(tabName)) {
            throw new IllegalArgumentException("tabName:" + tabName + " can not be null");
        }
        this.owner = owner;
        this.tabName = StringUtils.remove(tabName, ESCAPE_CHAR);
    }


}
