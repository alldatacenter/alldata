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
package com.qlangtech.tis.solrdao;

import java.util.List;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-01-29 15:08
 */
public interface ISchemaPluginContext extends ISchemaFieldTypeContext {

    ISchemaPluginContext NULL = new ISchemaPluginContext() {
        @Override
        public <TYPE extends IFieldTypeFactory> List<TYPE> getFieldTypeFactories() {
            return null;
        }

        @Override
        public <TYPE extends IFieldTypeFactory> TYPE findFieldTypeFactory(String name) {
            return null;
        }

        @Override
        public boolean isTokenizer(String typeName) {
            return false;
        }
    };


    <TYPE extends IFieldTypeFactory> List<TYPE> getFieldTypeFactories();

    <TYPE extends IFieldTypeFactory> TYPE findFieldTypeFactory(String name);
}
