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
package com.qlangtech.tis.runtime.module.screen;

import com.alibaba.citrus.turbine.Context;

/**
 * 全局扩展资源配置，例如，solrcore中为某个应用配置了个性化分词器，<br>
 * 打成了jar包，以往需要通过手工将这个jar包部署到solrcore 服务器上<br>
 * 现在，事先将这个分词jar包部署到终搜后台中
 *
 * @author 百岁（baisui@qlangtech.com）
 * @date 2012-6-1
 */
public class GlobalResource {

    /**
     */
    private static final long serialVersionUID = 1L;

    public static final String UPLOAD_RESOURCE_TYPE_GLOBAL = "global_res";
    // @Override
    // public void execute(Context context) throws Exception {
    // }
}
