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

package com.qlangtech.tis.plugin.datax.hudi.keygenerator.impl;

import com.alibaba.datax.plugin.writer.hudi.IPropertiesBuilder;
import com.qlangtech.plugins.org.apache.hudi.keygen.constant.KeyGeneratorType;
import com.qlangtech.tis.extension.TISExtension;
import com.qlangtech.tis.plugin.datax.hudi.IDataXHudiWriter;
import com.qlangtech.tis.plugin.datax.hudi.keygenerator.HudiKeyGenerator;

import java.util.List;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-06-07 15:54
 **/
public class ComplexKeyGenerator extends HudiKeyGenerator {
    private static final KeyGeneratorType genType = KeyGeneratorType.COMPLEX;

    @Override
    public List<String> getRecordFields() {
        return this.recordFields;
    }

    @Override
    public List<String> getPartitionPathFields() {
        return this.partitionPathFields;
    }

    @Override
    public KeyGeneratorType getKeyGeneratorType() {
        return genType;
    }

//    @Override
//    public void setProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {
//        this.partition.setProps(props, hudiWriter);
//    }

    @Override
    protected void setKeyGenProps(IPropertiesBuilder props, IDataXHudiWriter hudiWriter) {

    }

    @TISExtension
    public static class DefaultDescriptor extends BasicHudiKeyGeneratorDescriptor {
        public DefaultDescriptor() {
            super();
        }

        @Override
        public String getDisplayName() {
            return genType.name();
        }
    }
}
