/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.view.utils.hdfs;

import org.apache.hadoop.fs.FileEncryptionInfo;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.protocol.ErasureCodingPolicy;
import org.apache.hadoop.hdfs.protocol.HdfsFileStatus;

public abstract class DummyFileStatus extends FileStatus implements HdfsFileStatus {
    @Override
    public long getFileId() {
        return 0;
    }

    @Override
    public FileEncryptionInfo getFileEncryptionInfo() {
        return null;
    }

    @Override
    public byte[] getLocalNameInBytes() {
        return new byte[0];
    }

    @Override
    public byte[] getSymlinkInBytes() {
        return new byte[0];
    }

    @Override
    public int getChildrenNum() {
        return 0;
    }

    @Override
    public ErasureCodingPolicy getErasureCodingPolicy() {
        return null;
    }

    @Override
    public byte getStoragePolicy() {
        return 0;
    }

    @Override
    public void setPermission(FsPermission fsPermission) {

    }

    @Override
    public void setOwner(String s) {

    }

    @Override
    public void setGroup(String s) {

    }
}
