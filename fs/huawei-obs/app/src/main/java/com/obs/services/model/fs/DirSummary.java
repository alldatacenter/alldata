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

public class DirSummary {
    private String name;
    private long dirCount;
    private long fileCount;
    private long fileSize;
    private long inode;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getDirCount() {
        return dirCount;
    }

    public void setDirCount(long dirCount) {
        this.dirCount = dirCount;
    }

    public long getFileCount() {
        return fileCount;
    }

    public void setFileCount(long fileCount) {
        this.fileCount = fileCount;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getInode() {
        return inode;
    }

    public void setInode(long inode) {
        this.inode = inode;
    }

    @Override
    public String toString() {
        return "DirSummary{" +
                "name='" + name + '\'' +
                ", dirCount=" + dirCount +
                ", fileCount=" + fileCount +
                ", fileSize=" + fileSize +
                ", inode=" + inode +
                '}';
    }
}
