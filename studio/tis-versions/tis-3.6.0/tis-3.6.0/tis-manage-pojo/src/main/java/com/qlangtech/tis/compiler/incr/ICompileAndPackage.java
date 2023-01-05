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

package com.qlangtech.tis.compiler.incr;

import com.alibaba.citrus.turbine.Context;
import com.qlangtech.tis.compiler.java.FileObjectsContext;
import com.qlangtech.tis.runtime.module.misc.IControlMsgHandler;
import com.qlangtech.tis.sql.parser.IDBNodeMeta;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * 在主项目中对模块生成的代码进行隔离，可以在TIS自定义插件中进行扩展
 *
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2021-10-20 17:01
 **/
public interface ICompileAndPackage {

    /**
     * @param context
     * @param msgHandler
     * @param appName
     * @param dbNameMap
     * @param sourceRoot example: new File("/opt/data/tis/cfg_repo/streamscript/hudi3/20220527113451")
     * @param xmlConfigs 取得spring配置文件相关resourece
     * @return 构建完成TPI包
     * @throws Exception
     */
    File process(Context context, IControlMsgHandler msgHandler
            , String appName, Map<IDBNodeMeta, List<String>> dbNameMap, File sourceRoot, FileObjectsContext xmlConfigs) throws Exception;
}
