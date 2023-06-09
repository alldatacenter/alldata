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

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.hadoop.fs.Path;

public interface MockFileNames {

  String MOCK_FILE_NAME = "foo.csv";
  String MOCK_FILE_DIR_PATH = "/w/x/y";
  String MOCK_FILE_FQN = MOCK_FILE_DIR_PATH + "/" + MOCK_FILE_NAME;
  String MOCK_FILE_SYSTEM_NAME = "file:" + MOCK_FILE_FQN;
  Path MOCK_ROOT_PATH = new Path("file:/w");
  String MOCK_SUFFIX = "csv";
  String MOCK_DIR0 = "x";
  String MOCK_DIR1 = "y";
  Path MOCK_FILE_PATH = new Path(MOCK_FILE_SYSTEM_NAME);

  MajorType IMPLICIT_COL_TYPE = ImplicitColumnResolver.IMPLICIT_COL_TYPE;
  MajorType PARTITION_COL_TYPE = ImplicitColumnResolver.PARTITION_COL_TYPE;
}
