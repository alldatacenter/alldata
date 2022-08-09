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
package org.apache.ambari.server.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ambari.server.api.AmbariPersistFilter;
import org.apache.ambari.server.orm.entities.ViewEntity;
import org.apache.ambari.server.orm.entities.ViewInstanceEntity;
import org.apache.ambari.server.security.AmbariViewsSecurityHeaderFilter;
import org.apache.ambari.server.view.ViewContextImpl;
import org.apache.ambari.server.view.ViewInstanceHandlerList;
import org.apache.ambari.server.view.ViewRegistry;
import org.apache.ambari.view.SystemException;
import org.apache.ambari.view.ViewContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.session.SessionCache;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * An Ambari specific extension of the FailsafeHandlerList that allows for the addition
 * of view instances as handlers.
 */
@Singleton
public class AmbariHandlerList extends HandlerCollection implements ViewInstanceHandlerList {

  /**
   * The target pattern for a view resource request.
   */
  private static final Pattern VIEW_RESOURCE_TARGET_PATTERN =
    Pattern.compile("/api/(\\S+)/views/(\\S+)/versions/(\\S+)/instances/(\\S+)/resources/(\\S+)");

  /**
   * The view registry.
   */
  @Inject
  ViewRegistry viewRegistry;

  /**
   * Session manager.
   */
  @Inject
  SessionHandler sessionHandler;

  /**
   * The web app context provider.
   */
  @Inject
  Provider<WebAppContext> webAppContextProvider;

  /**
   * The persistence filter.
   */
  @Inject
  AmbariPersistFilter persistFilter;

  /**
   * The security filter.
   */
  @Inject
  DelegatingFilterProxy springSecurityFilter;

  /**
   * The security header filter - conditionally adds security-related headers to the HTTP response for Ambari Views requests.
   */
  @Inject
  AmbariViewsSecurityHeaderFilter ambariViewsSecurityHeaderFilter;

  @Inject
  SessionHandlerConfigurer sessionHandlerConfigurer;

  /**
   * Mapping of view instance entities to handlers.
   */
  private final Map<ViewInstanceEntity, WebAppContext> viewHandlerMap = new HashMap<>();

  /**
   * The non-view handlers.
   */
  private final Collection<Handler> nonViewHandlers = new HashSet<>();

  private static final Logger LOG = LoggerFactory.getLogger(AmbariHandlerList.class);


  // ----- Constructors ------------------------------------------------------

  /**
   * Construct an AmbariHandlerList.
   */
  public AmbariHandlerList() {
    super(true);
  }


  // ----- HandlerCollection -------------------------------------------------

  @Override
  public void handle(String target, Request baseRequest,
                     HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

    ViewEntity viewEntity = getTargetView(target);

    if (viewEntity == null) {
      processHandlers(target, baseRequest, request, response);
    } else {
      // if there is a view target (as in a view resource request) then set the view class loader
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      try {
        ClassLoader viewClassLoader = viewEntity.getClassLoader();
        if (viewClassLoader == null) {
          LOG.debug("No class loader associated with view {}.", viewEntity.getName());
        } else {
          Thread.currentThread().setContextClassLoader(viewClassLoader);
        }
        processHandlers(target, baseRequest, request, response);
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    }
  }

  @Override
  public void addHandler(Handler handler) {
    nonViewHandlers.add(handler);
    super.addHandler(handler);
  }

// ----- ViewInstanceHandler -----------------------------------------------

  @Override
  public void addViewInstance(ViewInstanceEntity viewInstanceDefinition) throws SystemException {
    WebAppContext handler = getHandler(viewInstanceDefinition);
    viewHandlerMap.put(viewInstanceDefinition, handler);
    super.addHandler(handler);
    // if this is running then start the handler being added...
    if(!isStopped() && !isStopping()) {
      try {
        handler.start();
      } catch (Exception e) {
        throw new SystemException("Caught exception adding a view instance.", e);
      }
    }
    handler.getSessionHandler().setSessionCache(sessionHandler.getSessionCache());
  }

  @Override
  public void shareSessionCacheToViews(SessionCache serverSessionCache) {
    for (WebAppContext webAppContext : viewHandlerMap.values()) {
      webAppContext.getSessionHandler().setSessionCache(serverSessionCache);
    }
  }

  @Override
  public void removeViewInstance(ViewInstanceEntity viewInstanceDefinition) {
    Handler handler = viewHandlerMap.get(viewInstanceDefinition);
    if (handler != null) {
      viewHandlerMap.remove(viewInstanceDefinition);
      removeHandler(handler);
    }
  }


  // ----- helper methods ----------------------------------------------------

  // call the handlers until the request is handled
  private void processHandlers(String target, Request baseRequest,
                               HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

    final Handler[] handlers = getHandlers();

    if (handlers != null && isStarted()) {
      if (!processHandlers(viewHandlerMap.values(), target, baseRequest, request, response)) {
        processHandlers(nonViewHandlers, target, baseRequest, request, response);
      }
    }
  }

  // call the given handlers until the request is handled; return true if the request is handled
  private boolean processHandlers(Collection<? extends Handler> handlers, String target, Request baseRequest,
                                  HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException {

    for (Handler handler : handlers) {
      handler.handle(target, baseRequest, request, response);
      if (baseRequest.isHandled()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Get a Handler for the given view instance.
   *
   * @param viewInstanceDefinition  the view instance definition
   *
   * @return a handler
   *
   * @throws org.apache.ambari.view.SystemException if an handler can not be obtained for the given view instance
   */
  private WebAppContext getHandler(ViewInstanceEntity viewInstanceDefinition)
    throws SystemException {

    ViewEntity    viewDefinition = viewInstanceDefinition.getViewEntity();
    WebAppContext webAppContext  = webAppContextProvider.get();

    webAppContext.setWar(viewDefinition.getArchive());
    webAppContext.setContextPath(viewInstanceDefinition.getContextPath());
    webAppContext.setClassLoader(viewInstanceDefinition.getViewEntity().getClassLoader());
    webAppContext.setAttribute(ViewContext.CONTEXT_ATTRIBUTE, new ViewContextImpl(viewInstanceDefinition, viewRegistry));
    webAppContext.setSessionHandler(new SharedSessionHandler(sessionHandler));
    webAppContext.addFilter(new FilterHolder(ambariViewsSecurityHeaderFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.addFilter(new FilterHolder(persistFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.addFilter(new FilterHolder(springSecurityFilter), "/*", AmbariServer.DISPATCHER_TYPES);
    webAppContext.setAllowNullPathInfo(true);

    return webAppContext;
  }

  /**
   * Get the view that is the target of the request; null if not a view request.
   *
   * @param target  the target of the request
   *
   * @return the view target; null if none
   */
  private ViewEntity getTargetView(String target) {
    Matcher matcher = VIEW_RESOURCE_TARGET_PATTERN.matcher(target);

    return matcher.matches() ? viewRegistry.getDefinition(matcher.group(2), matcher.group(3)) : null;
  }


  // ----- inner class : SharedSessionHandler --------------------------------

  /**
   * A session handler that shares its session manager with another app.
   * This handler DOES NOT attempt stop the shared session manager.
   */
  private class SharedSessionHandler extends SessionHandler {

    // ----- Constructors ----------------------------------------------------

    /**
     * Construct a SharedSessionHandler.
     *
     * @param sessionHandler  the shared session manager.
     */
    public SharedSessionHandler(SessionHandler sessionHandler) {
      setSessionIdManager(sessionHandler.getSessionIdManager());
      sessionHandlerConfigurer.configureSessionHandler(this);
    }


    // ----- SessionHandler --------------------------------------------------

    @Override
    protected void doStop() throws Exception {
      // do nothing...
    }
  }
}
