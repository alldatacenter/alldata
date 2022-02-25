/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.inject.persist.jpa;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;

/**
 * Override non-public class limitations as we need non-interface method
 */
public class AmbariJpaPersistService extends JpaPersistService {

  private final AtomicBoolean jpaStarted = new AtomicBoolean(false);

  @Inject
  public AmbariJpaPersistService(@Jpa String persistenceUnitName, @Jpa Map<?, ?> persistenceProperties) {
    super(persistenceUnitName, persistenceProperties);
  }

  /**
   * Starts the PersistService if it has not been previously started.
   */
  @Override
  public synchronized void start() {
    if (!jpaStarted.get()) {
      super.start();
      jpaStarted.set(true);
    }
  }

  /**
   * Returns whether JPA has been started or not
   *
   * @return <code>true</code> if JPA has been started; <code>false</code> if JPA has not been started
   */
  public boolean isStarted() {
    return jpaStarted.get();
  }

}
