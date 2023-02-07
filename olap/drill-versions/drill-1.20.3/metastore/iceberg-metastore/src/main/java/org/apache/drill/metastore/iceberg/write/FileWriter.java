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
package org.apache.drill.metastore.iceberg.write;

import org.apache.iceberg.data.Record;

import java.util.List;

/**
 * Generic interface that provides functionality to write data into the file.
 * File format will be determined by the implementation.
 */
public interface FileWriter {

  /**
   * Indicates list of records to be written.
   *
   * @param records list of records to be written into file
   * @return current File Writer instance
   */
  FileWriter records(List<Record> records);

  /**
   * Indicates location where new file will be written to.
   *
   * @param location file location
   * @return current File Writer instance
   */
  FileWriter location(String location);

  /**
   * Indicates name with which new file will be created.
   *
   * @param name file name
   * @return current File Writer instance
   */
  FileWriter name(String name);

  /**
   * Writes provided list of records into the file.
   *
   * @return written file information holder
   */
  File write();
}
