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
package com.qlangtech.tis.sql.parser;

import com.google.common.collect.Lists;
import com.qlangtech.tis.fullbuild.IFullBuildContext;
import com.qlangtech.tis.manage.common.CenterResource;
import com.qlangtech.tis.manage.common.Config;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-15 13:43
 */
public class TopologyDir {

    public final File dir;

    public final String relativePath;

    public TopologyDir(File dir, String topologyName) {
        this.dir = dir;
        this.relativePath = IFullBuildContext.NAME_DATAFLOW_DIR + "/" + topologyName;
    }

    public File synchronizeRemoteRes(String resName) {
        // CenterResource.copyFromRemote2Local(url, localFile);
        return CenterResource.copyFromRemote2Local(CenterResource.getPath(relativePath, resName), true);
        // return localFile;
    }

    public List<File> synchronizeSubRemoteRes() {

        File localSubFileDir = getLocalSubFileDir();

        List<File> subs = Lists.newArrayList();
        Boolean[] localFileExistFlag = null;
        List<String> subFiles = CenterResource.getSubFiles(relativePath, false, true);
        if (CenterResource.notFetchFromCenterRepository()) {
            subFiles.forEach((sf) -> subs.add(new File(Config.getMetaCfgDir(), relativePath + File.separator + sf)));
            return subs;
        }

        Map<String, Boolean[]> localFileTag
                = localSubFileDir.exists()
                ? Arrays.stream(localSubFileDir.list((d, n) -> !n.endsWith(CenterResource.KEY_LAST_MODIFIED_EXTENDION)))
                .collect(Collectors.toMap((r) -> r, (r) -> new Boolean[]{Boolean.FALSE}))
                : Collections.emptyMap();

        for (String f : subFiles) {
            /*****************************
             * 同步远端文件
             *****************************/
            subs.add(synchronizeRemoteRes(f));

            localFileExistFlag = localFileTag.get(f);
            if (localFileExistFlag != null) {
                // 标记本地文件对应的远端文件存在
                localFileExistFlag[0] = true;
            }
        }

        localFileTag.entrySet().forEach((entry) -> {
            if (!entry.getValue()[0]) {
                // 本地文件对应的远端文件不存在，则需要将本地文件删除掉
                FileUtils.deleteQuietly(new File(localSubFileDir, entry.getKey()));
                FileUtils.deleteQuietly(new File(localSubFileDir, entry.getKey() + CenterResource.KEY_LAST_MODIFIED_EXTENDION));
            }
        });

        return subs;
    }

    public File getLocalSubFileDir() {
        return new File(Config.getMetaCfgDir(), relativePath);
    }

    public void delete() {
        try {
            FileUtils.forceDelete(this.dir);
        } catch (IOException e) {
            throw new RuntimeException("path:" + this.dir.getAbsolutePath(), e);
        }
    }
}
