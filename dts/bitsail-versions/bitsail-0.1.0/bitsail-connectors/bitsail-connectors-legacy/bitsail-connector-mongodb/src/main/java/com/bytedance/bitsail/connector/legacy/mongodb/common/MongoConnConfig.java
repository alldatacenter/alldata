/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.mongodb.common;

import com.mongodb.MongoClientOptions;
import com.mongodb.WriteConcern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class MongoConnConfig implements Serializable {
  public enum CLIENT_MODE {
    URL, HOST_WITH_CREDENTIAL, HOST_WITHOUT_CREDENTIAL
  }

  public enum WRITE_MODE {
    INSERT, OVERWRITE
  }

  private CLIENT_MODE clientMode;
  private MongoConnOptions options;
  private String mongoUrl;
  private String hostsStr;
  private String host;
  private int port;
  private String collectionName;
  private String dbName;
  private String userName;
  private String password;

  private String authDbName;

  private WRITE_MODE writeMode;

  public MongoClientOptions getClientOption() {
    MongoClientOptions.Builder build = new MongoClientOptions.Builder();
    build.connectionsPerHost(this.options.getConnectionsPerHost());
    build.threadsAllowedToBlockForConnectionMultiplier(this.options.getThreadsAllowedToBlockForConnectionMultiplier());
    build.connectTimeout(options.getConnectTimeout());
    build.maxWaitTime(options.getMaxWaitTime());
    build.socketTimeout(options.getSocketTimeout());
    build.writeConcern(new WriteConcern(options.getWriteConcern()));
    return build.build();
  }
}

