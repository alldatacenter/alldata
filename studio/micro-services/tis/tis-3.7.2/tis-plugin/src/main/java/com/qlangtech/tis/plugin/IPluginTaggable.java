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

import java.util.List;

/**
 * 对插件进行打标签，可以在安装面板，或者插件可用下拉列表进行过滤
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2023-03-13 20:07
 **/
public interface IPluginTaggable {

    public List<PluginTag> getTags();

    enum PluginTag {
        OfflineParser("offline_parser");
        private final String token;

        public static PluginTag parse(String token) {
            for (PluginTag tag : PluginTag.values()) {
                if (tag.token.equalsIgnoreCase(token)) {
                    return tag;
                }
            }
            throw new IllegalStateException("token:" + token + " is invalid");
        }

        public String getToken() {
            return token;
        }

        PluginTag(String token) {
            this.token = token;
        }
    }
}
