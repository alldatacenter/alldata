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
package org.apache.ambari.server.cleanup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.orm.dao.Cleanable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;

/**
 * Configuration module for the cleanup framework.
 */
public class CleanupModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(CleanupModule.class);

  /**
   * Selectors identifying objects to be bound.
   *
   * @return a list with interface and annotation types
   */
  protected List<Class<?>> getSelectors() {
    List<Class<?>> selectorList = new ArrayList<>();
    selectorList.add(Cleanable.class);
    return selectorList;
  }

  /**
   * Gets the list of types to be excluded from bindings.
   *
   * @return a list with types to be left out from dynamic bindings
   */
  protected List<Class<?>> getExclusions() {
    return Collections.emptyList();
  }

  /**
   * Returns the package to be scanned for bindings of this module.
   *
   * @return the name of the package to be scanned
   */
  protected String getPackageToScan() {
    return Cleanable.class.getPackage().getName();
  }


  @Override
  protected void configure() {

    Multibinder<Cleanable> multiBinder = Multibinder.newSetBinder(binder(), Cleanable.class);
    Set<Class<?>> bindingSet = ClasspathScannerUtils.findOnClassPath(getPackageToScan(), getExclusions(), getSelectors());
    for (Class clazz : bindingSet) {
      LOG.info("Binding cleaner {}", clazz);
      multiBinder.addBinding().to(clazz).in(Scopes.SINGLETON);
    }

    bind(CleanupServiceImpl.class).in(Scopes.SINGLETON);
  }
}
