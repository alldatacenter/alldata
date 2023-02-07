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
package org.apache.drill.metastore.metadata;

import org.apache.hadoop.fs.Path;

/**
 * A metadata which has specific location.
 */
public interface LocationProvider {

  /**
   * Returns path of the metadata.
   * For files and row groups - full path to the file including file name.
   * For segments - path to the segment directory.
   *
   * @return metadata path
   */
  Path getPath();

  /**
   * Returns location of the metadata.
   * For files and row groups - path to the parent directory they reside it.
   * For segments - path to the segment directory.
   *
   * @return metadata location
   */
  Path getLocation();
}
