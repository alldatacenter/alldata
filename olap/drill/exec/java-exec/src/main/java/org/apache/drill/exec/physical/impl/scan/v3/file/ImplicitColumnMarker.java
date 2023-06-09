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
package org.apache.drill.exec.physical.impl.scan.v3.file;

import org.apache.drill.exec.store.ColumnExplorer.ImplicitFileColumns;
import org.apache.drill.exec.store.ColumnExplorer.ImplicitInternalFileColumns;

/**
 * Marks a column as implicit and provides a function to resolve an
 * implicit column given a description of the input file.
 */
public abstract class ImplicitColumnMarker {
  private int index = -1;

  public void setIndex(int index) {
    this.index = index;
  }

  public int index() { return index; }

  public abstract String resolve(FileDescrip fileInfo);

  /**
   * Marker for a file-based, non-internal implicit column that
   * extracts parts of the file name as defined by the implicit
   * column definition.
   */
  public static class FileImplicitMarker extends ImplicitColumnMarker {
    public final ImplicitFileColumns defn;

    public FileImplicitMarker(ImplicitFileColumns defn) {
      this.defn = defn;
    }

    @Override
    public String resolve(FileDescrip fileInfo) {
      return defn.getValue(fileInfo.filePath());
    }

    @Override
    public String toString() { return defn.name(); }
  }

  /**
   * Partition column defined by a partition depth from the scan
   * root folder. Partitions that reference non-existent directory levels
   * are null.
   */
  public static class PartitionColumnMarker extends ImplicitColumnMarker {
    private final int partition;

    public PartitionColumnMarker(int partition) {
      this.partition = partition;
    }

    @Override
    public String resolve(FileDescrip fileInfo) {
      return fileInfo.partition(partition);
    }

    @Override
    public String toString() { return "dir" + partition; }
  }

  public static class InternalColumnMarker extends ImplicitColumnMarker {
    public final ImplicitInternalFileColumns defn;

    public InternalColumnMarker(ImplicitInternalFileColumns defn) {
      this.defn = defn;
    }

    @Override
    public String resolve(FileDescrip fileInfo) {
      switch (defn) {
      case ROW_GROUP_INDEX:
        return valueOf(fileInfo.rowGroupIndex);
      case ROW_GROUP_START:
        return valueOf(fileInfo.rowGroupStart);
      case ROW_GROUP_LENGTH:
        return valueOf(fileInfo.rowGroupLength);
      case PROJECT_METADATA:
      case USE_METADATA:

        // Per Metadata code: if file is empty (and record is a placeholder)
        // return "false", else return null for valid rows.
        return fileInfo.isEmpty ? Boolean.FALSE.toString() : null;
      case LAST_MODIFIED_TIME:
        return fileInfo.getModTime();
      default:
        throw new IllegalStateException(defn.name());
      }
    }

    private String valueOf(Object value) {
      return value == null ? null : String.valueOf(value);
    }

    @Override
    public String toString() { return defn.name(); }
  }
}
