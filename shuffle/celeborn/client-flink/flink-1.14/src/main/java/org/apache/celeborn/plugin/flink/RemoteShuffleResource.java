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

package org.apache.celeborn.plugin.flink;

public class RemoteShuffleResource implements ShuffleResource {

  private static final long serialVersionUID = 6497939083185255973L;

  private final String rssMetaServiceHost;
  private final int rssMetaServicePort;

  private ShuffleResourceDescriptor shuffleResourceDescriptor;

  public RemoteShuffleResource(
      String rssMetaServiceHost,
      int rssMetaServicePort,
      ShuffleResourceDescriptor remoteShuffleDescriptor) {
    this.rssMetaServiceHost = rssMetaServiceHost;
    this.rssMetaServicePort = rssMetaServicePort;
    this.shuffleResourceDescriptor = remoteShuffleDescriptor;
  }

  @Override
  public ShuffleResourceDescriptor getMapPartitionShuffleDescriptor() {
    return shuffleResourceDescriptor;
  }

  public String getRssMetaServiceHost() {
    return rssMetaServiceHost;
  }

  public int getRssMetaServicePort() {
    return rssMetaServicePort;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("RemoteShuffleResource{");
    sb.append("rssMetaServiceHost='").append(rssMetaServiceHost).append('\'');
    sb.append(", rssMetaServicePort=").append(rssMetaServicePort);
    sb.append(", shuffleResourceDescriptor=").append(shuffleResourceDescriptor);
    sb.append('}');
    return sb.toString();
  }
}
