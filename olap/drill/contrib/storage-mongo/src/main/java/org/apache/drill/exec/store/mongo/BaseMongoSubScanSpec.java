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
package org.apache.drill.exec.store.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(MongoSubScan.ShardedMongoSubScanSpec.class),
  @JsonSubTypes.Type(MongoSubScan.MongoSubScanSpec.class)
})
public class BaseMongoSubScanSpec {

  @JsonProperty
  private final String dbName;

  @JsonProperty
  private final String collectionName;

  @JsonProperty
  private final List<String> hosts;

  protected BaseMongoSubScanSpec(BaseMongoSubScanSpecBuilder<?> b) {
    this.dbName = b.dbName;
    this.collectionName = b.collectionName;
    this.hosts = b.hosts;
  }

  public String getDbName() {
    return this.dbName;
  }

  public String getCollectionName() {
    return this.collectionName;
  }

  public List<String> getHosts() {
    return this.hosts;
  }

  public static abstract class BaseMongoSubScanSpecBuilder<B extends BaseMongoSubScanSpecBuilder<B>> {
    private String dbName;

    private String collectionName;

    private List<String> hosts;

    public B dbName(String dbName) {
      this.dbName = dbName;
      return self();
    }

    public B collectionName(String collectionName) {
      this.collectionName = collectionName;
      return self();
    }

    public B hosts(List<String> hosts) {
      this.hosts = hosts;
      return self();
    }

    protected abstract B self();
  }
}
