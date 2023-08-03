/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.data;

import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Objects;

/**
 * Interface can get iceberg sequenceNumber
 */
public abstract class IcebergContentFile<F> implements ContentFile<F>, Serializable {
  private long sequenceNumber;

  public IcebergContentFile(long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  public boolean isDataFile() {
    return content() == FileContent.DATA;
  }

  public boolean isDeleteFile() {
    return !isDataFile();
  }

  public IcebergDataFile asDataFile() {
    Preconditions.checkArgument(isDataFile(), "Not a data file");
    return (IcebergDataFile) this;
  }
  
  public abstract F internalFile();

  public IcebergDeleteFile asDeleteFile() {
    Preconditions.checkArgument(isDeleteFile(), "Not a delete file");
    return (IcebergDeleteFile) this;
  }

  public static IcebergContentFile<?> wrap(ContentFile<?> contentFile, long sequenceNumber) {
    if (contentFile instanceof DataFile) {
      if (contentFile instanceof IcebergDataFile) {
        return (IcebergDataFile) contentFile;
      } else {
        return new IcebergDataFile((DataFile) contentFile, sequenceNumber);
      }
    } else if (contentFile instanceof DeleteFile) {
      if (contentFile instanceof IcebergDeleteFile) {
        return (IcebergDeleteFile) contentFile;
      } else {
        return new IcebergDeleteFile((DeleteFile) contentFile, sequenceNumber);
      }
    } else {
      throw new IllegalArgumentException("Only support DataFile or DeleteFile, can not support: " +
          contentFile.getClass().getSimpleName());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    return Objects.equals(path(), ((IcebergContentFile<?>) o).path());
  }

  @Override
  public int hashCode() {
    return Objects.hash(path());
  }
}
