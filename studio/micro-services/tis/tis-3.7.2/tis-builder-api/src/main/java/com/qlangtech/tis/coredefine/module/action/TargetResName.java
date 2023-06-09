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

package com.qlangtech.tis.coredefine.module.action;

import com.qlangtech.tis.realtime.transfer.UnderlineUtils;
import org.apache.commons.lang.StringUtils;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-15 15:59
 **/
public class TargetResName {
    private final String name;

    public String getStreamSourceHandlerClass() {
        return "com.qlangtech.tis.realtime.transfer." + this.name + "." + UnderlineUtils.getJavaName(this.name) + "SourceHandle";
    }

    public TargetResName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("param name can not be empty");
        }
        this.name = name;
    }

    public boolean equalWithName(String name) {
        return this.getName().equals(name);
    }

    public String getName() {
        return this.name;
    }

    public String getK8SResName() {
        return StringUtils.replace(name, "_", "-");
    }

    @Override
    public String toString() {
        return "TargetResName{" +
                "name='" + name + '\'' +
                '}';
    }
}
