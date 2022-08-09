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

import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AmbariJpaPersistServiceTest {

  @Test
  public void start() {
    Injector injector = Guice.createInjector(
        new InMemoryDefaultTestModule()
    );

    AmbariJpaPersistService persistService = injector.getInstance(AmbariJpaPersistService.class);

    persistService.start();
    // This should not fail...
    persistService.start();
  }
}