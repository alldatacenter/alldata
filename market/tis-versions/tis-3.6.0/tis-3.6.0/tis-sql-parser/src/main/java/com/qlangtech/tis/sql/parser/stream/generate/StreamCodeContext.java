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
package com.qlangtech.tis.sql.parser.stream.generate;

import com.qlangtech.tis.manage.common.Config;
import com.qlangtech.tis.manage.common.incr.StreamContextConstant;
import org.apache.commons.lang.StringUtils;

import java.io.File;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2020/04/13
 */
public class StreamCodeContext {

    protected final String collectionName;

    protected final long timestamp;

    protected final File incrScriptDir;

    protected final boolean incrScriptDirCreated;

    public StreamCodeContext(String collectionName, long timestamp) {
        this.collectionName = collectionName;
        this.timestamp = timestamp;
        try {
            incrScriptDir = getScalaStreamScriptDir(this.collectionName, this.timestamp);
            this.incrScriptDirCreated = incrScriptDir.exists();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 存放增量脚本父目录
     *
     * @return
     */
    public File getIncrScriptDir() {
        return this.incrScriptDir;
    }

    public boolean isIncrScriptDirCreated() {
        return this.incrScriptDirCreated;
    }

    public static File getScalaStreamScriptDir(String collectionName, long timestamp) throws Exception {
        File dir = new File(StreamContextConstant.getStreamScriptRootDir(collectionName, timestamp)
                , "/src/main/scala/" + StringUtils.replace(Config.getGenerateParentPackage(), ".", "/") + "/" + collectionName);
        return dir;
    }
}
