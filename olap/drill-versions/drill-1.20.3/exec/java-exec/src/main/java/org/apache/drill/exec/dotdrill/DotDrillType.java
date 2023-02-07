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

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

public enum DotDrillType {
  VIEW,
  STATS;

  private final String ending;

  private DotDrillType(){
    this.ending = '.' + name().toLowerCase() + ".drill";
  }

  public boolean matches(FileStatus status){
    return status.getPath().getName().endsWith(ending);
  }

  /**
   * For a given parent directory and base file name return complete path including file type specific extensions.
   *
   * @param parentDir Directory where the DotDrillFile is stored.
   * @param name Base file name of the DotDrillFile.
   * @return Path including the extensions that can be used to read/write in filesystem.
   */
  public Path getPath(String parentDir, String name) {
    return new Path(parentDir, name + ending);
  }

  /**
   * Return extension string of file type represented by this object.
   *
   * @return File extension.
   */
  public String getEnding() {
    return ending;
  }

  /**
   * Return Glob pattern for given Dot Drill Types.
   * @param types
   * @return Glob pattern representing For Dot Drill Types provided as types param
   */
  public static String getDrillFileGlobPattern(DotDrillType[] types) {
    if (types.length == 1) {
      return "." + types[0].name().toLowerCase() + ".drill";
    }

    StringBuffer b = new StringBuffer();
    b.append(".{");
    for (DotDrillType d : types) {
      if (b.length() > 2) {
        b.append(',');
      }
      b.append(d.name().toLowerCase());
    }
    b.append("}.drill");
    return b.toString();
  }

  public static final String DOT_DRILL_GLOB;

  static{
    StringBuffer b = new StringBuffer();
    b.append(".{");
    for(DotDrillType d : values()){
      if(b.length() > 2){
        b.append(',');
      }
      b.append(d.name().toLowerCase());
    }
    b.append("}.drill");
    DOT_DRILL_GLOB = b.toString();
  }
}
