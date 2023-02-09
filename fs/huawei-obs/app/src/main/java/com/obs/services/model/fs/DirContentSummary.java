/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model.fs;

import java.util.List;

public class DirContentSummary {
    private String key;
    private boolean isTruncated;
    private String nextMarker;
    private long inode;
    private List<DirSummary> subDir;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getInode() {
        return inode;
    }

    public void setInode(long inode) {
        this.inode = inode;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public void setTruncated(boolean truncated) {
        isTruncated = truncated;
    }

    public String getNextMarker() {
        return nextMarker;
    }

    public void setNextMarker(String nextMarker) {
        this.nextMarker = nextMarker;
    }

    public List<DirSummary> getSubDir() {
        return subDir;
    }

    public void setSubDir(List<DirSummary> subDir) {
        this.subDir = subDir;
    }
}
