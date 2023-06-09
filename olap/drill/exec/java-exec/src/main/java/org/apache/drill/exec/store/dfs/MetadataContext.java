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
package org.apache.drill.exec.store.dfs;

import java.util.List;
import java.util.Map;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.fs.Path;

/**
 * A metadata context that holds state across multiple invocations of
 * the Parquet metadata APIs.
 */
public class MetadataContext {

  /**
   *  Map of directory path to the status of whether modification time was already checked.
   *  Note: the #directories is typically a small percentage of the #files, so the memory footprint
   *  is expected to be relatively small.
   */
  private Map<Path, Boolean> dirModifCheckMap = Maps.newHashMap();

  private PruneStatus pruneStatus = PruneStatus.NOT_STARTED;

  private boolean metadataCacheCorrupted;

  private List<Path> directories;

  public void setStatus(Path dir) {
    dirModifCheckMap.put(dir,  true);
  }

  public void clearStatus(Path dir) {
    dirModifCheckMap.put(dir,  false);
  }

  public boolean getStatus(Path dir) {
    if (dirModifCheckMap.containsKey(dir)) {
      return dirModifCheckMap.get(dir);
    }
    return false;
  }

  public void clear() {
    dirModifCheckMap.clear();
    metadataCacheCorrupted = false;
  }

  public void setPruneStatus(PruneStatus status) {
    pruneStatus = status;
  }

  public PruneStatus getPruneStatus() {
    return pruneStatus;
  }

  /**
   * @return true if parquet metadata cache files are missing or corrupted, false otherwise
   */
  public boolean isMetadataCacheCorrupted() {
    return metadataCacheCorrupted;
  }

  /**
   * Setting this as true allows to avoid double reading of corrupted, unsupported or missing metadata files
   *
   * @param metadataCacheCorrupted metadata corruption status
   */
  public void setMetadataCacheCorrupted(boolean metadataCacheCorrupted) {
    this.metadataCacheCorrupted = metadataCacheCorrupted;
  }

  public void setDirectories(List<Path> directories) {
    this.directories = directories;
  }

  public List<Path> getDirectories() {
    return directories;
  }

  public enum PruneStatus {
    NOT_STARTED,         // initial state
    PRUNED,              // partitions were pruned
    NOT_PRUNED           // partitions did not get pruned
  }

}
