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
package org.apache.drill.yarn.appMaster.http;

import static org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.ADMIN_ROLE;

import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.drill.yarn.appMaster.Dispatcher;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DrillOnYarnConfig;
import org.eclipse.jetty.security.authentication.FormAuthenticator;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerMvcFeature;

/**
 * The Drill AM web UI. The format is highly compact. We use javax.ws.rs to mark
 * up a Pojo with page path, permissions and HTTP methods. The ADMIN_ROLE is
 * reused from Drill's web UI.
 * <p>
 * In general, all pages require admin role, except for two: the login page and
 * the redirect page which the YARN web UI follows to start the AM UI.
 */

public class WebUiPageTree extends PageTree {

  /**
   * Main DoY page that displays cluster status, and the status of
   * the resource groups. Available only to the admin user when
   * DoY is secured.
   */

  @Path("/")
  @RolesAllowed(ADMIN_ROLE)
  public static class RootPage {
    @Inject
    SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      ControllerModel model = new ControllerModel();
      dispatcher.getController().visit(model);
      model.countStrayDrillbits(dispatcher.getController());
      return new Viewable("/drill-am/index.ftl", toModel(sc, model));
    }
  }

  /**
   * Pages, adapted from Drill, that display the login and logout pages.
   * Login uses the security mechanism, again borrowed from Drill, to
   * validate the user against either the simple user/password
   * configured in DoY, or the user who launched DoY using the
   * Drill security mechanism.
   */

  @Path("/")
  @PermitAll
  public static class LogInLogOutPages {
    @Inject
    SecurityContext sc;

    public static final String REDIRECT_QUERY_PARM = "redirect";
    public static final String LOGIN_RESOURCE = "login";

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getLoginPage(@Context HttpServletRequest request,
        @Context HttpServletResponse response, @Context SecurityContext sc,
        @Context UriInfo uriInfo,
        @QueryParam(REDIRECT_QUERY_PARM) String redirect) throws Exception {

      if (!StringUtils.isEmpty(redirect)) {
        // If the URL has redirect in it, set the redirect URI in session, so
        // that after the login is successful, request
        // is forwarded to the redirect page.
        final HttpSession session = request.getSession(true);
        final URI destURI = UriBuilder
            .fromUri(URLDecoder.decode(redirect, "UTF-8")).build();
        session.setAttribute(FormAuthenticator.__J_URI, destURI.getPath());
      }

      return new Viewable("/drill-am/login.ftl", toModel(sc, (Object) null));
    }

    // Request type is POST because POST request which contains the login
    // credentials are invalid and the request is
    // dispatched here directly.
    @POST
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public Viewable getLoginPageAfterValidationError() {
      return new Viewable("/drill-am/login.ftl",
          toModel(sc, "Invalid user name or password."));
    }

    @GET
    @Path("/logout")
    public Viewable logout(@Context HttpServletRequest req,
        @Context HttpServletResponse resp) throws Exception {
      final HttpSession session = req.getSession();
      if (session != null) {
        session.invalidate();
      }

      req.getRequestDispatcher("/login").forward(req, resp);
      return null;
    }
  }

  /**
   * DoY provides a link to YARN to display the AM UI. YARN wants to display the
   * linked page in a frame, which does not play well with the DoY UI. To avoid
   * this, we give YARN a link to this redirect page which does nothing other
   * than to redirect the browser to the (full) DoY main UI.
   */

  @Path("/redirect")
  @PermitAll
  public static class RedirectPage {
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      Map<String, String> map = new HashMap<>();
      String baseUrl = DoYUtil.unwrapAmUrl(dispatcher.getTrackingUrl());
      map.put("amLink", baseUrl);
      map.put("clusterName", config.getString(DrillOnYarnConfig.APP_NAME));
      return new Viewable("/drill-am/redirect.ftl", map);
    }
  }

  /**
   * Display the configuration page which displays the contents of
   * DoY and selected Drill config as name/value pairs. Visible only
   * to the admin when DoY is secure.
   */

  @Path("/config")
  @RolesAllowed(ADMIN_ROLE)
  public static class ConfigPage {
    @Inject
    private SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      return new Viewable("/drill-am/config.ftl",
          toModel(sc, DrillOnYarnConfig.instance().getPairs()));
    }
  }

  /**
   * Displays the list of Drillbits showing details for each Drillbit.
   * (DoY uses the generic term "task", but, at present, the only
   * task that DoY runs is a Drillbit.
   */

  @Path("/drillbits")
  @RolesAllowed(ADMIN_ROLE)
  public static class DrillbitsPage {
    @Inject
    private SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      AbstractTasksModel.TasksModel model = new AbstractTasksModel.TasksModel();
      dispatcher.getController().visitTasks(model);
      model.listAnomalies(dispatcher.getController());
      model.sortTasks();

      // Done this funky way because FreeMarker only understands lists if they
      // are members of a hash (grumble, grumble...)

      Map<String, Object> map = new HashMap<>();
      map.put("model", model);
      map.put("tasks", model.getTasks());
      if (model.hasUnmanagedDrillbits()) {
        map.put("strays", model.getUnnamaged());
      }
      if (model.hasBlacklist()) {
        map.put("blacklist", model.getBlacklist());
      }
      map.put("showDisks", dispatcher.getController().supportsDiskResource());
      map.put("refreshSecs", DrillOnYarnConfig.config()
          .getInt(DrillOnYarnConfig.HTTP_REFRESH_SECS));
      return new Viewable("/drill-am/tasks.ftl", toMapModel(sc, map));
    }
  }

  /**
   * Displays a warning page to ask the user if they want to cancel
   * a Drillbit. This is a bit old-school; we display this as a
   * separate page. A good future enhancement is to do this as
   * a pop-up in Javascript. The GET request display the confirmation
   * page, the PUT request confirms cancellation and does the deed.
   * The task to be cancelled appears as a query parameter:
   * <pre>.../cancel?id=&lt;task id></pre>
   */

  @Path("/cancel/")
  @RolesAllowed(ADMIN_ROLE)
  public static class CancelDrillbitPage {
    @Inject
    private SecurityContext sc;

    @Inject
    private HttpServletRequest request;

    @QueryParam("id")
    private int id;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getPage() {
      ConfirmShrink confirm;
      if (dispatcher.getController().isTaskLive(id)) {
        confirm = new ConfirmShrink(ConfirmShrink.Mode.KILL);
      } else {
        confirm = new ConfirmShrink(ConfirmShrink.Mode.CANCEL);
      }
      confirm.id = id;
      return new Viewable("/drill-am/shrink-warning.ftl", toModel(sc, confirm, request));
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    public Viewable postPage() {
      Acknowledge ack;
      if (dispatcher.getController().cancelTask(id)) {
        ack = new Acknowledge(Acknowledge.Mode.CANCELLED);
      } else {
        ack = new Acknowledge(Acknowledge.Mode.INVALID_TASK);
      }
      ack.value = id;
      return new Viewable("/drill-am/confirm.ftl", toModel(sc, ack));
    }
  }

  /**
   * Displays a history of completed tasks which indicates failed or cancelled
   * Drillbits. Helps the admin to understand what has been happening on the
   * cluster if Drillbits have died.
   */

  @Path("/history")
  @RolesAllowed(ADMIN_ROLE)
  public static class HistoryPage {
    @Inject
    SecurityContext sc;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      AbstractTasksModel.HistoryModel model = new AbstractTasksModel.HistoryModel();
      dispatcher.getController().visit(model);
      Map<String, Object> map = new HashMap<>();
      map.put("model", model.results);
      map.put("refreshSecs", DrillOnYarnConfig.config()
          .getInt(DrillOnYarnConfig.HTTP_REFRESH_SECS));
      return new Viewable("/drill-am/history.ftl", toMapModel(sc, map));
    }
  }

  /**
   * Page that lets the admin change the cluster size or shut down the cluster.
   */

  @Path("/manage")
  @RolesAllowed(ADMIN_ROLE)
  public static class ManagePage {
    @Inject
    SecurityContext sc;

    @Inject
    HttpServletRequest request;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable getRoot() {
      ControllerModel model = new ControllerModel();
      dispatcher.getController().visit(model);
      return new Viewable("/drill-am/manage.ftl", toModel(sc, model, request));
    }
  }

  /**
   * Passes information to the acknowledgement page.
   */

  public static class Acknowledge {
    public enum Mode {
      STOPPED, INVALID_RESIZE, INVALID_ACTION, NULL_RESIZE, RESIZED, CANCELLED, INVALID_TASK
    };

    Mode mode;
    Object value;

    public Acknowledge(Mode mode) {
      this.mode = mode;
    }

    public String getType() {
      return mode.toString();
    }

    public Object getValue() {
      return value;
    }
  }

  /**
   * Passes information to the confirmation page.
   */

  public static class ConfirmShrink {
    public enum Mode {
      SHRINK, STOP, CANCEL, KILL
    };

    Mode mode;
    int value;
    int id;

    public ConfirmShrink(Mode mode) {
      this.mode = mode;
    }

    public boolean isStop() {
      return mode == Mode.STOP;
    }

    public boolean isCancel() {
      return mode == Mode.CANCEL;
    }

    public boolean isKill() {
      return mode == Mode.KILL;
    }

    public boolean isShrink() {
      return mode == Mode.SHRINK;
    }

    public int getCount() {
      return value;
    }

    public int getId() {
      return id;
    }
  }

  /**
   * Confirm that the user wants to resize the cluster. Displays a warning if
   * the user wants to shrink the cluster, since, at present, doing so will
   * kill any in-flight queries. The GET request display the warning,
   * the POST request confirms the action. The action itself is provided
   * as query parameters:
   * <pre>.../resize?type=&lt;type>&n=&lt;quantity></pre>
   * Where the type is one of "resize", "grow", "shrink" or
   * "force-shrink" and n is the associated quantity.
   * <p>
   * Note that the manage page only provides the "resize" option; the
   * grow and shrink options were removed from the Web UI and are only
   * visible through the REST API.
   */

  @Path("/resize")
  @RolesAllowed(ADMIN_ROLE)
  public static class ResizePage {
    @Inject
    SecurityContext sc;

    @Inject
    HttpServletRequest request;

    @FormParam("n")
    int n;
    @FormParam("type")
    String type;

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Viewable resize() {
      int curSize = dispatcher.getController().getTargetCount();
      if (n <= 0) {
        Acknowledge confirm = new Acknowledge(Acknowledge.Mode.INVALID_RESIZE);
        confirm.value = n;
        return new Viewable("/drill-am/confirm.ftl", toModel(sc, confirm));
      }
      if (type == null) {
        type = "null";
      }
      int newSize;
      boolean confirmed = false;
      if (type.equalsIgnoreCase("resize")) {
        newSize = n;
      } else if (type.equalsIgnoreCase("grow")) {
        newSize = curSize + n;
      } else if (type.equalsIgnoreCase("shrink")) {
        newSize = curSize - n;
      } else if (type.equalsIgnoreCase("force-shrink")) {
        newSize = curSize - n;
        confirmed = true;
      } else {
        Acknowledge confirm = new Acknowledge(Acknowledge.Mode.INVALID_ACTION);
        confirm.value = type;
        return new Viewable("/drill-am/confirm.ftl", toModel(sc, confirm));
      }

      if (curSize == newSize) {
        Acknowledge confirm = new Acknowledge(Acknowledge.Mode.NULL_RESIZE);
        confirm.value = newSize;
        return new Viewable("/drill-am/confirm.ftl", toModel(sc, confirm));
      } else if (confirmed || curSize < newSize) {
        Acknowledge confirm = new Acknowledge(Acknowledge.Mode.RESIZED);
        confirm.value = dispatcher.getController().resizeTo(newSize);
        return new Viewable("/drill-am/confirm.ftl", toModel(sc, confirm));
      } else {
        ConfirmShrink confirm = new ConfirmShrink(ConfirmShrink.Mode.SHRINK);
        confirm.value = curSize - newSize;
        return new Viewable("/drill-am/shrink-warning.ftl",
            toModel(sc, confirm, request));
      }
    }
  }

  /**
   * Confirmation page when the admin asks to stop the cluster.
   * The GET request displays the confirmation, the POST does
   * the deed. As for other confirmation pages, this is an old-style,
   * quick & dirty solution. A more modern solution would be to use JavaScript
   * to pop up a confirmation dialog.
   */

  @Path("/stop/")
  @RolesAllowed(ADMIN_ROLE)
  public static class StopPage {
    @Inject
    SecurityContext sc;

    @Inject
    HttpServletRequest request;

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Viewable requestStop() {
      ConfirmShrink confirm = new ConfirmShrink(ConfirmShrink.Mode.STOP);
      return new Viewable("/drill-am/shrink-warning.ftl", toModel(sc, confirm, request));
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    public Viewable doStop() {
      dispatcher.getController().shutDown();
      Acknowledge confirm = new Acknowledge(Acknowledge.Mode.STOPPED);
      return new Viewable("/drill-am/confirm.ftl", toModel(sc, confirm));
    }
  }

  /**
   * Build the pages for the Web UI using Freemarker to implement the
   * MVC mechanism. This class builds on a rather complex mechanism; understand
   * that to understand what the lines of code below are doing.
   *
   * @param dispatcher the DoY AM dispatcher that receives requests for
   * information about, or requests to change the state of, the Drill clutser
   */

  public WebUiPageTree(Dispatcher dispatcher) {
    super(dispatcher);

    // Markup engine
    register(FreemarkerMvcFeature.class);

    // Web UI Pages
    register(RootPage.class);
    register(RedirectPage.class);
    register(ConfigPage.class);
    register(DrillbitsPage.class);
    register(CancelDrillbitPage.class);
    register(HistoryPage.class);
    register(ManagePage.class);
    register(ResizePage.class);
    register(StopPage.class);

    // Authorization
    // See: https://jersey.java.net/documentation/latest/security.html

    if (AMSecurityManagerImpl.isEnabled()) {
      register(LogInLogOutPages.class);
      register(AuthDynamicFeature.class);
      register(RolesAllowedDynamicFeature.class);
    }
  }

}
