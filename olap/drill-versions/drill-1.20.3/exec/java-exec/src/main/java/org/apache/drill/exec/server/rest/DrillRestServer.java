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
package org.apache.drill.exec.server.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jaxrs.base.JsonParseExceptionMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.core.HTMLOutputFormat;
import freemarker.template.Configuration;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import io.netty.util.concurrent.EventExecutor;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.rest.WebUserConnection.AnonWebUserConnection;
import org.apache.drill.exec.server.rest.auth.AuthDynamicFeature;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.AnonDrillUserPrincipal;
import org.apache.drill.exec.server.rest.profile.ProfileResources;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.work.WorkManager;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

public class DrillRestServer extends ResourceConfig {
  static final Logger logger = LoggerFactory.getLogger(DrillRestServer.class);

  public DrillRestServer(final WorkManager workManager, final ServletContext servletContext, final Drillbit drillbit) {
    register(DrillRoot.class);
    register(StatusResources.class);
    register(StorageResources.class);
    register(ProfileResources.class);
    register(QueryResources.class);
    register(MetricsResources.class);
    register(ThreadsResources.class);
    register(LogsResources.class);

    property(FreemarkerMvcFeature.TEMPLATE_OBJECT_FACTORY, getFreemarkerConfiguration(servletContext));
    register(FreemarkerMvcFeature.class);

    register(MultiPartFeature.class);
    property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);

    final boolean isAuthEnabled =
        workManager.getContext().getConfig().getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED);

    if (isAuthEnabled) {
      register(LogInLogOutResources.class);
      register(AuthDynamicFeature.class);
      register(RolesAllowedDynamicFeature.class);
    }

    //disable moxy so it doesn't conflict with jackson.
    final String disableMoxy = PropertiesHelper.getPropertyNameForRuntime(CommonProperties.MOXY_JSON_FEATURE_DISABLE,
        getConfiguration().getRuntimeType());
    property(disableMoxy, true);

    register(JsonParseExceptionMapper.class);
    register(JsonMappingExceptionMapper.class);
    register(GenericExceptionMapper.class);

    JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
    provider.setMapper(workManager.getContext().getLpPersistence().getMapper());
    register(provider);

    // Get an EventExecutor out of the BitServer EventLoopGroup to notify listeners for WebUserConnection. For
    // actual connections between Drillbits this EventLoopGroup is used to handle network related events. Though
    // there is no actual network connection associated with WebUserConnection but we need a CloseFuture in
    // WebSessionResources, so we are using EvenExecutor from network EventLoopGroup pool.
    final EventExecutor executor = workManager.getContext().getBitLoopGroup().next();

    register(new AbstractBinder() {
      @Override
      protected void configure() {
        DrillbitContext context = workManager.getContext();
        bind(drillbit).to(Drillbit.class);
        bind(workManager).to(WorkManager.class);
        bind(executor).to(EventExecutor.class);
        bind(context.getLpPersistence().getMapper()).to(ObjectMapper.class);
        bind(context.getStoreProvider()).to(PersistentStoreProvider.class);
        bind(context.getStorage()).to(StoragePluginRegistry.class);
        bind(new UserAuthEnabled(isAuthEnabled)).to(UserAuthEnabled.class);
        if (isAuthEnabled) {
          bindFactory(DrillUserPrincipalProvider.class).to(DrillUserPrincipal.class);
          bindFactory(AuthWebUserConnectionProvider.class).to(WebUserConnection.class);
        } else {
          bindFactory(AnonDrillUserPrincipalProvider.class).to(DrillUserPrincipal.class);
          bindFactory(AnonWebUserConnectionProvider.class).to(WebUserConnection.class);
        }
      }
    });
  }

  /**
   * Creates freemarker configuration settings,
   * default output format to trigger auto-escaping policy
   * and template loaders.
   *
   * @param servletContext servlet context
   * @return freemarker configuration settings
   */
  private Configuration getFreemarkerConfiguration(ServletContext servletContext) {
    Configuration configuration = new Configuration(Configuration.VERSION_2_3_26);
    configuration.setOutputFormat(HTMLOutputFormat.INSTANCE);

    List<TemplateLoader> loaders = new ArrayList<>();
    loaders.add(new WebappTemplateLoader(servletContext));
    loaders.add(new ClassTemplateLoader(DrillRestServer.class, "/"));
    try {
      loaders.add(new FileTemplateLoader(new File("/")));
    } catch (IOException e) {
      logger.error("Could not set up file template loader.", e);
    }
    configuration.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()])));
    return configuration;
  }

  public static class AuthWebUserConnectionProvider implements Factory<WebUserConnection> {

    @Inject
    HttpServletRequest request;

    @Inject
    WorkManager workManager;

    @Inject
    EventExecutor executor;

    @Override
    public WebUserConnection provide() {
      final HttpSession session = request.getSession();
      final Principal sessionUserPrincipal = request.getUserPrincipal();

      // If there is no valid principal this means user is not logged in yet.
      if (sessionUserPrincipal == null) {
        return null;
      }

      // User is logged in, get/set the WebSessionResources attribute
      WebSessionResources webSessionResources =
              (WebSessionResources) session.getAttribute(WebSessionResources.class.getSimpleName());

      if (webSessionResources == null) {
        // User is login in for the first time
        final DrillbitContext drillbitContext = workManager.getContext();
        final DrillConfig config = drillbitContext.getConfig();
        final UserSession drillUserSession = UserSession.Builder.newBuilder()
                .withCredentials(UserBitShared.UserCredentials.newBuilder()
                        .setUserName(sessionUserPrincipal.getName())
                        .build())
                .withOptionManager(drillbitContext.getOptionManager())
                .setSupportComplexTypes(config.getBoolean(ExecConstants.CLIENT_SUPPORT_COMPLEX_TYPES))
                .build();

        // Only try getting remote address in first login since it's a costly operation.
        SocketAddress remoteAddress = null;
        try {
          // This can be slow as the underlying library will try to resolve the address
          remoteAddress = new InetSocketAddress(InetAddress.getByName(request.getRemoteAddr()), request.getRemotePort());
          session.setAttribute(SocketAddress.class.getSimpleName(), remoteAddress);
        } catch (Exception ex) {
          //no-op
          logger.trace("Failed to get the remote address of the http session request", ex);
        }

        // Create per session BufferAllocator and set it in session
        final String sessionAllocatorName = String.format("WebServer:AuthUserSession:%s", session.getId());
        final BufferAllocator sessionAllocator = workManager.getContext().getAllocator().newChildAllocator(
                sessionAllocatorName,
                config.getLong(ExecConstants.HTTP_SESSION_MEMORY_RESERVATION),
                config.getLong(ExecConstants.HTTP_SESSION_MEMORY_MAXIMUM));

        // Create a future which is needed by Foreman only. Foreman uses this future to add a close
        // listener to known about channel close event from underlying layer. We use this future to notify Foreman
        // listeners when the Web session (not connection) between Web Client and WebServer is closed. This will help
        // Foreman to cancel all the running queries for this Web Client.
        final Promise<Void> closeFuture = new DefaultPromise<>(executor);

        // Create a WebSessionResource instance which owns the lifecycle of all the session resources.
        // Set this instance as an attribute of HttpSession, since it will be used until session is destroyed
        webSessionResources = new WebSessionResources(sessionAllocator, remoteAddress, drillUserSession, closeFuture);
        session.setAttribute(WebSessionResources.class.getSimpleName(), webSessionResources);
      }
      // Create a new WebUserConnection for the request
      return new WebUserConnection(webSessionResources);
    }

    @Override
    public void dispose(WebUserConnection instance) { }
  }

  public static class AnonWebUserConnectionProvider implements Factory<WebUserConnection> {

    @Inject
    HttpServletRequest request;

    @Inject
    WorkManager workManager;

    @Inject
    EventExecutor executor;

    @Override
    public WebUserConnection provide() {
      final DrillbitContext drillbitContext = workManager.getContext();
      final DrillConfig config = drillbitContext.getConfig();

      // Create an allocator here for each request
      final BufferAllocator sessionAllocator = drillbitContext.getAllocator()
              .newChildAllocator("WebServer:AnonUserSession",
                      config.getLong(ExecConstants.HTTP_SESSION_MEMORY_RESERVATION),
                      config.getLong(ExecConstants.HTTP_SESSION_MEMORY_MAXIMUM));

      final Principal sessionUserPrincipal = createSessionUserPrincipal(config, request);

      // Create new UserSession for each request from non-authenticated user
      final UserSession drillUserSession = UserSession.Builder.newBuilder()
              .withCredentials(UserBitShared.UserCredentials.newBuilder()
                      .setUserName(sessionUserPrincipal.getName())
                      .build())
              .withOptionManager(drillbitContext.getOptionManager())
              .setSupportComplexTypes(drillbitContext.getConfig().getBoolean(ExecConstants.CLIENT_SUPPORT_COMPLEX_TYPES))
              .build();

      // Try to get the remote Address but set it to null in case of failure.
      SocketAddress remoteAddress = null;
      try {
        // This can be slow as the underlying library will try to resolve the address
        remoteAddress = new InetSocketAddress(InetAddress.getByName(request.getRemoteAddr()), request.getRemotePort());
      } catch (Exception ex) {
        // no-op
        logger.trace("Failed to get the remote address of the http session request", ex);
      }

      // Create a close future which is needed by Foreman only. Foreman uses this future to add a close
      // listener to known about channel close event from underlying layer.
      //
      // The invocation of this close future is no-op as it will be triggered after query completion in unsecure case.
      // But we need this close future as it's expected by Foreman.
      final Promise<Void> closeFuture = new DefaultPromise(executor);

      final WebSessionResources webSessionResources = new WebSessionResources(sessionAllocator, remoteAddress,
          drillUserSession, closeFuture);

      // Create a AnonWenUserConnection for this request
      return new AnonWebUserConnection(webSessionResources);
    }

    @Override
    public void dispose(WebUserConnection instance) { }

    /**
     * Creates session user principal. If impersonation is enabled without
     * authentication and User-Name header is present and valid, will create
     * session user principal with provided user name, otherwise anonymous user
     * name will be used. In both cases session user principal will have admin
     * rights.
     *
     * @param config drill config
     * @param request client request
     * @return session user principal
     *
     * @deprecated a userName property has since been added to POST /query.json.
     * and the web UI now never sets a User-Name header. The restriction to
     * unauthenticated Drill is also not enough for general impersonation.
     * Choose one way to request impersonation over HTTP, this or the other.
     * @link{org.apache.drill.exec.server.rest.QueryResources#submitQuery}
     */
    @Deprecated
    private Principal createSessionUserPrincipal(DrillConfig config, HttpServletRequest request) {
      if (WebServer.isOnlyImpersonationEnabled(config)) {
        final String userName = request.getHeader("User-Name");
        if (!Strings.isNullOrEmpty(userName)) {
          return new DrillUserPrincipal(userName, true);
        }
      }
      return new AnonDrillUserPrincipal();
    }
  }

  /**
   * Provider which injects DrillUserPrincipal directly instead of getting it
   * from SecurityContext and typecasting
   */
  public static class DrillUserPrincipalProvider implements Factory<DrillUserPrincipal> {

    @Inject HttpServletRequest request;

    @Override
    public DrillUserPrincipal provide() {
      return (DrillUserPrincipal) request.getUserPrincipal();
    }

    @Override
    public void dispose(DrillUserPrincipal principal) { }
  }

  // Provider which creates and cleanups DrillUserPrincipal for anonymous (auth disabled) mode
  public static class AnonDrillUserPrincipalProvider implements Factory<DrillUserPrincipal> {

    @RequestScoped
    @Override
    public DrillUserPrincipal provide() {
      return new AnonDrillUserPrincipal();
    }

    @Override
    public void dispose(DrillUserPrincipal principal) {
      // If this worked it would have been clean to free the resources here, but there are various scenarios
      // where dispose never gets called due to bugs in jersey.
    }
  }

  // Returns whether auth is enabled or not in config
  public static class UserAuthEnabled {
    private final boolean value;

    public UserAuthEnabled(boolean value) {
      this.value = value;
    }

    public boolean get() {
      return value;
    }
  }
}
