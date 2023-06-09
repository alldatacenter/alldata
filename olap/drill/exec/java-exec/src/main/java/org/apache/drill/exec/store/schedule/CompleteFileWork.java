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
package org.apache.drill.exec.store.schedule;

import org.apache.drill.exec.store.dfs.easy.FileWork;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.hadoop.fs.Path;

public class CompleteFileWork implements FileWork, CompleteWork {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CompleteFileWork.class);

  private long start;
  private long length;
  private Path path;
  private EndpointByteMap byteMap;

  public CompleteFileWork(EndpointByteMap byteMap, long start, long length, Path path) {
    super();
    this.start = start;
    this.length = length;
    this.path = path;
    this.byteMap = byteMap;
  }

  @Override
  public int compareTo(CompleteWork o) {
    if(o instanceof CompleteFileWork){
      CompleteFileWork c = (CompleteFileWork) o;
      int cmp = path.compareTo(c.getPath());
      if(cmp != 0){
        return cmp;
      }

      cmp = Long.compare(start,  c.getStart());
      if(cmp != 0){
        return cmp;
      }

    }

    return Long.compare(getTotalBytes(), o.getTotalBytes());

  }

  @Override
  public long getTotalBytes() {
    return length;
  }

  @Override
  public EndpointByteMap getByteMap() {
    return byteMap;
  }

  @Override
  public Path getPath() {
    return path;
  }

  @Override
  public long getStart() {
    return start;
  }

  @Override
  public long getLength() {
    return length;
  }

  public FileWorkImpl getAsFileWork(){
    return new FileWorkImpl(start, length, path);
  }

  @Override
  public String toString() {
    return String.format("File: %s start: %d length: %d", path, start, length);
  }

  public static class FileWorkImpl implements FileWork {

    private long start;
    private long length;
    private Path path;

    @JsonCreator
    public FileWorkImpl(@JsonProperty("start") long start,
                        @JsonProperty("length") long length,
                        @JsonProperty("path") Path path) {
      this.start = start;
      this.length = length;
      this.path = path;
    }

    @Override
    public Path getPath() {
      return path;
    }

    @Override
    public long getStart() {
      return start;
    }

    @Override
    public long getLength() {
      return length;
    }

    @Override
    public String toString() {
      return "FileWorkImpl{" +
          "start=" + start +
          ", length=" + length +
          ", path=" + path +
          '}';
    }
  }
}
