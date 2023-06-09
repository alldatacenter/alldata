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

package com.qlangtech.tis.config.spark;

import com.qlangtech.tis.config.ParamsConfig;
import com.qlangtech.tis.plugin.IdentityName;

import java.io.File;

/**
 * @author: 百岁（baisui@qlangtech.com）
 * @create: 2022-01-28 09:57
 **/
public interface ISparkConnGetter extends IdentityName {

    public static ISparkConnGetter getConnGetter(String sparkConn) {
        return ParamsConfig.getItem(sparkConn, ISparkConnGetter.PLUGIN_NAME);
    }

    String PLUGIN_NAME = "SparkConn";

  //  String KEY_CONN_YARN = "yarn";

    /**
     * example: spark://192.168.28.201:7077
     *
     * @param cfgDir
     * @return
     */
    public String getSparkMaster(File cfgDir);
}
