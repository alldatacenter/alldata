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
package org.apache.drill.exec.testing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.concurrent.ExtendedLatch;

/**
 * See {@link org.apache.drill.exec.testing.CountDownLatchInjection} Degenerates to
 * {@link org.apache.drill.exec.testing.PauseInjection#pause}, if initialized to zero count. In any case, this injection
 * provides more control than PauseInjection.
 */
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class CountDownLatchInjectionImpl extends Injection implements CountDownLatchInjection {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CountDownLatchInjectionImpl.class);

  private ExtendedLatch latch = null;

  @JsonCreator // ensures instances are created only through JSON
  private CountDownLatchInjectionImpl(@JsonProperty("address") final String address,
                                      @JsonProperty("port") final int port,
                                      @JsonProperty("siteClass") final String siteClass,
                                      @JsonProperty("desc") final String desc) throws InjectionConfigurationException {
    super(address, port, siteClass, desc, 0, 1, 0L);
  }

  @Override
  protected boolean injectNow() {
    return true;
  }

  @Override
  public void initialize(final int count) {
    Preconditions.checkArgument(latch == null, "Latch can be initialized only once at %s in %s.", desc,
      siteClass.getSimpleName());
    Preconditions.checkArgument(count > 0, "Count has to be a positive integer at %s in %s.", desc,
      siteClass.getSimpleName());
    latch = new ExtendedLatch(count);
  }

  @Override
  public void await() throws InterruptedException {
    Preconditions.checkNotNull(latch, "Latch not initialized in %s at %s.", siteClass.getSimpleName(), desc);
    try {
      latch.await();
    } catch (final InterruptedException e) {
      logger.warn("Interrupted while awaiting in {} at {}.", siteClass.getSimpleName(), desc);
      throw e;
    }
  }

  @Override
  public void awaitUninterruptibly() {
    Preconditions.checkNotNull(latch, "Latch not initialized in %s at %s.", siteClass.getSimpleName(), desc);
    latch.awaitUninterruptibly();
  }

  @Override
  public void countDown() {
    Preconditions.checkNotNull(latch, "Latch not initialized in %s at %s.", siteClass.getSimpleName(), desc);
    Preconditions.checkArgument(latch.getCount() > 0, "Counting down on latch more than intended.");
    latch.countDown();
  }

  @Override
  public void close() {
    latch = null;
  }
}
