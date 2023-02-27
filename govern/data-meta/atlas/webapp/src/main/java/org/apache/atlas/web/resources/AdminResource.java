/**
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

package org.apache.atlas.web.resources;

import com.sun.jersey.multipart.FormDataParam;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.authorize.AtlasAdminAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.authorize.AtlasEntityAccessRequest;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.discovery.SearchContext;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.audit.AtlasAuditEntry.AuditOperation;
import org.apache.atlas.model.audit.AuditSearchParameters;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.model.instance.AtlasCheckStateRequest;
import org.apache.atlas.model.instance.AtlasCheckStateResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.metrics.AtlasMetrics;
import org.apache.atlas.model.metrics.AtlasMetricsMapToChart;
import org.apache.atlas.model.metrics.AtlasMetricsStat;
import org.apache.atlas.model.patches.AtlasPatch.AtlasPatches;
import org.apache.atlas.model.tasks.AtlasTask;
import org.apache.atlas.repository.audit.AtlasAuditService;
import org.apache.atlas.repository.audit.EntityAuditRepository;
import org.apache.atlas.repository.impexp.AtlasServerService;
import org.apache.atlas.repository.impexp.ExportImportAuditService;
import org.apache.atlas.repository.impexp.ExportService;
import org.apache.atlas.repository.impexp.ImportService;
import org.apache.atlas.repository.impexp.MigrationProgressService;
import org.apache.atlas.repository.impexp.ZipSink;
import org.apache.atlas.repository.patches.AtlasPatchManager;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.services.MetricsService;
import org.apache.atlas.tasks.TaskManagement;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.SearchTracker;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.web.filters.AtlasCSRFPreventionFilter;
import org.apache.atlas.web.service.AtlasDebugMetricsSink;
import org.apache.atlas.web.service.ServiceState;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.apache.atlas.web.filters.AtlasCSRFPreventionFilter.CSRF_TOKEN;


/**
 * Jersey Resource for admin operations.
 */
@Path("admin")
@Singleton
@Service
@EnableScheduling
public class AdminResource {
    private static final Logger LOG = LoggerFactory.getLogger(AdminResource.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("AdminResource");

    private static final String isCSRF_ENABLED                 = "atlas.rest-csrf.enabled";
    private static final String BROWSER_USER_AGENT_PARAM       = "atlas.rest-csrf.browser-useragents-regex";
    private static final String CUSTOM_METHODS_TO_IGNORE_PARAM = "atlas.rest-csrf.methods-to-ignore";
    private static final String CUSTOM_HEADER_PARAM            = "atlas.rest-csrf.custom-header";
    private static final String isEntityUpdateAllowed          = "atlas.entity.update.allowed";
    private static final String isEntityCreateAllowed          = "atlas.entity.create.allowed";
    private static final String editableEntityTypes            = "atlas.ui.editable.entity.types";
    private static final String DEFAULT_EDITABLE_ENTITY_TYPES  = "hdfs_path";
    private static final String DEFAULT_UI_VERSION             = "atlas.ui.default.version";
    private static final String UI_VERSION_V2                  = "v2";
    private static final String UI_DATE_TIMEZONE_FORMAT_ENABLED = "atlas.ui.date.timezone.format.enabled";
    private static final String UI_DATE_FORMAT                 = "atlas.ui.date.format";
    private static final String UI_DATE_DEFAULT_FORMAT         = "MM/DD/YYYY hh:mm:ss A";
    private static final String OPERATION_STATUS               = "operationStatus";
    private static final List TIMEZONE_LIST                    = Arrays.asList(TimeZone.getAvailableIDs());

    private static final String METRICS_PERSIST_INTERVAL         = "atlas.metrics.persist.schedule";
    private static final String METRICS_PERSIST_INTERVAL_DEFAULT = "0 0 0/1 * * *";     // 1 hour interval

    @Context
    private HttpServletRequest httpServletRequest;

    @Context
    private HttpServletResponse httpServletResponse;

    private Response version;

    private static Configuration            atlasProperties;
    private final  ServiceState             serviceState;
    private final  MetricsService           metricsService;
    private final  ExportService            exportService;
    private final  ImportService            importService;
    private final  SearchTracker            activeSearches;
    private final  AtlasTypeRegistry        typeRegistry;
    private final  MigrationProgressService migrationProgressService;
    private final  ReentrantLock            importExportOperationLock;
    private final  ExportImportAuditService exportImportAuditService;
    private final  TaskManagement           taskManagement;
    private final  AtlasServerService       atlasServerService;
    private final  AtlasEntityStore         entityStore;
    private final  AtlasPatchManager        patchManager;
    private final  AtlasAuditService        auditService;
    private final  String                   defaultUIVersion;
    private final  EntityAuditRepository    auditRepository;
    private final  boolean                  isTimezoneFormatEnabled;
    private final  String                   uiDateFormat;
    private final  AtlasDebugMetricsSink    debugMetricsRESTSink;
    private final  boolean                  isDebugMetricsEnabled;
    private final  boolean                  isTasksEnabled;
    private final  boolean                  isOnDemandLineageEnabled;
    private final  int                      defaultLineageNodeCount;

    static {
        try {
            atlasProperties = ApplicationProperties.get();
        } catch (Exception e) {
            LOG.info("Failed to load application properties", e);
        }
    }

    @Inject
    public AdminResource(ServiceState serviceState, MetricsService metricsService, AtlasTypeRegistry typeRegistry,
                         ExportService exportService, ImportService importService, SearchTracker activeSearches,
                         MigrationProgressService migrationProgressService,
                         AtlasServerService serverService,
                         ExportImportAuditService exportImportAuditService, AtlasEntityStore entityStore,
                         AtlasPatchManager patchManager, AtlasAuditService auditService, EntityAuditRepository auditRepository,
                         TaskManagement taskManagement, AtlasDebugMetricsSink debugMetricsRESTSink) {
        this.serviceState              = serviceState;
        this.metricsService            = metricsService;
        this.exportService             = exportService;
        this.importService             = importService;
        this.activeSearches            = activeSearches;
        this.typeRegistry              = typeRegistry;
        this.migrationProgressService  = migrationProgressService;
        this.atlasServerService        = serverService;
        this.entityStore               = entityStore;
        this.exportImportAuditService  = exportImportAuditService;
        this.importExportOperationLock = new ReentrantLock();
        this.patchManager              = patchManager;
        this.auditService              = auditService;
        this.auditRepository           = auditRepository;
        this.taskManagement            = taskManagement;
        this.debugMetricsRESTSink      = debugMetricsRESTSink;

        if (atlasProperties != null) {
            this.defaultUIVersion = atlasProperties.getString(DEFAULT_UI_VERSION, UI_VERSION_V2);
            this.isTimezoneFormatEnabled = atlasProperties.getBoolean(UI_DATE_TIMEZONE_FORMAT_ENABLED, true);
            this.uiDateFormat = atlasProperties.getString(UI_DATE_FORMAT, UI_DATE_DEFAULT_FORMAT);
            this.isDebugMetricsEnabled = AtlasConfiguration.DEBUG_METRICS_ENABLED.getBoolean();
            this.isTasksEnabled = AtlasConfiguration.TASKS_USE_ENABLED.getBoolean();
            this.isOnDemandLineageEnabled = AtlasConfiguration.LINEAGE_ON_DEMAND_ENABLED.getBoolean();
            this.defaultLineageNodeCount = AtlasConfiguration.LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT.getInt();
        } else {
            this.defaultUIVersion = UI_VERSION_V2;
            this.isTimezoneFormatEnabled = true;
            this.uiDateFormat = UI_DATE_DEFAULT_FORMAT;
            this.isDebugMetricsEnabled = false;
            this.isTasksEnabled = false;
            this.isOnDemandLineageEnabled = false;
            this.defaultLineageNodeCount = 3;
        }
    }

    /**
     * Fetches the thread stack dump for this application.
     *
     * @return json representing the thread stack dump.
     */
    @GET
    @Path("stack")
    @Produces(MediaType.TEXT_PLAIN)
    public String getThreadDump() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getThreadDump()");
        }

        ThreadGroup topThreadGroup = Thread.currentThread().getThreadGroup();

        while (topThreadGroup.getParent() != null) {
            topThreadGroup = topThreadGroup.getParent();
        }
        Thread[] threads = new Thread[topThreadGroup.activeCount()];

        int nr = topThreadGroup.enumerate(threads);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < nr; i++) {
            builder.append(threads[i].getName()).append("\nState: ").
                    append(threads[i].getState()).append("\n");
            String stackTrace = StringUtils.join(threads[i].getStackTrace(), "\n");
            builder.append(stackTrace);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getThreadDump()");
        }

        return builder.toString();
    }

    /**
     * Fetches the version for this application.
     *
     * @return json representing the version.
     */
    @GET
    @Path("version")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getVersion() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getVersion()");
        }

        if (version == null) {
            try {
                PropertiesConfiguration configProperties = new PropertiesConfiguration("atlas-buildinfo.properties");

                Map<String, Object> response = new HashMap<String, Object>();
                response.put("Version", configProperties.getString("build.version", "UNKNOWN"));
                response.put("Revision",configProperties.getString("vc.revision", "UNKNOWN"));
                response.put("Name", configProperties.getString("project.name", "apache-atlas"));
                response.put("Description", configProperties.getString("project.description",
                        "Metadata Management and Data Governance Platform over Hadoop"));

                // todo: add hadoop version?
                // response.put("Hadoop", VersionInfo.getVersion() + "-r" + VersionInfo.getRevision());
                version = Response.ok(AtlasJson.toV1Json(response)).build();
            } catch (ConfigurationException e) {
                throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getVersion()");
        }

        return version;
    }

    @GET
    @Path("status")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getStatus() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getStatus()");
        }

        Map<String, Object> responseData = new HashMap() {{
                put(AtlasClient.STATUS, serviceState.getState().toString());
            }};

        if(serviceState.isInstanceInMigration()) {
            MigrationStatus status = migrationProgressService.getStatus();
            if (status != null) {
                responseData.put("MigrationStatus", status);
            }
        }

        Response response = Response.ok(AtlasJson.toV1Json(responseData)).build();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getStatus()");
        }

        return response;
    }

    @GET
    @Path("session")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Response getUserProfile(@Context HttpServletRequest request) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getUserProfile()");
        }

        Response response;

        boolean isEntityUpdateAccessAllowed = false;
        boolean isEntityCreateAccessAllowed = false;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userName = null;
        Set<String> groups = new HashSet<>();
        if (auth != null) {
            userName = auth.getName();
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            for (GrantedAuthority c : authorities) {
                groups.add(c.getAuthority());
            }

            isEntityUpdateAccessAllowed = AtlasAuthorizationUtils.isAccessAllowed(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_UPDATE));
            isEntityCreateAccessAllowed = AtlasAuthorizationUtils.isAccessAllowed(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_CREATE));
        }

        Map<String, Object> responseData = new HashMap<>();

        responseData.put(isCSRF_ENABLED, AtlasCSRFPreventionFilter.isCSRF_ENABLED);
        responseData.put(BROWSER_USER_AGENT_PARAM, AtlasCSRFPreventionFilter.BROWSER_USER_AGENTS_DEFAULT);
        responseData.put(CUSTOM_METHODS_TO_IGNORE_PARAM, AtlasCSRFPreventionFilter.METHODS_TO_IGNORE_DEFAULT);
        responseData.put(CUSTOM_HEADER_PARAM, AtlasCSRFPreventionFilter.HEADER_DEFAULT);
        responseData.put(isEntityUpdateAllowed, isEntityUpdateAccessAllowed);
        responseData.put(isEntityCreateAllowed, isEntityCreateAccessAllowed);
        responseData.put(editableEntityTypes, getEditableEntityTypes(atlasProperties));
        responseData.put(DEFAULT_UI_VERSION, defaultUIVersion);
        responseData.put("userName", userName);
        responseData.put("groups", groups);
        responseData.put("timezones", TIMEZONE_LIST);
        responseData.put(UI_DATE_TIMEZONE_FORMAT_ENABLED, isTimezoneFormatEnabled);
        responseData.put(UI_DATE_FORMAT, uiDateFormat);
        responseData.put(AtlasConfiguration.DEBUG_METRICS_ENABLED.getPropertyName(), isDebugMetricsEnabled);
        responseData.put(AtlasConfiguration.TASKS_USE_ENABLED.getPropertyName(), isTasksEnabled);
        responseData.put(AtlasConfiguration.LINEAGE_ON_DEMAND_ENABLED.getPropertyName(), isOnDemandLineageEnabled);
        responseData.put(AtlasConfiguration.LINEAGE_ON_DEMAND_DEFAULT_NODE_COUNT.getPropertyName(), defaultLineageNodeCount);

        if (AtlasConfiguration.SESSION_TIMEOUT_SECS.getInt() != -1) {
            responseData.put(AtlasConfiguration.SESSION_TIMEOUT_SECS.getPropertyName(), AtlasConfiguration.SESSION_TIMEOUT_SECS.getInt());
        }

        String salt = (String) request.getSession().getAttribute(CSRF_TOKEN);
        if (StringUtils.isEmpty(salt)) {
            salt = RandomStringUtils.random(20, 0, 0, true, true, null, new SecureRandom());
            request.getSession().setAttribute(CSRF_TOKEN, salt);
        }

        responseData.put(CSRF_TOKEN, salt);

        response = Response.ok(AtlasJson.toV1Json(responseData)).build();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getUserProfile()");
        }

        return response;
    }

    @GET
    @Path("metrics")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasMetrics getMetrics() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getMetrics()");
        }

        AtlasMetrics metrics = metricsService.getMetrics();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getMetrics()");
        }

        return metrics;
    }

    /** Auto-scheduling API for both creating a Metrics entity and saving it to the database at in preset time interval,
     *  and sweeping through entities that are outside of the valid ttl hours.
     *  @throws AtlasBaseException when the MetricsStat entity has already existed.
     */
    @Scheduled(cron="#{getCronExpression}")
    public void scheduleSaveAndDeleteMetrics() throws AtlasBaseException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.scheduleSaveAndDeleteMetrics()");
        }

        // auto persist
        saveMetrics();

        // auto purge
        metricsService.purgeMetricsStats();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.scheduleSaveAndDeleteMetrics()");
        }
    }

    /**
     * Bulk retrieval API for getting all MetricsStats, with mininfo flag return metrics with specific details, or with minimal information.
     * @return all MetricsStats in Atlas.
     * @throws AtlasBaseException when there is no MetricsStats entity in the database.
     */
    @GET
    @Path("metricsstats")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<AtlasMetricsStat> getAllMetrics(@QueryParam("mininfo") @DefaultValue("true") Boolean minInfo) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "AdminResource.getAllMetrics()");
            }

            return metricsService.getAllMetricsStats(minInfo);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Retrieval API for retrieving the MetricsStat with a specific collectionTime.
     * @return the MetricsStat with the specific collectionTime.
     * @throws AtlasBaseException when the MetricsStat entity with this specific collectionTime cannot be found.
     */
    @GET
    @Path("metricsstat/{collectionTime}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasMetricsStat getMetricsByCollectionTime(@PathParam("collectionTime") String collectionTime) throws AtlasBaseException {
        Servlets.validateQueryParamLength("collectionTime", collectionTime);

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer
                        .getPerfTracer(PERF_LOG,
                                "AdminResource.getMetricsByCollectionTime(collectionTime=" + collectionTime + ")");
            }

            return metricsService.getMetricsStatByCollectionTime(collectionTime);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /** Retrieval API for retrieving persisted MetricsStats with collectionTime within range of startTime and endTime.
     * @param startTime start timestamp of the time range.
     * @param endTime   end timestamp of the time range.
     * @param typeNames a list of typeNames with their counting information, as well as their metrics' minimal information.
     * @return          persisted Metrics with its collectionTime within time range, in the form of minimal information.
     * @throws AtlasBaseException when the input of startTime and endTime is null or invalid.
     */
    @GET
    @Path("metricsstats/range")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<AtlasMetricsStat> getMetricsInTimeRange(@QueryParam("startTime") String startTime,
                                                        @QueryParam("endTime")   String endTime,
                                                        @QueryParam("typeName")  List<String> typeNames) throws AtlasBaseException {
        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "startTime or endTime is null/empty.");
        }

        Servlets.validateQueryParamLength("startTime", startTime);
        Servlets.validateQueryParamLength("endTime", endTime);
        for (String typeName : typeNames) {
            Servlets.validateQueryParamLength("typeName", typeName);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG,
                        "AdminResource.getMetricsInTimeRange(startTime=" + startTime + ", " +
                                "endTime="   + endTime   + ", " +
                                "listOfTypeNames="  + String.join(", ", typeNames)  + ")" );
            }

            return metricsService.getMetricsInRangeByTypeNames(Long.parseLong(startTime), Long.parseLong(endTime), typeNames);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /** Retrieval API for retrieving & formatting MetricsStats (within valid range) to render stacked area chart. The process contains:
     * 1. retrieve persisted MetricsStats with collectionTime within range of startTime and endTime by one typeName
     * 2. map the returned MetricsStats to the required format for rendering stacked area chart
     * Currently, one typeName corresponds to one chart. The API can take multiple typeNames. The returned JSON file can be used to render multiple charts.
     * @param startTime start timestamp of the time range.
     * @param endTime   end timestamp of the time range.
     * @param typeNames a list of typeNames with their counting information, as well as their metrics' minimal information.
     * @return          formatted metrics to render one or multiple stacked area charts.
     * @throws AtlasBaseException when the input of startTime and endTime is null or invalid.
     */
    @GET
    @Path("metricsstats/charts")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Map<String, List<AtlasMetricsMapToChart>> getMetricsForChartByTypeNames(
            @QueryParam("startTime")  String startTime,
            @QueryParam("endTime")    String endTime,
            @QueryParam("typeName")   List<String> typeNames) throws AtlasBaseException {

        if (StringUtils.isBlank(startTime) || StringUtils.isBlank(endTime)) {
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST, "startTime or endTime is null/empty.");
        }

        Servlets.validateQueryParamLength("startTime", startTime);
        Servlets.validateQueryParamLength("endTime", endTime);
        for (String typeName : typeNames) {
            Servlets.validateQueryParamLength("typeName", typeName);
        }

        AtlasPerfTracer perf = null;
        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG,
                        "AdminResource.getMetricsForChartByTypeNames(" +
                                "startTime="        + startTime + ", " +
                                "endTime="          + endTime   + ", " +
                                "listOfTypeNames="  + String.join(", ", typeNames)  + ")" );
            }

            return metricsService.getMetricsForChartByTypeNames(Long.parseLong(startTime), Long.parseLong(endTime), typeNames);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @POST
    @Path("/export")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    public Response export(AtlasExportRequest request) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.export()");
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasAdminAccessRequest(AtlasPrivilege.ADMIN_EXPORT), "export");

        boolean preventMultipleRequests = request != null && request.getOptions() != null
                && !(request.getOptions().containsKey(AtlasExportRequest.OPTION_SKIP_LINEAGE)
                     || request.getOptions().containsKey(AtlasExportRequest.OPTION_KEY_REPLICATED_TO));
        if (preventMultipleRequests) {
            acquireExportImportLock("export");
        }

        ZipSink exportSink = null;
        boolean isSuccessful = false;
        AtlasExportResult result = null;
        try {
            exportSink = new ZipSink(httpServletResponse.getOutputStream());
            result = exportService.run(exportSink, request, AtlasAuthorizationUtils.getCurrentUserName(),
                                                         Servlets.getHostName(httpServletRequest),
                                                         AtlasAuthorizationUtils.getRequestIpAddress(httpServletRequest));

            exportSink.close();

            httpServletResponse.addHeader("Content-Encoding","gzip");
            httpServletResponse.setContentType("application/zip");
            httpServletResponse.setHeader("Content-Disposition",
                                          "attachment; filename=" + result.getClass().getSimpleName());
            httpServletResponse.setHeader("Transfer-Encoding", "chunked");

            httpServletResponse.getOutputStream().flush();
            isSuccessful = true;
            return Response.ok().build();
        } catch (IOException excp) {
            LOG.error("export() failed", excp);

            throw new AtlasBaseException(excp);
        } finally {
            if (preventMultipleRequests) {
                releaseExportImportLock();
            }

            if (exportSink != null) {
                exportSink.close();
            }

            addToExportOperationAudits(isSuccessful, result);

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== AdminResource.export()");
            }
        }
    }

    @POST
    @Path("/import")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public AtlasImportResult importData(@DefaultValue("{}") @FormDataParam("request") String jsonData,
                                        @FormDataParam("data") InputStream inputStream) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.importData(jsonData={}, inputStream={})", jsonData, (inputStream != null));
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasAdminAccessRequest(AtlasPrivilege.ADMIN_IMPORT), "importData");

        AtlasImportResult result = null;
        boolean preventMultipleRequests = true;

        try {
            AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);
            preventMultipleRequests = request != null && request.getOptions() != null
                    && !request.getOptions().containsKey(AtlasImportRequest.OPTION_KEY_REPLICATED_FROM);
            if (preventMultipleRequests) {
                acquireExportImportLock("import");
            }

            result = importService.run(inputStream, request, Servlets.getUserName(httpServletRequest),
                    Servlets.getHostName(httpServletRequest),
                    AtlasAuthorizationUtils.getRequestIpAddress(httpServletRequest));
        } catch (AtlasBaseException excp) {
            if (excp.getAtlasErrorCode().equals(AtlasErrorCode.IMPORT_ATTEMPTING_EMPTY_ZIP)) {
                LOG.info(excp.getMessage());
                return new AtlasImportResult();
            } else {
                LOG.error("importData(binary) failed", excp);
                throw excp;
            }

        } catch (Exception excp) {
            LOG.error("importData(binary) failed", excp);

            throw new AtlasBaseException(excp);
        } finally {
            if (preventMultipleRequests) {
                releaseExportImportLock();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== AdminResource.importData(binary)");
            }
        }

        addToImportOperationAudits(result);

        return result;
    }

    @PUT
    @Path("/purge")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public EntityMutationResponse purgeByIds(Set<String> guids) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(guids)) {
            for (String guid : guids) {
                Servlets.validateQueryParamLength("guid", guid);
            }
        }

        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "AdminResource.purgeByIds(" + guids  + ")");
            }

            EntityMutationResponse resp =  entityStore.purgeByIds(guids);

            final List<AtlasEntityHeader> purgedEntities = resp.getPurgedEntities();
            if(purgedEntities != null && purgedEntities.size() > 0) {
                auditService.add(AuditOperation.PURGE, guids.toString(), resp.getPurgedEntitiesIds(),
                        resp.getPurgedEntities().size());
            }

            return resp;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @POST
    @Path("/importfile")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasImportResult importFile(String jsonData) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.importFile()");
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasAdminAccessRequest(AtlasPrivilege.ADMIN_IMPORT), "importFile");
        boolean preventMultipleRequests = true;
        AtlasImportResult result;

        try {
            AtlasImportRequest request = AtlasType.fromJson(jsonData, AtlasImportRequest.class);
            preventMultipleRequests = request != null && request.getOptions() != null
                    && request.getOptions().containsKey(AtlasImportRequest.OPTION_KEY_REPLICATED_FROM);

            if (preventMultipleRequests) {
                acquireExportImportLock("importFile");
            }

            result = importService.run(request, AtlasAuthorizationUtils.getCurrentUserName(),
                                       Servlets.getHostName(httpServletRequest),
                                       AtlasAuthorizationUtils.getRequestIpAddress(httpServletRequest));
        } catch (AtlasBaseException excp) {
            if (excp.getAtlasErrorCode().getErrorCode().equals(AtlasErrorCode.IMPORT_ATTEMPTING_EMPTY_ZIP)) {
                LOG.info(excp.getMessage());
            } else {
                LOG.error("importData(binary) failed", excp);
            }

            throw excp;
        } catch (Exception excp) {
            LOG.error("importFile() failed", excp);

            throw new AtlasBaseException(excp);
        } finally {
            if (preventMultipleRequests) {
                releaseExportImportLock();
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== AdminResource.importFile()");
            }
        }

        return result;
    }

    /**
     * Fetch details of a cluster.
     * @param serverName name of target cluster with which it is paired
     * @return AtlasServer
     * @throws AtlasBaseException
     */
    @GET
    @Path("/server/{serverName}")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasServer getCluster(@PathParam("serverName") String serverName) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "cluster.getServer(" + serverName + ")");
            }

            AtlasServer cluster = new AtlasServer(serverName, serverName);
            return atlasServerService.get(cluster);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @GET
    @Path("/expimp/audit")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<ExportImportAuditEntry> getExportImportAudit(@QueryParam("serverName") String serverName,
                                                             @QueryParam("userName") String userName,
                                                             @QueryParam("operation") String operation,
                                                             @QueryParam("startTime") String startTime,
                                                             @QueryParam("endTime") String endTime,
                                                             @QueryParam("limit") int limit,
                                                             @QueryParam("offset") int offset) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "getExportImportAudit(" + serverName + ")");
            }

            return exportImportAuditService.get(userName, operation, serverName, startTime, endTime, limit, offset);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @POST
    @Path("/audits")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<AtlasAuditEntry> getAtlasAudits(AuditSearchParameters auditSearchParameters) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "AdminResource.getAtlasAudits(" + auditSearchParameters + ")");
            }

            AtlasAuthorizationUtils.verifyAccess(new AtlasAdminAccessRequest(AtlasPrivilege.ADMIN_AUDITS), "Admin Audits");

            return auditService.get(auditSearchParameters);
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }


    @GET
    @Path("/audit/{auditGuid}/details")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<AtlasEntityHeader> getAuditDetails(@PathParam("auditGuid") String auditGuid,
                                    @QueryParam("limit") @DefaultValue("10") int limit,
                                    @QueryParam("offset") @DefaultValue("0") int offset) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "AdminResource.getAuditDetails(" + auditGuid + ", " + limit + ", " + offset + ")");
            }

            List<AtlasEntityHeader> ret = new ArrayList<>();

            AtlasAuditEntry auditEntry = auditService.toAtlasAuditEntry(entityStore.getById(auditGuid, false, true));

            if(auditEntry != null && StringUtils.isNotEmpty(auditEntry.getResult())) {
                String[] listOfResultGuid = auditEntry.getResult().split(",");
                EntityAuditActionV2 auditAction = auditEntry.getOperation().toEntityAuditActionV2();

                if(offset <= listOfResultGuid.length) {
                    for(int index=offset; index < listOfResultGuid.length && index < (offset + limit); index++) {
                        List<EntityAuditEventV2> events = auditRepository.listEventsV2(listOfResultGuid[index], auditAction, null, (short)1);

                        for (EntityAuditEventV2 event : events) {
                            AtlasEntityHeader entityHeader = event.getEntityHeader();
                            if(entityHeader != null) {
                                ret.add(entityHeader);
                            }
                        }
                    }
                }
            }

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @GET
    @Path("activeSearches")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public Set<String> getActiveSearches() {
        return activeSearches.getActiveSearches();
    }

    @DELETE
    @Path("activeSearches/{id}")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public boolean terminateActiveSearch(@PathParam("id") String searchId) {
        SearchContext terminate = activeSearches.terminate(searchId);
        return null != terminate;
    }

    @POST
    @Path("checkstate")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    public AtlasCheckStateResult checkState(AtlasCheckStateRequest request) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "checkState(" + request + ")");
            }

            AtlasCheckStateResult ret = entityStore.checkState(request);

            return ret;
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    @GET
    @Path("patches")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public AtlasPatches getAtlasPatches() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.getAtlasPatches()");
        }

        AtlasPatches ret = patchManager.getAllPatches();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.getAtlasPatches()");
        }

        return ret;
    }

    @GET
    @Path("/tasks")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public List<AtlasTask> getTaskStatus(@QueryParam("guids") List<String> guids) throws AtlasBaseException {
        return CollectionUtils.isNotEmpty(guids) ? taskManagement.getByGuids(guids) : taskManagement.getAll();
    }

    @DELETE
    @Path("/tasks")
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public void deleteTask(@QueryParam("guids") List<String> guids) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(guids)) {
            taskManagement.deleteByGuids(guids);
        }
    }

    @GET
    @Path("/debug/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Map getDebugMetrics() {
        return debugMetricsRESTSink.getMetrics();
    }

    private String getEditableEntityTypes(Configuration config) {
        String ret = DEFAULT_EDITABLE_ENTITY_TYPES;

        if (config != null && config.containsKey(editableEntityTypes)) {
            Object value = config.getProperty(editableEntityTypes);

            if (value instanceof String) {
                ret = (String) value;
            } else if (value instanceof Collection) {
                StringBuilder sb = new StringBuilder();

                for (Object elem : ((Collection) value)) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }

                    sb.append(elem.toString());
                }

                ret = sb.toString();
            }
        }

        return ret;
    }

    private void acquireExportImportLock(String activity) throws AtlasBaseException {
        boolean alreadyLocked = importExportOperationLock.isLocked();
        if (alreadyLocked) {
            LOG.warn("Another export or import is currently in progress..aborting this " + activity, Thread.currentThread().getName());

            throw new AtlasBaseException(AtlasErrorCode.FAILED_TO_OBTAIN_IMPORT_EXPORT_LOCK);
        }

        importExportOperationLock.lock();
    }

    private void addToImportOperationAudits(AtlasImportResult result) throws AtlasBaseException {
        Map<String, Object> optionMap = new HashMap<>();
        optionMap.put(OPERATION_STATUS, result.getOperationStatus().name());
        String params = AtlasJson.toJson(optionMap);

        if(result.getExportResult().getRequest() == null) {
            int resultCount = result.getProcessedEntities().size();
            auditService.add(AuditOperation.IMPORT, params, AtlasJson.toJson(result.getMetrics()), resultCount);
        } else {
            List<AtlasObjectId> objectIds = result.getExportResult().getRequest().getItemsToExport();
            auditImportExportOperations(objectIds, AuditOperation.IMPORT, params);
        }
    }

    private void addToExportOperationAudits(boolean isSuccessful, AtlasExportResult result) throws AtlasBaseException {
        if (!isSuccessful
                || CollectionUtils.isEmpty(result.getRequest().getItemsToExport())
                || result.getRequest().getOptions() == null) {
            return;
        }

        Map<String, Object> optionMap = result.getRequest().getOptions();
        optionMap.put(OPERATION_STATUS, result.getOperationStatus().name());
        String params = AtlasJson.toJson(optionMap);

        List<AtlasObjectId> objectIds = result.getRequest().getItemsToExport();

        auditImportExportOperations(objectIds, AuditOperation.EXPORT, params);
    }

    private void auditImportExportOperations(List<AtlasObjectId> objectIds, AuditOperation auditOperation, String params) throws AtlasBaseException {

        Map<String, Long> entityCountByType = objectIds.stream().collect(Collectors.groupingBy(AtlasObjectId::getTypeName, Collectors.counting()));
        int resultCount = objectIds.size();

        auditService.add(auditOperation, params, AtlasJson.toJson(entityCountByType), resultCount);
    }

    private void releaseExportImportLock() {
        importExportOperationLock.unlock();
    }

    /** Get customized time interval to persist metrics in CM, or use default persist hour (1hr interval).
     * There are 6 fields. Default 1 hr interval: 0 0 0/1 * * *
     */
    @Bean
    private String getCronExpression() {
        if (atlasProperties != null) {
            return atlasProperties.getString(METRICS_PERSIST_INTERVAL, METRICS_PERSIST_INTERVAL_DEFAULT);
        } else {
            return METRICS_PERSIST_INTERVAL_DEFAULT;
        }
    }

    /** Save an AtlasMetrics as AtlasMetricsStat to db.
     * @throws AtlasBaseException when the AtlasMetricsStat is null or when the AtlasMetricsStat already exists in db.
     */
    private void saveMetrics() throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> AdminResource.saveMetrics()");
        }

        AtlasMetrics metrics = metricsService.getMetrics();

        AtlasMetricsStat metricsStat = new AtlasMetricsStat(metrics);
        metricsService.saveMetricsStat(metricsStat);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== AdminResource.saveMetrics()");
        }
    }

}
