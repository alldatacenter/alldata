/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.dotdrill;

import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.fs.FileStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;

public class DotDrillFile {

  private final FileStatus status;
  private final DotDrillType type;
  private final DrillFileSystem fs;

  public static DotDrillFile create(DrillFileSystem fs, FileStatus status){
    for(DotDrillType d : DotDrillType.values()){
      if(!status.isDirectory() && d.matches(status)){
        return new DotDrillFile(fs, status, d);
      }
    }
    return null;
  }

  private DotDrillFile(DrillFileSystem fs, FileStatus status, DotDrillType type){
    this.fs = fs;
    this.status = status;
    this.type = type;
  }

  public DotDrillType getType(){
    return type;
  }

  /**
   * @return Return owner of the file in underlying file system.
   */
  public String getOwner() {
    if (type == DotDrillType.VIEW && status.getOwner().isEmpty()) {
      // Drill view S3AFileStatus is not populated with owner (it has default value of "").
      // This empty String causes IllegalArgumentException to be thrown (if impersonation is enabled) in
      // SchemaTreeProvider#createRootSchema(String, SchemaConfigInfoProvider). To work-around the issue
      // we can return current user as if they were the owner of the file (since they have access to it).
      return ImpersonationUtil.getProcessUserName();
    }
    return status.getOwner();
  }

  /**
   * Return base file name without the parent directory and extensions.
   * @return Base file name.
   */
  public String getBaseName() {
    final String fileName = status.getPath().getName();
    return fileName.substring(0, fileName.lastIndexOf(type.getEnding()));
  }

  public View getView(ObjectMapper mapper) throws IOException {
    Preconditions.checkArgument(type == DotDrillType.VIEW);
    try(InputStream is = fs.open(status.getPath())){
      return mapper.readValue(is, View.class);
    }
  }
}
