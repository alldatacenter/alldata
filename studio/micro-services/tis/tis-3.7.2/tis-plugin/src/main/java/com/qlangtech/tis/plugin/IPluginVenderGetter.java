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

package com.qlangtech.tis.plugin;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-05 10:57
 **/
public interface IPluginVenderGetter extends IEndTypeGetter {

    /**
     * 供应商
     *
     * @return
     */
    PluginVender getVender();

    enum PluginVender {
        FLINK_CDC("FlinkCDC", "flink-cdc", "https://ververica.github.io/flink-cdc-connectors") //
        , CHUNJUN("Chunjun", "chunjun", "https://dtstack.github.io/chunjun") //
        , TIS("TIS", "tis", "https://github.com/qlangtech/tis") //
        , DATAX("DataX", "datax", "https://github.com/alibaba/DataX");
        final String name;
        final String tokenId;
        final String url;

        private PluginVender(String name, String tokenId, String url) {
            this.name = name;
            this.tokenId = tokenId;
            this.url = url;
        }

        public String getTokenId() {
            return this.tokenId;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }
    }
}
