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

import com.qlangtech.tis.annotation.Public;
import com.qlangtech.tis.config.hive.HiveUserToken;
import com.qlangtech.tis.config.hive.IHiveUserTokenVisitor;
import com.qlangtech.tis.extension.Descriptor;
import com.qlangtech.tis.extension.TISExtension;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-05-03 09:33
 **/
@Public
public class OffHiveUserToken extends HiveUserToken implements IOffHiveUserToken {

    @Override
    public void accept(IHiveUserTokenVisitor visitor) {
        visitor.visit(this);
    }

    @TISExtension
    public static class DefaultDesc extends Descriptor<HiveUserToken> {
        @Override
        public String getDisplayName() {
            return SWITCH_OFF;
        }
    }
}
