/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.audit.request.RequestAuditLogger;
import org.apache.ambari.server.audit.request.RequestAuditLoggerImpl;
import org.apache.ambari.server.audit.request.eventcreator.RequestAuditEventCreator;
import org.apache.ambari.server.cleanup.ClasspathScannerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

public class AuditLoggerModule extends AbstractModule {
  private static final Logger LOG = LoggerFactory.getLogger(AuditLoggerModule.class);

  /**
   * Selectors identifying objects to be bound.
   *
   * @return a list with interface and annotation types
   */
  protected List<Class<?>> getSelectors() {
    List<Class<?>> selectorList = new ArrayList<>();
    selectorList.add(RequestAuditEventCreator.class);
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
    return RequestAuditEventCreator.class.getPackage().getName();
  }

  @Override
  protected void configure() {
    bind(AuditLogger.class).to(AsyncAuditLogger.class);

    // set AuditLoggerDefaultImpl to be used by AsyncAuditLogger
    bind(AuditLogger.class).annotatedWith(Names.named(AsyncAuditLogger.InnerLogger)).to(AuditLoggerDefaultImpl.class);

    // binding for audit event creators
    Multibinder<RequestAuditEventCreator> multiBinder = Multibinder.newSetBinder(binder(), RequestAuditEventCreator.class);
    Set<Class<?>> bindingSet = ClasspathScannerUtils.findOnClassPath(getPackageToScan(), getExclusions(), getSelectors());

    for (Class clazz : bindingSet) {
      LOG.info("Binding audit event creator {}", clazz);
      multiBinder.addBinding().to(clazz).in(Scopes.SINGLETON);
    }

    bind(RequestAuditLogger.class).to(RequestAuditLoggerImpl.class);
  }
}
