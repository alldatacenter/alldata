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
package org.apache.drill.exec.store.sys.store;

/**
 * Holder for store version. By default version is {@link DataChangeVersion#UNDEFINED}.
 */
public class DataChangeVersion {

  // is used when store in unreachable
  public static final int NOT_AVAILABLE = -1;
  // is used when store does not support versioning
  public static final int UNDEFINED = -2;

  private int version = UNDEFINED;

  public void setVersion(int version) {
    this.version = version;
  }

  public int getVersion() {
    return version;
  }

}
