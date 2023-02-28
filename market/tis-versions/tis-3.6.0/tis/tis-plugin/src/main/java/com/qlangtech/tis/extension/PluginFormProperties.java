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
package com.qlangtech.tis.extension;

import com.alibaba.fastjson.JSON;
import com.qlangtech.tis.extension.impl.BaseSubFormProperties;
import com.qlangtech.tis.extension.impl.PropertyType;
import com.qlangtech.tis.extension.impl.RootFormProperties;
import com.qlangtech.tis.extension.impl.SuFormProperties;

import java.util.Map;
import java.util.Set;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-04-12 11:01
 */
public abstract class PluginFormProperties {
    public abstract Set<Map.Entry<String, PropertyType>> getKVTuples();

    public abstract JSON getInstancePropsJson(Object instance);

    public abstract <T> T accept(IVisitor visitor);

    public interface IVisitor {
        default <T> T visit(RootFormProperties props) {
            //throw new UnsupportedOperationException("process RootFormProperties");
            return null;
        }

        default <T> T visit(BaseSubFormProperties props) {
            return null;
            //throw new UnsupportedOperationException("process SuFormProperties");
        }
    }

}
