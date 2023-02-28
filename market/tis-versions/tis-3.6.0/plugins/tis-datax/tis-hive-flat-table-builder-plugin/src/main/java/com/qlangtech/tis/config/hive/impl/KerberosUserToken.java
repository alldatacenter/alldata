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

package com.qlangtech.tis.config.hive.impl;

import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.config.hive.HiveUserToken;
import com.qlangtech.tis.config.hive.IHiveUserTokenVisitor;
import com.qlangtech.tis.config.kerberos.IKerberos;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.annotation.FormField;
import com.qlangtech.tis.plugin.annotation.FormFieldType;
import com.qlangtech.tis.plugin.annotation.Validator;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-01 12:59
 **/
public class KerberosUserToken extends HiveUserToken implements IKerberosUserToken {

    @FormField(ordinal = 1, type = FormFieldType.SELECTABLE, validate = {Validator.require})
    public String kerberos;

    @Override
    public void accept(IHiveUserTokenVisitor visitor) {
        visitor.visit(this);

    }

    @Override
    public IKerberos getKerberosCfg() {
        return ParamsConfig.getItem(kerberos, IKerberos.IDENTITY);
    }

    @TISExtension
    public static class DefaultDesc extends Descriptor<HiveUserToken> {
        public DefaultDesc() {
            super();
            this.registerSelectOptions(IKerberos.IDENTITY, () -> ParamsConfig.getItems(IKerberos.IDENTITY));
        }

        @Override
        public String getDisplayName() {
            return IKerberos.IDENTITY;
        }
    }
}
