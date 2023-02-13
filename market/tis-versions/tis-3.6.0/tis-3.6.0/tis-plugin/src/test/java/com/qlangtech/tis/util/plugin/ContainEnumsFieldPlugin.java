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

package com.qlangtech.tis.util.plugin;

import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.util.plugin.impl.EnumProp1;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-09-16 08:22
 **/
public class ContainEnumsFieldPlugin extends TestPlugin {

    @FormField(ordinal = 0)
    public BaseEnumProp enumProp;


    public static String acceptEnumProp = EnumProp1.KEY;

    public static List<Descriptor> filter(List<Descriptor> enumPropDescs) {
        return enumPropDescs.stream().filter((desc) ->
                acceptEnumProp.equals(desc.getDisplayName())).collect(Collectors.toList());
    }

    @TISExtension
    public static class DefaultDesc extends Descriptor<TestPlugin> {
    }
}
