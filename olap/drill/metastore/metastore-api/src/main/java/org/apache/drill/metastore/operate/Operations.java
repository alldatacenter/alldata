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
package org.apache.drill.metastore.operate;

/**
 * Each Metastore component must provide mechanisms which return
 * component metadata, allow reading / writing data from / into Metastore.
 *
 * @param <T> component unit type
 */
public interface Operations<T> {

  /**
   * @return component metadata instance
   */
  Metadata metadata();

  /**
   * @return component read mechanism implementation
   */
  Read<T> read();

  /**
   * @return component write mechanism implementation
   */
  Modify<T> modify();
}
