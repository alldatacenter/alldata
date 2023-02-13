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

import com.qlangtech.tis.plugin.IdentityName;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-09 09:53
 **/
public class ContainAdvanceFieldPlugin implements Describable<ContainAdvanceFieldPlugin>, IdentityName {

    @FormField(identity = true, type = FormFieldType.INPUTTEXT)
    public String name;

    @FormField(type = FormFieldType.ENUM, advance = true)
    public String propVal;

    @Override
    public String identityValue() {
        return this.name;
    }

    @TISExtension
    public static class DefaultDescriptor extends Descriptor<ContainAdvanceFieldPlugin> {
        public DefaultDescriptor() {
            super();
        }
    }

}
