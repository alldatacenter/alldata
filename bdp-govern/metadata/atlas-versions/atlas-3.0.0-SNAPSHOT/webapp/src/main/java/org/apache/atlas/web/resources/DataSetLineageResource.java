/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.resources;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.discovery.AtlasLineageService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.lineage.AtlasLineageInfo;
import org.apache.atlas.model.lineage.AtlasLineageInfo.LineageDirection;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.atlas.v1.model.lineage.DataSetLineageResponse;
import org.apache.atlas.v1.model.lineage.SchemaResponse;
import org.apache.atlas.web.util.LineageUtils;
import org.apache.atlas.web.util.Servlets;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

import static org.apache.atlas.v1.model.lineage.SchemaResponse.SchemaDetails;

/**
 * Jersey Resource for Hive Table Lineage.
 */
@Path("lineage/hive")
@Singleton
@Service
@Deprecated
public class DataSetLineageResource {

    private static final Logger LOG      = LoggerFactory.getLogger(DataSetLineageResource.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("rest.DataSetLineageResource");

    private final AtlasLineageService atlasLineageService;
    private final AtlasTypeRegistry   typeRegistry;
    private final AtlasEntityStore    atlasEntityStore;

    @Inject
    public DataSetLineageResource(final AtlasLineageService atlasLineageService, final AtlasTypeRegistry typeRegistry, final AtlasEntityStore atlasEntityStore) {
        this.atlasLineageService = atlasLineageService;
        this.typeRegistry = typeRegistry;
        this.atlasEntityStore = atlasEntityStore;
    }

    /**
     * Returns the inputs graph for a given entity.
     *
     * @param tableName table name
     */
    @GET
    @Path("table/{tableName}/inputs/graph")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public DataSetLineageResponse inputsGraph(@Context HttpServletRequest request, @PathParam("tableName") String tableName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> DataSetLineageResource.inputsGraph({})", tableName);
        }

        DataSetLineageResponse ret  = new DataSetLineageResponse();
        AtlasPerfTracer        perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataSetLineageResource.inputsGraph(tableName=" + tableName + ")");
            }

            String guid = getGuid(tableName);

            AtlasLineageInfo lineageInfo = atlasLineageService.getAtlasLineageInfo(guid, LineageDirection.INPUT, -1);
            ret.setTableName(tableName);
            ret.setRequestId(Servlets.getRequestId());
            ret.setResults(LineageUtils.toLineageStruct(lineageInfo, typeRegistry));

            return ret;
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get lineage inputs graph for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get lineage inputs graph for table {}", tableName, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get lineage inputs graph for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Returns the outputs graph for a given entity.
     *
     * @param tableName table name
     */
    @GET
    @Path("table/{tableName}/outputs/graph")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public DataSetLineageResponse outputsGraph(@Context HttpServletRequest request, @PathParam("tableName") String tableName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> DataSetLineageResource.outputsGraph({})", tableName);
        }

        DataSetLineageResponse ret  = new DataSetLineageResponse();
        AtlasPerfTracer        perf = null;

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataSetLineageResource.outputsGraph(tableName=" + tableName + ")");
            }

            String guid = getGuid(tableName);

            AtlasLineageInfo lineageInfo = atlasLineageService.getAtlasLineageInfo(guid, LineageDirection.OUTPUT, -1);
            ret.setTableName(tableName);
            ret.setRequestId(Servlets.getRequestId());
            ret.setResults(LineageUtils.toLineageStruct(lineageInfo, typeRegistry));

            return ret;
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get lineage outputs graph for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get lineage outputs graph for table {}", tableName, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get lineage outputs graph for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    /**
     * Return the schema for the given tableName.
     *
     * @param tableName table name
     */
    @GET
    @Path("table/{tableName}/schema")
    @Consumes(Servlets.JSON_MEDIA_TYPE)
    @Produces(Servlets.JSON_MEDIA_TYPE)
    public SchemaResponse schema(@Context HttpServletRequest request, @PathParam("tableName") String tableName) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> DataSetLineageResource.schema({})", tableName);
        }

        AtlasPerfTracer perf = null;
        SchemaResponse  ret  = new SchemaResponse();

        try {
            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "DataSetLineageResource.schema(tableName=" + tableName + ")");
            }

            SchemaDetails schemaDetails = atlasLineageService.getSchemaForHiveTableByName(tableName);

            ret.setRequestId(Servlets.getRequestId());
            ret.setTableName(tableName);
            ret.setResults(schemaDetails);
            return ret;
        } catch (IllegalArgumentException e) {
            LOG.error("Unable to get schema for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.BAD_REQUEST));
        } catch (WebApplicationException e) {
            LOG.error("Unable to get schema for table {}", tableName, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("Unable to get schema for table {}", tableName, e);
            throw new WebApplicationException(Servlets.getErrorResponse(e, Response.Status.INTERNAL_SERVER_ERROR));
        } finally {
            AtlasPerfTracer.log(perf);
        }
    }

    private String getGuid(String tableName) throws AtlasBaseException {
        if (StringUtils.isEmpty(tableName)) {
            // TODO: Fix the error code if mismatch
            throw new AtlasBaseException(AtlasErrorCode.BAD_REQUEST);
        }
        Map<String, Object> lookupAttributes = new HashMap<>();
        lookupAttributes.put("qualifiedName", tableName);
        AtlasEntityType                    entityType = typeRegistry.getEntityTypeByName("hive_table");
        AtlasEntity.AtlasEntityWithExtInfo hive_table = atlasEntityStore.getByUniqueAttributes(entityType, lookupAttributes);
        if (hive_table != null) {
            return hive_table.getEntity().getGuid();
        } else {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_NOT_FOUND, tableName);
        }
    }
}
