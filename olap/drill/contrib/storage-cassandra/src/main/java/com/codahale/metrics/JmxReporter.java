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
package com.codahale.metrics;

import java.io.Closeable;

/**
 * Adapter for compatibility of metrics-jms for 3 and 4 versions.
 */
public class JmxReporter implements Reporter, Closeable {

  private final com.codahale.metrics.jmx.JmxReporter delegate;

  public JmxReporter(com.codahale.metrics.jmx.JmxReporter delegate) {
    this.delegate = delegate;
  }

  public static Builder forRegistry(MetricRegistry registry) {
    return new Builder(registry);
  }

  public void start() {
    delegate.start();
  }

  public void stop() {
    delegate.stop();
  }

  @Override
  public void close() {
    delegate.close();
  }

  public static class Builder {
    private final com.codahale.metrics.jmx.JmxReporter.Builder delegate;

    public Builder(MetricRegistry registry) {
      delegate = com.codahale.metrics.jmx.JmxReporter.forRegistry(registry);
    }

    public Builder inDomain(String domain) {
      delegate.inDomain(domain);
      return this;
    }

    public JmxReporter build() {
      return new JmxReporter(delegate.build());
    }
  }
}
