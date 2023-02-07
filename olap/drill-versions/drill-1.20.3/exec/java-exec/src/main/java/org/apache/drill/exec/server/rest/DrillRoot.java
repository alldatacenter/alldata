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

import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.rest.DrillRestServer.UserAuthEnabled;
import org.apache.drill.exec.server.rest.auth.AuthDynamicFeature;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.work.WorkManager;
import org.apache.drill.exec.work.foreman.rm.DistributedQueryQueue;
import org.apache.drill.exec.work.foreman.rm.DistributedQueryQueue.ZKQueueInfo;
import org.apache.drill.exec.work.foreman.rm.DynamicResourceManager;
import org.apache.drill.exec.work.foreman.rm.QueryQueue;
import org.apache.drill.exec.work.foreman.rm.ResourceManager;
import org.apache.drill.exec.work.foreman.rm.ThrottledResourceManager;
import org.apache.http.client.methods.HttpPost;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;

import static org.apache.drill.exec.server.rest.auth.DrillUserPrincipal.ADMIN_ROLE;

@Path("/")
@PermitAll
public class DrillRoot {
  private static final Logger logger = LoggerFactory.getLogger(DrillRoot.class);

  @Inject
  UserAuthEnabled authEnabled;
  @Inject
  WorkManager work;
  @Inject
  SecurityContext sc;
  @Inject
  Drillbit drillbit;
  @Inject
  HttpServletRequest request;

  @GET
  @Produces(MediaType.TEXT_HTML)
  public Viewable getClusterInfo() {
    return ViewableWithPermissions.create(authEnabled.get(), "/rest/index.ftl", sc, getClusterInfoJSON());
  }


  @GET
  @Path("/state")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getDrillbitStatus() {
    Collection<DrillbitInfo> drillbits = getClusterInfoJSON().getDrillbits();
    Map<String, String> drillStatusMap = new HashMap<String, String>();
    for (DrillbitInfo drillbit : drillbits) {
      drillStatusMap.put(drillbit.getAddress() + "-" + drillbit.getHttpPort(), drillbit.getState());
    }
    return setResponse(drillStatusMap);
  }

  @GET
  @Path("/gracePeriod")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Integer> getGracePeriod() {
    final DrillConfig config = work.getContext().getConfig();
    final int gracePeriod = config.getInt(ExecConstants.GRACE_PERIOD);
    Map<String, Integer> gracePeriodMap = new HashMap<String, Integer>();
    gracePeriodMap.put("gracePeriod", gracePeriod);
    return gracePeriodMap;
  }

  @GET
  @Path("/portNum")
  @Produces(MediaType.APPLICATION_JSON)
  public Map<String, Integer> getPortNum() {

    final DrillConfig config = work.getContext().getConfig();
    final int port = config.getInt(ExecConstants.HTTP_PORT);
    Map<String, Integer> portMap = new HashMap<String, Integer>();
    portMap.put("port", port);
    return portMap;
  }

  @GET
  @Path("/queriesCount")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRemainingQueries() {
    Map<String, Integer> queriesInfo = work.getRemainingQueries();
    return setResponse(queriesInfo);
  }

  @POST
  @Path("/gracefulShutdown")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN_ROLE)
  public Response shutdownDrillbit() throws Exception {
    String resp = "Graceful Shutdown request is triggered";
    return shutdown(resp);
  }

  @POST
  @Path("/gracefulShutdown/{hostname}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN_ROLE)
  public String shutdownDrillbitByName(@PathParam("hostname") String hostname) throws Exception {
    URL shutdownURL = WebUtils.getDrillbitURL(work, request, hostname, "/gracefulShutdown");
    return WebUtils.doHTTPRequest(new HttpPost(shutdownURL.toURI()), work.getContext().getConfig());
  }

  @POST
  @Path("/shutdown")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN_ROLE)
  public Response shutdownForcefully() throws Exception {
    drillbit.setForcefulShutdown(true);
    String resp = "Forceful shutdown request is triggered";
    return shutdown(resp);
  }

  @POST
  @Path("/quiescent")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed(ADMIN_ROLE)
  public Response drillbitToQuiescentMode() throws Exception {
    drillbit.setQuiescentMode(true);
    String resp = "Request to put drillbit in Quiescent mode is triggered";
    return shutdown(resp);
  }

  @GET
  @Path("/cluster.json")
  @Produces(MediaType.APPLICATION_JSON)
  public ClusterInfo getClusterInfoJSON() {
    final Collection<DrillbitInfo> drillbits = Sets.newTreeSet();
    final Collection<String> mismatchedVersions = Sets.newTreeSet();

    final DrillbitContext dbContext = work.getContext();
    final DrillbitEndpoint currentDrillbit = dbContext.getEndpoint();
    final String currentVersion = currentDrillbit.getVersion();

    final DrillConfig config = dbContext.getConfig();
    final boolean userEncryptionEnabled =
            config.getBoolean(ExecConstants.USER_ENCRYPTION_SASL_ENABLED) ||
                    config .getBoolean(ExecConstants.USER_SSL_ENABLED);
    final boolean bitEncryptionEnabled = config.getBoolean(ExecConstants.BIT_ENCRYPTION_SASL_ENABLED);

    OptionManager optionManager = work.getContext().getOptionManager();
    final boolean isUserLoggedIn = AuthDynamicFeature.isUserLoggedIn(sc);
    final boolean shouldShowAdminInfo = isUserLoggedIn && ((DrillUserPrincipal)sc.getUserPrincipal()).isAdminUser();

    for (DrillbitEndpoint endpoint : work.getContext().getAvailableBits()) {
      final DrillbitInfo drillbit = new DrillbitInfo(endpoint,
              isDrillbitsTheSame(currentDrillbit, endpoint),
              currentVersion.equals(endpoint.getVersion()));
      if (!drillbit.isVersionMatch()) {
        mismatchedVersions.add(drillbit.getVersion());
      }
      drillbits.add(drillbit);
    }

    // If the user is logged in and is admin user then show the admin user info
    // For all other cases the user info need-not or should-not be displayed
    if (shouldShowAdminInfo) {
      final String processUser = ImpersonationUtil.getProcessUserName();
      final String processUserGroups = Joiner.on(", ").join(ImpersonationUtil.getProcessUserGroupNames());
      String adminUsers = ExecConstants.ADMIN_USERS_VALIDATOR.getAdminUsers(optionManager);
      String adminUserGroups = ExecConstants.ADMIN_USER_GROUPS_VALIDATOR.getAdminUserGroups(optionManager);

      logger.debug("Admin info: user: "  + adminUsers +  " user group: " + adminUserGroups +
          " userLoggedIn "  + isUserLoggedIn + " shouldShowAdminInfo: " + shouldShowAdminInfo);

      return new ClusterInfo(drillbits, currentVersion, mismatchedVersions,
          userEncryptionEnabled, bitEncryptionEnabled, shouldShowAdminInfo,
          QueueInfo.build(dbContext.getResourceManager()),
          processUser, processUserGroups, adminUsers, adminUserGroups, authEnabled.get());
    }

    return new ClusterInfo(drillbits, currentVersion, mismatchedVersions,
        userEncryptionEnabled, bitEncryptionEnabled, shouldShowAdminInfo,
        QueueInfo.build(dbContext.getResourceManager()), authEnabled.get());
  }

  /**
   * Compares two drillbits based on their address and ports (control, data, user).
   *
   * @param endpoint1 first drillbit to compare
   * @param endpoint2 second drillbit to compare
   * @return true if drillbit are the same
   */
  private boolean isDrillbitsTheSame(DrillbitEndpoint endpoint1, DrillbitEndpoint endpoint2) {
    return endpoint1.getAddress().equals(endpoint2.getAddress()) &&
        endpoint1.getControlPort() == endpoint2.getControlPort() &&
        endpoint1.getDataPort() == endpoint2.getDataPort() &&
        endpoint1.getUserPort() == endpoint2.getUserPort();
  }

  private Response setResponse(Map<String, ?> entity) {
    return Response.ok()
            .entity(entity)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
            .header("Access-Control-Allow-Credentials","true")
            .allow("OPTIONS").build();
  }

  private Response shutdown(String resp) throws Exception {
    Map<String, String> shutdownInfo = new HashMap<String, String>();
    new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            drillbit.close();
          } catch (Exception e) {
            logger.error("Request to shutdown drillbit failed", e);
          }
        }
      }).start();
    shutdownInfo.put("response",resp);
    return setResponse(shutdownInfo);
  }

/**
 * Pretty-printing wrapper class around the ZK-based queue summary.
 */

@XmlRootElement
public static class QueueInfo {
  private final ZKQueueInfo zkQueueInfo;

  public static QueueInfo build(ResourceManager rm) {

    // Consider queues enabled only if the ZK-based queues are in use.

    ThrottledResourceManager throttledRM = null;
    if (rm != null && rm instanceof DynamicResourceManager) {
      DynamicResourceManager dynamicRM = (DynamicResourceManager) rm;
      rm = dynamicRM.activeRM();
    }
    if (rm != null && rm instanceof ThrottledResourceManager) {
      throttledRM = (ThrottledResourceManager) rm;
    }
    if (throttledRM == null) {
      return new QueueInfo(null);
    }
    QueryQueue queue = throttledRM.queue();
    if (queue == null || !(queue instanceof DistributedQueryQueue)) {
      return new QueueInfo(null);
    }

    return new QueueInfo(((DistributedQueryQueue) queue).getInfo());
  }

  @JsonCreator
  public QueueInfo(ZKQueueInfo queueInfo) {
    zkQueueInfo = queueInfo;
  }

  public boolean isEnabled() { return zkQueueInfo != null; }

  public int smallQueueSize() {
    return isEnabled() ? zkQueueInfo.smallQueueSize : 0;
  }

  public int largeQueueSize() {
    return isEnabled() ? zkQueueInfo.largeQueueSize : 0;
  }

  public String threshold() {
    return isEnabled()
            ? Double.toString(zkQueueInfo.queueThreshold)
            : "N/A";
  }

  public String smallQueueMemory() {
    return isEnabled()
            ? toBytes(zkQueueInfo.memoryPerSmallQuery)
            : "N/A";
  }

  public String largeQueueMemory() {
    return isEnabled()
            ? toBytes(zkQueueInfo.memoryPerLargeQuery)
            : "N/A";
  }

  public String totalMemory() {
    return isEnabled()
            ? toBytes(zkQueueInfo.memoryPerNode)
            : "N/A";
  }

  private final long ONE_MB = 1024 * 1024;

  private String toBytes(long memory) {
    if (memory < 10 * ONE_MB) {
      return String.format("%,d bytes", memory);
    } else {
      return String.format("%,.0f MB", memory * 1.0D / ONE_MB);
    }
  }
}

@XmlRootElement
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public static class ClusterInfo {
  private final Collection<DrillbitInfo> drillbits;
  private final String currentVersion;
  private final Collection<String> mismatchedVersions;
  private final boolean userEncryptionEnabled;
  private final boolean bitEncryptionEnabled;
  private final boolean shouldShowAdminInfo;
  private final boolean authEnabled;
  private final QueueInfo queueInfo;

  private String adminUsers;
  private String adminUserGroups;
  private String processUser;
  private String processUserGroups;

  @JsonCreator
  public ClusterInfo(Collection<DrillbitInfo> drillbits,
                     String currentVersion,
                     Collection<String> mismatchedVersions,
                     boolean userEncryption,
                     boolean bitEncryption,
                     boolean shouldShowAdminInfo,
                     QueueInfo queueInfo,
                     boolean authEnabled) {
    this.drillbits = Sets.newTreeSet(drillbits);
    this.currentVersion = currentVersion;
    this.mismatchedVersions = Sets.newTreeSet(mismatchedVersions);
    this.userEncryptionEnabled = userEncryption;
    this.bitEncryptionEnabled = bitEncryption;
    this.shouldShowAdminInfo = shouldShowAdminInfo;
    this.queueInfo = queueInfo;
    this.authEnabled = authEnabled;
  }

  @JsonCreator
  public ClusterInfo(Collection<DrillbitInfo> drillbits,
                     String currentVersion,
                     Collection<String> mismatchedVersions,
                     boolean userEncryption,
                     boolean bitEncryption,
                     boolean shouldShowAdminInfo,
                     QueueInfo queueInfo,
                     String processUser,
                     String processUserGroups,
                     String adminUsers,
                     String adminUserGroups,
                     boolean authEnabled) {
    this(drillbits, currentVersion, mismatchedVersions, userEncryption, bitEncryption, shouldShowAdminInfo, queueInfo, authEnabled);
    this.processUser = processUser;
    this.processUserGroups = processUserGroups;
    this.adminUsers = adminUsers;
    this.adminUserGroups = adminUserGroups;
  }

  public Collection<DrillbitInfo> getDrillbits() {
    return Sets.newTreeSet(drillbits);
  }

  public String getCurrentVersion() {
    return currentVersion;
  }

  public Collection<String> getMismatchedVersions() {
    return Sets.newTreeSet(mismatchedVersions);
  }

  public boolean isUserEncryptionEnabled() { return userEncryptionEnabled; }

  public boolean isBitEncryptionEnabled() { return bitEncryptionEnabled; }

  public String getProcessUser() { return processUser; }

  public String getProcessUserGroups() { return processUserGroups; }

  public String getAdminUsers() { return adminUsers; }

  public String getAdminUserGroups() { return adminUserGroups; }

  public boolean shouldShowAdminInfo() { return shouldShowAdminInfo; }

  public QueueInfo queueInfo() { return queueInfo; }

  public boolean isAuthEnabled() { return  authEnabled; }
}

public static class DrillbitInfo implements Comparable<DrillbitInfo> {
  private final String address;
  private final String httpPort;
  private final String userPort;
  private final String controlPort;
  private final String dataPort;
  private final String version;
  private final boolean current;
  private final boolean versionMatch;
  private final String state;

  @JsonCreator
  public DrillbitInfo(DrillbitEndpoint drillbit, boolean current, boolean versionMatch) {
    this.address = drillbit.getAddress();
    this.httpPort = String.valueOf(drillbit.getHttpPort());
    this.userPort = String.valueOf(drillbit.getUserPort());
    this.controlPort = String.valueOf(drillbit.getControlPort());
    this.dataPort = String.valueOf(drillbit.getDataPort());
    this.version = Strings.isNullOrEmpty(drillbit.getVersion()) ? "Undefined" : drillbit.getVersion();
    this.current = current;
    this.versionMatch = versionMatch;
    this.state = String.valueOf(drillbit.getState());
  }

  public String getAddress() { return address; }

  public String getHttpPort() { return httpPort; }

  public String getUserPort() { return userPort; }

  public String getControlPort() { return controlPort; }

  public String getDataPort() { return dataPort; }

  public String getVersion() { return version; }

  public boolean isCurrent() { return current; }

  public boolean isVersionMatch() { return versionMatch; }

  public String getState() { return state; }

  /**
   * Method used to sort Drillbits. Current Drillbit goes first.
   * Then Drillbits with matching versions, after them Drillbits with mismatching versions.
   * Matching Drillbits are sorted according address natural order,
   * mismatching Drillbits are sorted according version, address natural order.
   *
   * @param drillbitToCompare Drillbit to compare against
   * @return -1 if Drillbit should be before, 1 if after in list
   */
  @Override
  public int compareTo(DrillbitInfo drillbitToCompare) {
    if (this.isCurrent()) {
      return -1;
    }

    if (drillbitToCompare.isCurrent()) {
      return 1;
    }

    if (this.isVersionMatch() == drillbitToCompare.isVersionMatch()) {
      if (this.version.equals(drillbitToCompare.getVersion())) {
        {
          if (this.address.equals(drillbitToCompare.getAddress())) {
            return (this.controlPort.compareTo(drillbitToCompare.getControlPort()));
          }
          return (this.address.compareTo(drillbitToCompare.getAddress()));
        }
      }
      return this.version.compareTo(drillbitToCompare.getVersion());
    }
    return this.versionMatch ? -1 : 1;
  }
}
}
