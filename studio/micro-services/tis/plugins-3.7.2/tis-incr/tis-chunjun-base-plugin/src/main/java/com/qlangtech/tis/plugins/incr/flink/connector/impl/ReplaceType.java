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

package com.qlangtech.tis.plugins.incr.flink.connector.impl;

import com.dtstack.chunjun.sink.WriteMode;
import com.qlangtech.tis.extension.TISExtension;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-07-18 10:09
 **/
public class ReplaceType extends BasicUpdate {

//    @Override
//    protected String getMode() {
//        return WriteMode.REPLACE.getMode();
//    }

    @TISExtension
    public static final class DftDescriptor extends BasicDescriptor {
        public DftDescriptor() {
            super(WriteMode.REPLACE);
        }

//        public String getDisplayName() {
//            return StringUtils.capitalize(WriteMode.REPLACE.getMode());
//        }
    }
}
