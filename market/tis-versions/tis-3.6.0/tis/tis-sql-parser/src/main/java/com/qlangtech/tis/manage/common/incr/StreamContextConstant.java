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
package com.qlangtech.tis.manage.common.incr;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.plugin.ds.DBConfig;
import com.qlangtech.tis.sql.parser.DBNode;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StreamContextConstant {

    // streamscript
    public static final String DIR_STREAMS_SCRIPT = "streamscript";

    public static final String DIR_META = "meta";

    public static final String DIR_DAO = "dao";

    public static final String FILE_DB_DEPENDENCY_CONFIG = "db_dependency_config.yaml";

    public static String getDbDependencyConfigFilePath(String collection, long timestamp) {
        return StreamContextConstant.DIR_STREAMS_SCRIPT + "/" + collection + "/" + timestamp + "/" + StreamContextConstant.DIR_META + "/" + StreamContextConstant.FILE_DB_DEPENDENCY_CONFIG;
    }

    public static File getDAORootDir(String dbName, long timestamp) {
        return new File(Config.getMetaCfgDir(), getDAORootPath(dbName, timestamp));
    }

    public static File getDAOJarFile(DBNode dbNode) {
        return new File(getDAORootDir(dbNode.getDbName(), dbNode.getTimestampVer()), DBConfig.getDAOJarName(dbNode.getDbName()));
    }

    public static String getDAORootPath(String dbName, long timestamp) {
        if (timestamp < 1) {
            throw new IllegalArgumentException("param timestamp:" + timestamp + " can not small than 1");
        }
        return (DIR_DAO + "/" + DBConfig.getFormatDBName(dbName) + "/" + timestamp);
    }

    public static File getStreamScriptRootDir(String collectionName, long timestamp) {
        return new File(Config.getMetaCfgDir() + "/" + DIR_STREAMS_SCRIPT + "/" + collectionName + "/" + timestamp);
    }

    public static String getIncrStreamJarName(String collection) {
        return StringUtils.lowerCase(collection + "-incr.jar");
    }

    public static File getIncrStreamJarFile(String collection, long timestamp) {
        return new File(getStreamScriptRootDir(collection, timestamp), getIncrStreamJarName(collection));
    }

    /**
     * db 依赖版本配置依赖元数据
     *
     * @return
     */
    public static File getDbDependencyConfigMetaFile(String collectionName, long incrScriptTimestamp) {
        return new File(StreamContextConstant.getStreamScriptRootDir(collectionName, incrScriptTimestamp), StreamContextConstant.DIR_META + "/" + StreamContextConstant.FILE_DB_DEPENDENCY_CONFIG);
    }
}
