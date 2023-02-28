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
package com.qlangtech.tis.sql.parser;

import org.apache.commons.lang.StringUtils;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class ColName {

    private final String name;

    private final String alias;

    public boolean useAlias() {
        return !StringUtils.equals(this.name, this.alias);
    }

    public ColName(String name) {
        this(name, name);
    }

    public String getName() {
        return this.name;
    }

    public String getAliasName() {
        return this.alias;
    }

    public ColName(String name, String alias) {
        super();
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("param name can not be empty");
        }
        this.name = name;
        this.alias = alias;
    }

    @Override
    public int hashCode() {
        return alias.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        return this.hashCode() == obj.hashCode();
    }

    public String toString() {
        if (StringUtils.equals(name, this.alias)) {
            return "name:" + name;
        } else {
            return "name:" + name + ",alias:" + this.alias;
        }
    }
}
