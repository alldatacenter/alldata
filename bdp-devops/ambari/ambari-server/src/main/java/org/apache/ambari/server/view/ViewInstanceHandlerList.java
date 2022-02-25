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
package org.apache.ambari.server.view;

import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.view.SystemException;
import org.eclipse.jetty.server.session.SessionCache;

/**
 * List of handlers for deployed view instances.
 */
public interface ViewInstanceHandlerList {

  /**
   * Add a handler for the given view instance.
   *
   * @param viewInstanceDefinition  the view instance
   *
   * @throws SystemException if a handler the view instance can not be added
   */
  void addViewInstance(ViewInstanceEntity viewInstanceDefinition) throws SystemException;

  /**
   * Shares specified sessionCache between all views' handlers
   * @param serverSessionCache the sessionCache instance
   */
  void shareSessionCacheToViews(SessionCache serverSessionCache);

  /**
   * Remove the handler for the given view instance.
   *
   * @param viewInstanceDefinition  the view instance
   */
  void removeViewInstance(ViewInstanceEntity viewInstanceDefinition);
}
