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
package org.apache.drill.exec.store.sys;

import java.util.Iterator;

import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.store.pojo.NonNullable;

public class DrillbitIterator implements Iterator<Object> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillbitIterator.class);

  private Iterator<DrillbitEndpoint> endpoints;
  private DrillbitEndpoint current;

  public DrillbitIterator(ExecutorFragmentContext c) {
    this.endpoints = c.getBits().iterator();
    this.current = c.getEndpoint();
  }

  public static class DrillbitInstance {
    @NonNullable
    public String hostname;
    public int user_port;
    public int control_port;
    public int data_port;
    public int http_port;
    @NonNullable
    public boolean current;
    @NonNullable
    public String version;
    @NonNullable
    public String state;
  }

  @Override
  public boolean hasNext() {
    return endpoints.hasNext();
  }

  @Override
  public Object next() {
    DrillbitEndpoint ep = endpoints.next();
    DrillbitInstance i = new DrillbitInstance();
    i.current = isCurrent(ep);
    i.hostname = ep.getAddress();
    i.http_port = ep.getHttpPort();
    i.user_port = ep.getUserPort();
    i.control_port = ep.getControlPort();
    i.data_port = ep.getDataPort();
    i.version = ep.getVersion();
    i.state = ep.getState().toString();
    return i;
  }

  public boolean isCurrent(DrillbitEndpoint ep) {

    String epAddress = ep.getAddress();
    int epPort = ep.getUserPort();
    String currentEpAddress = current.getAddress();
    int currentEpPort = current.getUserPort();
    if (currentEpAddress.equals(epAddress) && currentEpPort == epPort) {
      return true;
    }
    return false;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
