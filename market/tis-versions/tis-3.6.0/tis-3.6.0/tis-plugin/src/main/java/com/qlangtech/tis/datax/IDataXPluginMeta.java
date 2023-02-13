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

package com.qlangtech.tis.datax;

import com.alibaba.fastjson.JSON;
import com.qlangtech.tis.extension.impl.IOUtils;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-04-24 16:25
 **/
public interface IDataXPluginMeta {


    String END_TARGET_TYPE = "targetType";

    default DataXMeta getDataxMeta() {
        Class<?> clazz = this.getOwnerClass();
        return IOUtils.loadResourceFromClasspath(clazz, clazz.getSimpleName() + "_plugin.json", true
                , new IOUtils.WrapperResult<DataXMeta>() {
                    @Override
                    public DataXMeta process(InputStream input) throws IOException {
                        return JSON.parseObject(input, DataXMeta.class);
                    }
                });
    }

    default Class<?> getOwnerClass() {
        return this.getClass();
    }

    public class DataXMeta {
        private String name;
        private String clazz;
        private String description;
        private String developer;

        public String getImplClass() {
            if (StringUtils.isEmpty(this.clazz)) {
                throw new IllegalStateException("plugin:" + name + " relevant implements class can not be null ");
            }
            return this.clazz;
        }

        public void setClass(String clazz) {
            this.clazz = clazz;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDeveloper() {
            return developer;
        }

        public void setDeveloper(String developer) {
            this.developer = developer;
        }
    }
}
