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
package org.apache.oozie.ambari.view;

import static org.apache.oozie.ambari.view.Constants.MESSAGE_KEY;
import static org.apache.oozie.ambari.view.Constants.STATUS_KEY;
import static org.apache.oozie.ambari.view.Constants.STATUS_OK;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.view.ViewContext;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.oozie.ambari.view.assets.AssetResource;
import org.apache.oozie.ambari.view.exception.ErrorCode;
import org.apache.oozie.ambari.view.exception.WfmException;
import org.apache.oozie.ambari.view.exception.WfmWebException;
import org.apache.oozie.ambari.view.workflowmanager.WorkflowManagerService;
import org.apache.oozie.ambari.view.workflowmanager.WorkflowsManagerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;

import org.json.simple.JSONObject;




/**
 * This is a class used to bridge the communication between the and the Oozie
 * API executing inside ambari.
 */
@Singleton
public class OozieProxyImpersonator {
  private final static Logger LOGGER = LoggerFactory
    .getLogger(OozieProxyImpersonator.class);
  private static final boolean PROJ_MANAGER_ENABLED = true;
  public static final String RESPONSE_TYPE = "response-type";
  public static final String OLDER_FORMAT_DRAFT_INGORED = "olderFormatDraftIngored";

  private final ViewContext viewContext;
  private final Utils utils = new Utils();


  private final HDFSFileUtils hdfsFileUtils;
  private final WorkflowFilesService workflowFilesService;
  private WorkflowManagerService workflowManagerService;

  private final OozieDelegate oozieDelegate;
  private final OozieUtils oozieUtils = new OozieUtils();
  private final AssetResource assetResource;


  private static enum WorkflowFormat{
    XML("xml"),
    DRAFT("draft");
    String value;
    WorkflowFormat(String value) {
      this.value=value;
    }

    public String getValue() {
      return value;
    }
  }
  @Inject
  public OozieProxyImpersonator(ViewContext viewContext) {
    this.viewContext = viewContext;
    hdfsFileUtils = new HDFSFileUtils(viewContext);
    workflowFilesService = new WorkflowFilesService(hdfsFileUtils);
    this.oozieDelegate = new OozieDelegate(viewContext);
    assetResource = new AssetResource(viewContext);
    if (PROJ_MANAGER_ENABLED) {
      workflowManagerService = new WorkflowManagerService(viewContext);
    }

    LOGGER.info(String.format(
      "OozieProxyImpersonator initialized for instance: %s",
      viewContext.getInstanceName()));

  }

  @GET
  @Path("hdfsCheck")
  public Response hdfsCheck(){
    try {
      hdfsFileUtils.hdfsCheck();
      return Response.ok().build();
    }catch (Exception ex){
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @GET
  @Path("homeDirCheck")
  public Response homeDirCheck(){
    try{
      hdfsFileUtils.homeDirCheck();
      return Response.ok().build();
    }catch (Exception ex){
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @Path("/fileServices")
  public FileServices fileServices() {
    return new FileServices(viewContext);
  }

  @Path("/wfprojects")
  public WorkflowsManagerResource workflowsManagerResource() {
    return new WorkflowsManagerResource(viewContext);
  }

  @Path("/assets")
  public AssetResource assetResource() {
    return this.assetResource;
  }

  @GET
  @Path("/getCurrentUserName")
  public Response getCurrentUserName() {

    JSONObject obj = new JSONObject();

    obj.put("username", viewContext.getUsername());

    return Response.ok(obj).build();
  }

  @GET
  @Path("/getWorkflowManagerConfigs")
  public Response getWorkflowConfigs() {
    try {
      HashMap<String, String> workflowConfigs = new HashMap<String, String>();
      workflowConfigs.put("nameNode", viewContext.getProperties().get("webhdfs.url"));
      workflowConfigs.put("resourceManager", viewContext.getProperties().get("yarn.resourcemanager.address"));
      workflowConfigs.put("userName", viewContext.getUsername());
      workflowConfigs.put("checkHomeDir",hdfsFileUtils.shouldCheckForHomeDir().toString());
      return Response.ok(workflowConfigs).build();
    } catch (Exception e) {
      LOGGER.error(e.getMessage(), e);
      throw new WfmWebException(e);
    }
  }

  @POST
  @Path("/submitJob")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response submitJob(String postBody, @Context HttpHeaders headers,
                            @Context UriInfo ui, @QueryParam("app.path") String appPath,
                            @QueryParam("projectId") String projectId,
                            @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite,
                            @QueryParam("description") String description,
                            @QueryParam("jobType") String jobTypeString) {
    LOGGER.info("submit workflow job called");
    JobType jobType = JobType.valueOf(jobTypeString);
    if (StringUtils.isEmpty(appPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    appPath = workflowFilesService.getWorkflowFileName(appPath.trim(), jobType);
    try {
      if (!overwrite) {
        boolean fileExists = hdfsFileUtils.fileExists(appPath);
        if (fileExists) {
          throw new WfmWebException(ErrorCode.WORKFLOW_PATH_EXISTS);
        }
      }
      postBody = utils.formatXml(postBody);

      String filePath = workflowFilesService.createFile(appPath, postBody, overwrite);
      LOGGER.info(String.format("submit workflow job done. filePath=[%s]", filePath));

      if (PROJ_MANAGER_ENABLED) {
        String name = oozieUtils.deduceWorkflowNameFromXml(postBody);
        workflowManagerService.saveWorkflow(projectId, appPath, jobType,
          null, viewContext.getUsername(), name);
      }
      String response = oozieDelegate.submitWorkflowJobToOozie(headers,
        appPath, ui.getQueryParameters(), jobType);
      return Response.status(Status.OK).entity(response).build();
    } catch (WfmWebException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw ex;
    } catch(WfmException ex){
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex,ex.getErrorCode());
    } catch(Exception ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @POST
  @Path("/saveWorkflow")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response saveWorkflow(String postBody, @Context HttpHeaders headers,
                               @Context UriInfo ui, @QueryParam("app.path") String appPath,
                               @QueryParam("jobType") String jobTypeStr,
                               @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
    LOGGER.info("save workflow  called");
    if (StringUtils.isEmpty(appPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    JobType jobType = StringUtils.isEmpty(jobTypeStr) ? JobType.WORKFLOW : JobType.valueOf(jobTypeStr);
    String workflowFilePath = workflowFilesService.getWorkflowFileName(appPath.trim(), jobType);
    try {
      if (!overwrite) {
        boolean fileExists = hdfsFileUtils.fileExists(workflowFilePath);
        if (fileExists) {
          throw new WfmWebException(ErrorCode.WORKFLOW_PATH_EXISTS);
        }
      }
      if (utils.isXml(postBody)) {
        saveWorkflowXml(jobType, appPath, postBody, overwrite);
      } else {
        saveDraft(jobType, appPath, postBody, overwrite);
      }
      if (PROJ_MANAGER_ENABLED) {
        workflowManagerService.saveWorkflow(null, workflowFilePath, jobType, null,
          viewContext.getUsername(), getWorkflowName(postBody));
      }
    } catch (WfmWebException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
    return Response.ok().build();
  }

  private String getWorkflowName(String postBody) {
    if (utils.isXml(postBody)) {
      return oozieUtils.deduceWorkflowNameFromXml(postBody);
    } else {
      return oozieUtils.deduceWorkflowNameFromJson(postBody);
    }
  }

  private void saveWorkflowXml(JobType jobType, String appPath, String postBody,
                               Boolean overwrite) throws IOException {
    appPath = workflowFilesService.getWorkflowFileName(appPath.trim(), jobType);
    postBody = utils.formatXml(postBody);
    workflowFilesService.createFile(appPath, postBody, overwrite);
    String workflowDraftPath = workflowFilesService.getWorkflowDraftFileName(appPath.trim(), jobType);
    if (hdfsFileUtils.fileExists(workflowDraftPath)) {
      hdfsFileUtils.deleteFile(workflowDraftPath);
    }
  }

  private void saveDraft(JobType jobType, String appPath, String postBody, Boolean overwrite) throws IOException {
    String workflowFilePath = workflowFilesService.getWorkflowFileName(appPath.trim(), jobType);
    if (!hdfsFileUtils.fileExists(workflowFilePath)) {
      String noOpWorkflow = oozieUtils.getNoOpWorkflowXml(postBody, jobType);
      workflowFilesService.createFile(workflowFilePath, noOpWorkflow, overwrite);
    }
    String workflowDraftPath = workflowFilesService.getWorkflowDraftFileName(appPath.trim(), jobType);
    workflowFilesService.createFile(workflowDraftPath, postBody, true);
  }

  @POST
  @Path("/publishAsset")
  @Consumes({MediaType.TEXT_PLAIN + "," + MediaType.TEXT_XML})
  public Response publishAsset(String postBody, @Context HttpHeaders headers,
                               @Context UriInfo ui, @QueryParam("uploadPath") String uploadPath,
                               @DefaultValue("false") @QueryParam("overwrite") Boolean overwrite) {
    LOGGER.info("publish asset called");
    if (StringUtils.isEmpty(uploadPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    uploadPath = uploadPath.trim();
    try {
      Map<String, String> validateAsset = assetResource.validateAsset(headers, postBody,
        ui.getQueryParameters());
      if (!STATUS_OK.equals(validateAsset.get(STATUS_KEY))) {
        WfmWebException wfmEx=new WfmWebException(ErrorCode.INVALID_ASSET_INPUT);
        wfmEx.setAdditionalDetail(validateAsset.get(MESSAGE_KEY));
        throw wfmEx;
      }
      return saveAsset(postBody, uploadPath, overwrite);
    } catch (WfmWebException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  private Response saveAsset(String postBody, String uploadPath, Boolean overwrite) throws IOException {
    uploadPath = workflowFilesService.getAssetFileName(uploadPath);
    if (!overwrite) {
      boolean fileExists = hdfsFileUtils.fileExists(uploadPath);
      if (fileExists) {
        throw new WfmWebException(ErrorCode.WORKFLOW_PATH_EXISTS);
      }
    }
    postBody = utils.formatXml(postBody);
    String filePath = workflowFilesService.createAssetFile(uploadPath, postBody, overwrite);
    LOGGER.info(String.format("publish asset job done. filePath=[%s]", filePath));
    return Response.ok().build();
  }

  @GET
  @Path("/readAsset")
  public Response readAsset(@QueryParam("assetPath") String assetPath) {
    if (StringUtils.isEmpty(assetPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    try {
      final InputStream is = workflowFilesService.readAssset(assetPath);
      StreamingOutput streamer = utils.streamResponse(is);
      return Response.ok(streamer).status(200).build();
    } catch (IOException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @GET
  @Path("/readWorkflowDraft")
  public Response readDraft(@QueryParam("workflowXmlPath") String workflowPath) {
    if (StringUtils.isEmpty(workflowPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    try {
      final InputStream is = workflowFilesService.readDraft(workflowPath);
      StreamingOutput streamer = utils.streamResponse(is);
      return Response.ok(streamer).status(200).build();
    } catch (IOException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @POST
  @Path("/discardWorkflowDraft")
  public Response discardDraft(
    @QueryParam("workflowXmlPath") String workflowPath) {
    try {
      workflowFilesService.discardDraft(workflowPath);
      return Response.ok().build();
    } catch (IOException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @GET
  @Path("/readWorkflow")
  public Response readWorkflow(
    @QueryParam("workflowPath") String workflowPath, @QueryParam("jobType") String jobTypeStr) {
    try {
      String workflowFileName = workflowFilesService.getWorkflowFileName(workflowPath, JobType.valueOf(jobTypeStr));
      if (!hdfsFileUtils.fileExists(workflowFileName)) {
        throw new WfmWebException(ErrorCode.WORKFLOW_XML_DOES_NOT_EXIST);
      }
      WorkflowFileInfo workflowDetails = workflowFilesService
        .getWorkflowDetails(workflowPath, JobType.valueOf(jobTypeStr));
      if (workflowPath.endsWith(Constants.WF_DRAFT_EXTENSION) || workflowDetails.getIsDraftCurrent()) {
        String filePath = workflowFilesService.getWorkflowDraftFileName(workflowPath, JobType.valueOf(jobTypeStr));

        InputStream inputStream = workflowFilesService.readWorkflowXml(filePath);
        String stringResponse = IOUtils.toString(inputStream);
        if (!workflowFilesService.isDraftFormatCurrent(stringResponse)) {
          filePath = workflowFilesService.getWorkflowFileName(workflowPath, JobType.valueOf(jobTypeStr));
          return getWorkflowResponse(filePath, WorkflowFormat.XML.getValue(), true);
        } else {
          return Response.ok(stringResponse).header(RESPONSE_TYPE, WorkflowFormat.DRAFT.getValue()).build();
        }
      } else {
        String filePath = workflowFilesService.getWorkflowFileName(workflowPath, JobType.valueOf(jobTypeStr));
        return getWorkflowResponse(filePath, WorkflowFormat.XML.getValue(), false);
      }
    } catch (WfmWebException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  private Response getWorkflowResponse(String filePath, String responseType,
                                       boolean olderFormatDraftIngored) throws IOException {
    final InputStream is = workflowFilesService.readWorkflowXml(filePath);
    StreamingOutput streamer = utils.streamResponse(is);
    Response.ResponseBuilder responseBuilder = Response.ok(streamer).header(RESPONSE_TYPE, responseType);
    if (olderFormatDraftIngored) {
      responseBuilder.header(OLDER_FORMAT_DRAFT_INGORED, Boolean.TRUE.toString());
    }
    return responseBuilder.build();

  }

  @GET
  @Path("/readWorkflowXml")
  public Response readWorkflowXml(
    @QueryParam("workflowXmlPath") String workflowPath,@QueryParam("jobType") String jobTypeStr) {
    if (StringUtils.isEmpty(workflowPath)) {
      throw new WfmWebException(ErrorCode.INVALID_EMPTY_INPUT);
    }
    try {
      if (!hdfsFileUtils.fileExists(workflowPath)) {
        throw new WfmWebException(ErrorCode.WORKFLOW_XML_DOES_NOT_EXIST);
      }
      final InputStream is = workflowFilesService.readWorkflowXml(workflowPath);
      StreamingOutput streamer = utils.streamResponse(is);
      return Response.ok(streamer).status(200).build();
    } catch (WfmWebException ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw ex;
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(),ex);
      throw new WfmWebException(ex);
    }
  }

  @GET
  @Path("/{path: .*}")
  public Response handleGet(@Context HttpHeaders headers, @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.GET, null);
    } catch (Exception ex) {
      LOGGER.error("Error in GET proxy", ex);
      throw new WfmWebException(ex);
    }
  }

  @POST
  @Path("/{path: .*}")
  public Response handlePost(String xml, @Context HttpHeaders headers,
                             @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.POST, xml);
    } catch (Exception ex) {
      LOGGER.error("Error in POST proxy", ex);
      throw new WfmWebException(ex);
    }
  }

  @DELETE
  @Path("/{path: .*}")
  public Response handleDelete(@Context HttpHeaders headers,
                               @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.POST, null);
    } catch (Exception ex) {
      LOGGER.error("Error in DELETE proxy", ex);
      throw new WfmWebException(ex);
    }
  }

  @PUT
  @Path("/{path: .*}")
  public Response handlePut(String body, @Context HttpHeaders headers,
                            @Context UriInfo ui) {
    try {
      return oozieDelegate.consumeService(headers, ui.getAbsolutePath()
        .getPath(), ui.getQueryParameters(), HttpMethod.PUT, body);
    } catch (Exception ex) {
      LOGGER.error("Error in PUT proxy", ex);
      throw new WfmWebException(ex);
    }
  }
}
