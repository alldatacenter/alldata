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
package org.apache.drill.exec.store.mock;

import org.apache.drill.common.logical.StoragePluginConfig;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName(MockStorageEngineConfig.NAME)
public class MockStorageEngineConfig extends StoragePluginConfig {
  public static final String NAME = "mock";
  public static final MockStorageEngineConfig INSTANCE = new MockStorageEngineConfig("mock:///");

  private final String url;

  @JsonCreator
  public MockStorageEngineConfig(@JsonProperty("url") String url) {
    this.url = url;
  }

  public String getUrl() {
    return url;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MockStorageEngineConfig that = (MockStorageEngineConfig) o;

    if (url != null ? !url.equals(that.url) : that.url != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    return url != null ? url.hashCode() : 0;
  }
}
