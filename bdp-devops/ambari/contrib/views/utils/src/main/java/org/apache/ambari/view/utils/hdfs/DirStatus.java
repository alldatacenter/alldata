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

import org.apache.hadoop.fs.FileStatus;

import java.util.Arrays;

public class DirStatus {
  private DirListInfo dirListInfo;
  private FileStatus [] fileStatuses;

  public DirStatus(FileStatus[] fileStatuses, DirListInfo dirListInfo) {
    this.fileStatuses = fileStatuses;
    this.dirListInfo = dirListInfo;
  }

  public DirListInfo getDirListInfo() {
    return dirListInfo;
  }

  public void setDirListInfo(DirListInfo dirListInfo) {
    this.dirListInfo = dirListInfo;
  }

  public FileStatus[] getFileStatuses() {
    return fileStatuses;
  }

  public void setFileStatuses(FileStatus[] fileStatuses) {
    this.fileStatuses = fileStatuses;
  }

  @Override
  public String toString() {
    return "DirStatus{" +
        "dirListInfo=" + dirListInfo +
        ", fileStatuses=" + Arrays.toString(fileStatuses) +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DirStatus dirStatus = (DirStatus) o;

    if (dirListInfo != null ? !dirListInfo.equals(dirStatus.dirListInfo) : dirStatus.dirListInfo != null) return false;
    // Probably incorrect - comparing Object[] arrays with Arrays.equals
    return Arrays.equals(fileStatuses, dirStatus.fileStatuses);
  }

  @Override
  public int hashCode() {
    int result = dirListInfo != null ? dirListInfo.hashCode() : 0;
    result = 31 * result + Arrays.hashCode(fileStatuses);
    return result;
  }
}
