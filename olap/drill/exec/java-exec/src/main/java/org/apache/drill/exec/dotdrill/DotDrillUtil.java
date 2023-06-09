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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.GlobPattern;
import org.apache.hadoop.fs.Path;

public class DotDrillUtil {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DotDrillUtil.class);

  /**
   * Returns List of DotDrillFile objects for given list of FileStatus objects matching the given Dot Drill File Types.
   * Return an empty list if no FileStatus matches the given Dot Drill File Types.
   * @param fs DrillFileSystem instance
   * @param statuses List of FileStatus objects
   * @param types Dot Drill Types to be matched
   * @return List of matched DotDrillFile objects
   */
  private static List<DotDrillFile> getDrillFiles(DrillFileSystem fs, List<FileStatus> statuses, DotDrillType... types){
    if (statuses == null) {
      return Collections.emptyList();
    }
    List<DotDrillFile> files = Lists.newArrayList();
    for(FileStatus s : statuses){
      DotDrillFile f = DotDrillFile.create(fs, s);
      if(f != null){
        if(types.length == 0){
          files.add(f);
        }else{
          for(DotDrillType t : types){
            if(t == f.getType()){
              files.add(f);
            }
          }
        }

      }
    }
    return files;
  }
  /**
   * Return list of DotDrillFile objects whose file name ends with .drill and matches the provided Drill Dot files types
   * in a given parent Path.
   * Return an empty list if no files matches the given Dot Drill File Types.
   * @param fs DrillFileSystem instance
   * @param root parent Path
   * @param types Dot Drill Types to be matched
   * @return List of matched DotDrillFile objects
   * @throws IOException
   */
  public static List<DotDrillFile> getDotDrills(DrillFileSystem fs, Path root, DotDrillType... types) throws IOException{
    return getDrillFiles(fs, getDrillFileStatus(fs, root,"*.drill"), types);
  }

  /**
   * Return list of DotDrillFile objects whose file name matches the provided name pattern and Drill Dot files types
   * in a given parent Path.
   * Return an empty list if no files matches the given file name and Dot Drill File Types.
   * @param fs DrillFileSystem instance
   * @param root parent Path
   * @param name name/pattern of the file
   * @param types Dot Drill Types to be matched
   * @return List of matched DotDrillFile objects
   * @throws IOException
   */
  public static List<DotDrillFile> getDotDrills(DrillFileSystem fs, Path root, String name, DotDrillType... types) throws IOException{
   return getDrillFiles(fs, getDrillFileStatus(fs, root, name, types), types);
  }

  /**
   * Return list of FileStatus objects matching '.drill' files for a given name in the parent path.
   *   a) If given name ends with '.drill', it return all '.drill' files's status matching the name pattern.
   *   b) If given name does not end with '.drill', it return file statues starting with name
   *      and ending with pattern matching
   *       1) all the valid DotDrillTypes if no DotDrillType is provided.
   *       2) given DotDrillTypes if DotDrillType is provided.
   * Return an empty list if no files matches the pattern and Drill Dot file types.
   * @param fs DrillFileSystem instance
   * @param root parent Path
   * @param name name/pattern of the file
   * @param types Dot Drill Types to be matched. Applies type matching only if name does not end with '.drill'
   * @return List of FileStatuses for files matching name and  Drill Dot file types.
   * @throws IOException  if any I/O error occurs when fetching file status
   */
  private static List<FileStatus> getDrillFileStatus(DrillFileSystem fs, Path root, String name, DotDrillType... types) throws IOException {
    List<FileStatus> statuses = new ArrayList<FileStatus>();

    if (name.endsWith(".drill")) {
      FileStatus[] status = fs.globStatus(new Path(root, name));
      if (status != null) {
        statuses.addAll(Arrays.asList(status));
      }
    } else {
      // If no DotDrillTypes are provided, check file status for all DotDrillTypes available.
      // Else check the file status for provided types.
      if (types.length == 0) {
        types = DotDrillType.values();
      }
      // Check if path has glob pattern or wildcards.If yes, use globStatus with globPattern for given types.
      GlobPattern pathGlob = new GlobPattern((new Path(root, name)).toString());
      if (pathGlob.hasWildcard()) {
        String patternAppliedName = name + DotDrillType.getDrillFileGlobPattern(types);
        FileStatus[] status = fs.globStatus(new Path(root, patternAppliedName));
        if (status != null) {
          statuses.addAll(Arrays.asList(status));
        }
      } else { // use list status if no glob_pattern/wildcards exist in path
        for (DotDrillType dotType : types) {
          try {
            FileStatus[] status = fs.listStatus(new Path(root, name + dotType.getEnding()));
            statuses.addAll(Arrays.asList(status));
          } catch (FileNotFoundException ex) {
          }
        }
      }
    }
    return statuses;
  }
}
