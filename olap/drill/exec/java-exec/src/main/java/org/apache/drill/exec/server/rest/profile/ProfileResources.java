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
package org.apache.drill.exec.server.rest.profile;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.store.TransientStore;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryInfo;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.rest.DrillRestServer.UserAuthEnabled;
import org.apache.drill.exec.server.QueryProfileStoreContext;
import org.apache.drill.exec.server.rest.ViewableWithPermissions;
import org.apache.drill.exec.server.rest.auth.DrillUserPrincipal;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.work.WorkManager;
import org.apache.drill.exec.work.foreman.Foreman;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.glassfish.jersey.server.mvc.Viewable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.cache.Cache;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;

import static org.owasp.encoder.Encode.forHtml;

@Path("/")
@RolesAllowed(DrillUserPrincipal.AUTHENTICATED_ROLE)
public class ProfileResources {
  private static final Logger logger = LoggerFactory.getLogger(ProfileResources.class);

  @Inject
  UserAuthEnabled authEnabled;

  @Inject
  WorkManager work;

  @Inject
  DrillUserPrincipal principal;

  @Inject
  SecurityContext sc;

  @Inject
  HttpServletRequest request;

  public static class ProfileInfo implements Comparable<ProfileInfo> {
    private static final int QUERY_SNIPPET_MAX_CHAR = 150;
    private static final int QUERY_SNIPPET_MAX_LINES = 8;

    public static final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final String queryId;
    private final long startTime;
    private final long endTime;
    private final Date time;
    private final String link;
    private final String foreman;
    private final String query;
    private final String state;
    private final String user;
    private final double totalCost;
    private final String queueName;

    public ProfileInfo(DrillConfig drillConfig, String queryId, long startTime, long endTime, String foreman, String query,
                       String state, String user, double totalCost, String queueName) {
      this.queryId = queryId;
      this.startTime = startTime;
      this.endTime = endTime;
      this.time = new Date(startTime);
      this.foreman = foreman;
      this.link = generateLink(drillConfig, foreman, queryId);
      this.query = extractQuerySnippet(query);
      this.state = state;
      this.user = user;
      this.totalCost = totalCost;
      this.queueName = queueName;
    }

    public String getUser() { return user; }

    public String getQuery() { return query; }

    public String getQueryId() { return queryId; }

    public String getTime() { return format.format(time); }

    public long getStartTime() { return startTime; }

    public long getEndTime() { return endTime; }

    public String getDuration() {
      return (new SimpleDurationFormat(startTime, endTime)).verbose();
    }

    public String getState() { return state; }

    public String getLink() { return link; }

    public String getForeman() { return foreman; }

    public double getTotalCost() { return totalCost; }

    public String getQueueName() { return queueName; }

    @Override
    public int compareTo(ProfileInfo other) {
      return time.compareTo(other.time);
    }

    /**
     * Generates link which will return query profile in json representation.
     *
     * @param drillConfig drill configuration
     * @param foreman foreman hostname
     * @param queryId query id
     * @return link
     */
    private String generateLink(DrillConfig drillConfig, String foreman, String queryId) {
      StringBuilder sb = new StringBuilder();
      if (drillConfig.getBoolean(ExecConstants.HTTP_ENABLE_SSL)) {
        sb.append("https://");
      } else {
        sb.append("http://");
      }
      sb.append(foreman);
      sb.append(":");
      sb.append(drillConfig.getInt(ExecConstants.HTTP_PORT));
      sb.append("/profiles/");
      sb.append(queryId);
      sb.append(".json");
      return sb.toString();
    }

    /**
     * Extract only the first 150 characters of the query.
     * If this spans more than 8 lines, we truncate excess lines for sake of readability
     * @param queryText
     * @return truncated text
     */
    private String extractQuerySnippet(String queryText) {
      //Extract upto max char limit as snippet
      String sizeCappedQuerySnippet = queryText.substring(0,  Math.min(queryText.length(), QUERY_SNIPPET_MAX_CHAR));
      String[] queryParts = sizeCappedQuerySnippet.split(System.lineSeparator());
      //Trimming down based on line-count
      if (QUERY_SNIPPET_MAX_LINES < queryParts.length) {
        int linesConstructed = 0;
        StringBuilder lineCappedQuerySnippet = new StringBuilder();
        for (String qPart : queryParts) {
          lineCappedQuerySnippet.append(qPart);
          if (++linesConstructed < QUERY_SNIPPET_MAX_LINES) {
            lineCappedQuerySnippet.append(System.lineSeparator());
          } else {
            lineCappedQuerySnippet.append(" ... ");
            break;
          }
        }
        return lineCappedQuerySnippet.toString();
      }
      return sizeCappedQuerySnippet;
    }
  }

  protected PersistentStoreProvider getProvider() {
    return work.getContext().getStoreProvider();
  }

  protected ClusterCoordinator getCoordinator() {
    return work.getContext().getClusterCoordinator();
  }

  @XmlRootElement
  public class QProfilesBase {
    private final List<String> errors;

    public QProfilesBase(List<String> errors) {
      this.errors = errors;
    }

    public List<String> getErrors() {
      return errors;
    }

    public int getMaxFetchedQueries() {
      return work.getContext().getConfig().getInt(ExecConstants.HTTP_MAX_PROFILES);
    }

    public String getQueriesPerPage() {
      List<Integer> queriesPerPageOptions = work.getContext().getConfig().getIntList(ExecConstants.HTTP_PROFILES_PER_PAGE);
      Collections.sort(queriesPerPageOptions);
      return Joiner.on(",").join(queriesPerPageOptions);
    }
  }

  @XmlRootElement
  public class QProfiles extends QProfilesBase {
    private  final List<ProfileInfo> runningQueries;
    private  final List<ProfileInfo> finishedQueries;

    public QProfiles(List<ProfileInfo> runningQueries, List<ProfileInfo> finishedQueries, List<String> errors) {
      super(errors);
      this.runningQueries = runningQueries;
      this.finishedQueries = finishedQueries;
    }

    public List<ProfileInfo> getRunningQueries() {
      return runningQueries;
    }

    public List<ProfileInfo> getFinishedQueries() {
      return finishedQueries;
    }
  }

  @XmlRootElement
  public class QProfilesRunning extends QProfilesBase {
    private final List<ProfileInfo> runningQueries;

    public QProfilesRunning(List<ProfileInfo> runningQueries,List<String> errors) {
      super(errors);
      this.runningQueries = runningQueries;
    }

    public List<ProfileInfo> getRunningQueries() {
      return runningQueries;
    }
  }

  @XmlRootElement
  public class QProfilesCompleted extends QProfilesBase {
    private final List<ProfileInfo> finishedQueries;

    public QProfilesCompleted(List<ProfileInfo> finishedQueries, List<String> errors) {
      super(errors);
      this.finishedQueries = finishedQueries;
    }

    public List<ProfileInfo> getFinishedQueries() {
      return finishedQueries;
    }
  }

  //max Param to cap listing of profiles
  private static final String MAX_QPROFILES_PARAM = "max";

  private static final Cache<String, String> PROFILE_CACHE = CacheBuilder
    .newBuilder().expireAfterAccess(1, TimeUnit.MINUTES).build();

  @GET
  @Path("/profiles.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfilesJSON(@Context UriInfo uriInfo) {
    QProfilesRunning running_results = (QProfilesRunning)getRunningProfilesJSON(uriInfo).getEntity();
    QProfilesCompleted completed_results = (QProfilesCompleted)getCompletedProfilesJSON(uriInfo).getEntity();
    final List<String> total_errors = Lists.newArrayList();
    total_errors.addAll(running_results.getErrors());
    total_errors.addAll(completed_results.getErrors());

    QProfiles final_results = new QProfiles(running_results.runningQueries, completed_results.finishedQueries, total_errors);
    return total_errors.size() == 0
        ? Response.ok().entity(final_results).build()
        : Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(final_results)
          .build();
  }

  @GET
  @Path("/profiles/json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSpecificJSON(@Context UriInfo uriInfo, @QueryParam("status") String status) {
    switch (status) {
    case "running":
      return getRunningProfilesJSON(uriInfo);
    case "completed":
      return getCompletedProfilesJSON(uriInfo);
    case "all":
    default:
      return getProfilesJSON(uriInfo);
    }
  }

  @GET
  @Path("/profiles/running.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getRunningProfilesJSON(@Context UriInfo uriInfo) {
    try {
      final QueryProfileStoreContext profileStoreContext = work.getContext().getProfileStoreContext();
      final TransientStore<QueryInfo> running = profileStoreContext.getRunningProfileStore();
      final List<String> errors = Lists.newArrayList();
      final List<ProfileInfo> runningQueries = Lists.newArrayList();

      final Iterator<Map.Entry<String, QueryInfo>> runningEntries = running.entries();
      while (runningEntries.hasNext()) {
        try {
          final Map.Entry<String, QueryInfo> runningEntry = runningEntries.next();
          final QueryInfo profile = runningEntry.getValue();
          if (principal.canManageProfileOf(profile.getUser())) {
            runningQueries.add(
                new ProfileInfo(work.getContext().getConfig(),
                    runningEntry.getKey(), profile.getStart(),
                    System.currentTimeMillis(), profile.getForeman().getAddress(),
                    profile.getQuery(),
                    ProfileUtil.getQueryStateDisplayName(profile.getState()),
                    profile.getUser(), profile.getTotalCost(),
                    profile.getQueueName()));
          }
        } catch (Exception e) {
          errors.add(e.getMessage());
          logger.error("Error getting running query info.", e);
        }
      }
      Collections.sort(runningQueries, Collections.reverseOrder());
      QProfilesRunning rProf = new QProfilesRunning(runningQueries, errors);
      return errors.size() == 0
          ? Response.ok().entity(rProf).build()
          : Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(rProf)
            .build();
    } catch (Exception e) {
      throw UserException.resourceError(e).message("Failed to get running profiles from ephemeral store.").build(logger);
    }
  }

  @GET
  @Path("/profiles/completed.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getCompletedProfilesJSON(@Context UriInfo uriInfo) {
    try {
      final QueryProfileStoreContext profileStoreContext = work.getContext().getProfileStoreContext();
      final PersistentStore<QueryProfile> completed = profileStoreContext.getCompletedProfileStore();
      final List<String> errors = Lists.newArrayList();
      final List<ProfileInfo> finishedQueries = Lists.newArrayList();

     // Defining #Profiles to load
      int maxProfilesToLoad = work.getContext().getConfig().getInt(ExecConstants.HTTP_MAX_PROFILES);
      String maxProfilesParams = uriInfo.getQueryParameters().getFirst(MAX_QPROFILES_PARAM);
      if (maxProfilesParams != null && !maxProfilesParams.isEmpty()) {
        maxProfilesToLoad = Integer.valueOf(maxProfilesParams);
      }

      final Iterator<Map.Entry<String, QueryProfile>> range = completed.getRange(0, maxProfilesToLoad);
      while (range.hasNext()) {
        try {
          final Map.Entry<String, QueryProfile> profileEntry = range.next();
          final QueryProfile profile = profileEntry.getValue();
          if (principal.canManageProfileOf(profile.getUser())) {
            finishedQueries.add(
                new ProfileInfo(work.getContext().getConfig(),
                    profileEntry.getKey(), profile.getStart(), profile.getEnd(),
                    profile.getForeman().getAddress(), profile.getQuery(),
                    ProfileUtil.getQueryStateDisplayName(profile.getState()),
                    profile.getUser(), profile.getTotalCost(), profile.getQueueName()));
          }
        } catch (Exception e) {
          errors.add(e.getMessage());
          logger.error("Error getting finished query profile.", e);
        }
      }
      Collections.sort(finishedQueries, Collections.reverseOrder());
      QProfilesCompleted cProf = new QProfilesCompleted(finishedQueries, errors);
      return errors.size() == 0
          ? Response.ok().entity(cProf).build()
          : Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(cProf)
            .build();
    } catch (Exception e) {
      throw UserException.resourceError(e).message("Failed to get completed profiles from persistent store.").build(logger);
    }
  }

  @GET
  @Path("/profiles")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getProfiles(@Context UriInfo uriInfo) {
    QProfiles profiles = (QProfiles) getProfilesJSON(uriInfo).getEntity();
    return ViewableWithPermissions.create(authEnabled.get(), "/rest/profile/list.ftl", sc, profiles);
  }

  private QueryProfile getQueryProfile(String queryId) {
    QueryId id = QueryIdHelper.getQueryIdFromString(queryId);

    // first check local running
    Foreman f = work.getBee().getForemanForQueryId(id);
    if(f != null){
      QueryProfile queryProfile = f.getQueryManager().getQueryProfile();
      checkOrThrowProfileViewAuthorization(queryProfile);
      return queryProfile;
    }

    // then check remote running
    try {
      final TransientStore<QueryInfo> running = work.getContext().getProfileStoreContext().getRunningProfileStore();
      final QueryInfo info = running.get(queryId);
      if (info != null) {
        QueryProfile queryProfile = work.getContext()
            .getController()
            .getTunnel(info.getForeman())
            .requestQueryProfile(id)
            .checkedGet(2, TimeUnit.SECONDS);
        checkOrThrowProfileViewAuthorization(queryProfile);
        return queryProfile;
      }
    }catch(Exception e){
      logger.trace("Failed to find query as running profile.", e);
    }

    // then check blob store
    try {
      final PersistentStore<QueryProfile> profiles = work.getContext().getProfileStoreContext().getCompletedProfileStore();
      final QueryProfile queryProfile = profiles.get(queryId);
      if (queryProfile != null) {
        checkOrThrowProfileViewAuthorization(queryProfile);
        return queryProfile;
      }
    } catch (final Exception e) {
      throw new DrillRuntimeException("error while retrieving profile", e);
    }

    throw UserException.validationError()
    .message("No profile with given query id '%s' exists. Please verify the query id.", queryId)
    .build(logger);
  }

  @GET
  @Path("/profiles/{queryid}.json")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getProfileJSON(@PathParam("queryid") String queryId) {
    try {
      String profileData = PROFILE_CACHE.getIfPresent(queryId);
      if (profileData == null) {
        profileData = new String(work.getContext().getProfileStoreContext()
          .getProfileStoreConfig().getSerializer().serialize(getQueryProfile(queryId)));
      } else {
        PROFILE_CACHE.invalidate(queryId);
      }

      return Response.ok().entity(profileData).build();

    } catch (Exception e) {
      logger.debug("Failed to serialize profile for: " + queryId);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .entity("{ 'message' : 'error (unable to serialize profile)' }")
        .build();
    }
  }

  @GET
  @Path("/profiles/{queryid}")
  @Produces(MediaType.TEXT_HTML)
  public Viewable getProfile(@PathParam("queryid") String queryId){
    try {
      ProfileWrapper wrapper = new ProfileWrapper(getQueryProfile(queryId), work.getContext().getConfig(), request);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/profile/profile.ftl", sc, wrapper);
    } catch (Exception | Error e) {
      logger.error("Exception was thrown when fetching profile {} :\n{}", queryId, e);
      return ViewableWithPermissions.create(authEnabled.get(), "/rest/errorMessage.ftl", sc, e);
    }
  }

  @POST
  @Path("/profiles/view")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.TEXT_HTML)
  public Viewable viewProfile(@FormDataParam("profileData") String content) {
    try {
      QueryProfile profile = work.getContext().getProfileStoreContext()
        .getProfileStoreConfig().getSerializer().deserialize(content.getBytes());
      PROFILE_CACHE.put(profile.getQueryId(), content);
      ProfileWrapper wrapper = new ProfileWrapper(profile,
        work.getContext().getConfig(), request);
      return ViewableWithPermissions.create(authEnabled.get(),
        "/rest/profile/profile.ftl", sc, wrapper);
    } catch (Exception | Error e) {
      logger.error("Exception was thrown when parsing profile {} :\n{}",
        content, e);
      return ViewableWithPermissions.create(authEnabled.get(),
        "/rest/errorMessage.ftl", sc, e);
    }
  }

  @GET
  @Path("/profiles/cancel/{queryid}")
  @Produces(MediaType.TEXT_PLAIN)
  public String cancelQuery(@PathParam("queryid") String queryId) {

    QueryId id = QueryIdHelper.getQueryIdFromString(queryId);

    // Prevent XSS
    String encodedQueryID = forHtml(queryId);

    // first check local running
    if (work.getBee().cancelForeman(id, principal)) {
      return String.format("Cancelled query %s on locally running node.", encodedQueryID);
    }

    // then check remote running
    try {
      final TransientStore<QueryInfo> running = work.getContext().getProfileStoreContext().getRunningProfileStore();
      final QueryInfo info = running.get(queryId);
      checkOrThrowQueryCancelAuthorization(info.getUser(), queryId);
      Ack a = work.getContext().getController().getTunnel(info.getForeman()).requestCancelQuery(id).checkedGet(2, TimeUnit.SECONDS);
      if(a.getOk()){
        return String.format("Query %s canceled on node %s.", encodedQueryID, info.getForeman().getAddress());
      }else{
        return String.format("Attempted to cancel query %s on %s but the query is no longer active on that node.", encodedQueryID, info.getForeman().getAddress());
      }
    }catch(Exception e){
      logger.debug("Failure to find query as running profile.", e);
      return String.format
          ("Failure attempting to cancel query %s.  Unable to find information about where query is actively running.", encodedQueryID);
    }
  }

  private void checkOrThrowProfileViewAuthorization(final QueryProfile profile) {
    if (!principal.canManageProfileOf(profile.getUser())) {
      throw UserException.permissionError()
      .message("Not authorized to view the profile of query '%s'", profile.getId())
      .build(logger);
    }
  }

  private void checkOrThrowQueryCancelAuthorization(final String queryUser, final String queryId) {
    if (!principal.canManageQueryOf(queryUser)) {
      throw UserException.permissionError()
      .message("Not authorized to cancel the query '%s'", queryId)
      .build(logger);
    }
  }
}
