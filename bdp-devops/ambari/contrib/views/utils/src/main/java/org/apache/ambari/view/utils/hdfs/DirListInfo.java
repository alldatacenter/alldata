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

public class DirListInfo {
  private int originalSize;
  private boolean truncated;
  private int finalSize;
  private String nameFilter;

  public DirListInfo(int originalSize, boolean truncated, int finalSize, String nameFilter) {
    this.originalSize = originalSize;
    this.truncated = truncated;
    this.finalSize = finalSize;
    this.nameFilter = nameFilter;
  }

  public int getOriginalSize() {
    return originalSize;
  }

  public void setOriginalSize(int originalSize) {
    this.originalSize = originalSize;
  }

  public boolean isTruncated() {
    return truncated;
  }

  public void setTruncated(boolean truncated) {
    this.truncated = truncated;
  }

  public int getFinalSize() {
    return finalSize;
  }

  public void setFinalSize(int finalSize) {
    this.finalSize = finalSize;
  }

  public String getNameFilter() {
    return nameFilter;
  }

  public void setNameFilter(String nameFilter) {
    this.nameFilter = nameFilter;
  }

  @Override
  public String toString() {
    return "DirListInfo{" +
        "originalSize=" + originalSize +
        ", truncated=" + truncated +
        ", finalSize=" + finalSize +
        ", nameFilter='" + nameFilter + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DirListInfo that = (DirListInfo) o;

    if (originalSize != that.originalSize) return false;
    if (truncated != that.truncated) return false;
    if (finalSize != that.finalSize) return false;
    return nameFilter != null ? nameFilter.equals(that.nameFilter) : that.nameFilter == null;
  }

  @Override
  public int hashCode() {
    int result = originalSize;
    result = 31 * result + (truncated ? 1 : 0);
    result = 31 * result + finalSize;
    result = 31 * result + (nameFilter != null ? nameFilter.hashCode() : 0);
    return result;
  }
}
