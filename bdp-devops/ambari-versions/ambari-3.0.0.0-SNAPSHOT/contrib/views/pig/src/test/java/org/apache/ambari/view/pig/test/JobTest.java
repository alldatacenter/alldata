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

package org.apache.ambari.view.pig.test;

import org.apache.ambari.view.pig.BasePigTest;
import org.apache.ambari.view.pig.resources.jobs.JobResourceManager;
import org.apache.ambari.view.pig.resources.jobs.JobService;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.utils.BadRequestFormattedException;
import org.apache.ambari.view.pig.utils.NotFoundFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.ambari.view.pig.utils.UserLocalObjects;
import org.apache.ambari.view.utils.UserLocal;
import org.apache.ambari.view.utils.hdfs.HdfsApi;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.easymock.EasyMock;
import org.json.simple.JSONObject;
import org.junit.*;
import org.junit.rules.ExpectedException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.List;

import static org.easymock.EasyMock.*;

public class JobTest extends BasePigTest {
  private JobService jobService;
  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void startUp() throws Exception {
    BasePigTest.startUp(); // super
  }

  @AfterClass
  public static void shutDown() throws Exception {
    BasePigTest.shutDown(); // super
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    jobService = getService(JobService.class, handler, context);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    UserLocal.dropAllConnections(TempletonApi.class);
    UserLocal.dropAllConnections(HdfsApi.class);
  }

  public static Response doCreateJob(String title, String pigScript, String templetonArguments, JobService jobService) {
    return doCreateJob(title, pigScript, templetonArguments, null, null, jobService);
  }

  public static Response doCreateJob(String title, String pigScript, String templetonArguments, String forcedContent, String scriptId, JobService jobService) {
    JobService.PigJobRequest request = new JobService.PigJobRequest();
    request.job = new PigJob();
    request.job.setTitle(title);
    request.job.setPigScript(pigScript);
    request.job.setTempletonArguments(templetonArguments);
    request.job.setForcedContent(forcedContent);
    request.job.setScriptId(scriptId);

    UriInfo uriInfo = createNiceMock(UriInfo.class);
    URI uri = UriBuilder.fromUri("http://host/a/b").build();
    expect(uriInfo.getAbsolutePath()).andReturn(uri);

    HttpServletResponse resp_obj = createStrictMock(HttpServletResponse.class);

    resp_obj.setHeader(eq("Location"), anyString());

    replay(uriInfo, resp_obj);
    return jobService.runJob(request, resp_obj, uriInfo);
  }

  @Test
  public void testSubmitJob() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_1466418324742_0005";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);

    Assert.assertEquals("-useHCatalog", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertNotNull(((PigJob) obj.get("job")).getId());
    Assert.assertFalse(((PigJob) obj.get("job")).getId().isEmpty());
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/test"));

    PigJob job = ((PigJob) obj.get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());
  }

  @Test
  public void testListJobs() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream).anyTimes();
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_1466418324742_0005";
    expect(api.runPigQuery((File) anyObject(), anyString(), (String) isNull())).andReturn(data).anyTimes();
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", null, null, "x42", jobService);
    Assert.assertEquals(201, response.getStatus());

    response = doCreateJob("Test", "/tmp/script.pig", null, null, "x42", jobService);
    Assert.assertEquals(201, response.getStatus());

    response = doCreateJob("Test", "/tmp/script.pig", null, null, "100", jobService);
    Assert.assertEquals(201, response.getStatus());

    response = jobService.getJobList("x42");
    Assert.assertEquals(200, response.getStatus());
    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("jobs"));
    Assert.assertEquals(2, ((List) obj.get("jobs")).size());

    response = jobService.getJobList(null);
    Assert.assertEquals(200, response.getStatus());
    obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("jobs"));
    Assert.assertTrue(((List) obj.get("jobs")).size() > 2);
  }

  @Test
  public void testSubmitJobUsernameProvided() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_1466418324742_0005";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    properties.put("dataworker.username", "luke");
    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);
    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/test"));
  }

  @Test
  public void testSubmitJobNoArguments() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_1466418324742_0005";
    expect(api.runPigQuery((File) anyObject(), anyString(), (String) isNull())).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", null, jobService);

    Assert.assertEquals("", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    JSONObject obj = (JSONObject)response.getEntity();
    Assert.assertTrue(obj.containsKey("job"));
    Assert.assertNotNull(((PigJob) obj.get("job")).getId());
    Assert.assertFalse(((PigJob) obj.get("job")).getId().isEmpty());
    Assert.assertTrue(((PigJob) obj.get("job")).getStatusDir().startsWith("/tmp/.pigjobs/test"));

    PigJob job = ((PigJob) obj.get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());
  }

  @Test
  public void testSubmitJobNoFile() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", null, "-useHCatalog", jobService);
  }

  @Test
  public void testSubmitJobForcedContent() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);

    ByteArrayOutputStream baScriptStream = new ByteArrayOutputStream();
    ByteArrayOutputStream baTempletonArgsStream = new ByteArrayOutputStream();

    FSDataOutputStream scriptStream = new FSDataOutputStream(baScriptStream, null);
    FSDataOutputStream templetonArgsStream = new FSDataOutputStream(baTempletonArgsStream, null);
    expect(hdfsApi.create(endsWith("script.pig"), eq(true))).andReturn(scriptStream);
    expect(hdfsApi.create(endsWith("params"), eq(true))).andReturn(templetonArgsStream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_1466418324742_0005";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", null, "-useHCatalog", "pwd", null, jobService);  // with forcedContent
    Assert.assertEquals(201, response.getStatus());
    Assert.assertEquals("-useHCatalog", baTempletonArgsStream.toString());
    Assert.assertEquals("pwd", baScriptStream.toString());
  }

  @Test
  public void testSubmitJobNoTitle() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(BadRequestFormattedException.class);
    doCreateJob(null, "/tmp/1.pig", "-useHCatalog", jobService);
  }

  @Test
  public void testSubmitJobFailed() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));
    EasyMock.expectLastCall().andThrow(new HdfsApiException("Copy failed"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);
  }

  @Test
  public void testSubmitJobTempletonError() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    // Templeton returns 500 e.g.
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andThrow(new IOException());
    replay(api);

    thrown.expect(ServiceFormattedException.class);
    doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);
  }

  @Test
  public void testKillJobNoRemove() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createStrictMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_id_##";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);
    Assert.assertEquals(201, response.getStatus());

    reset(api);
    api.killJob(eq("job_id_##"));
    expect(api.checkJob(anyString())).andReturn(api.new JobInfo()).anyTimes();
    replay(api);
    JSONObject obj = (JSONObject)response.getEntity();
    PigJob job = ((PigJob)obj.get("job"));
    response = jobService.killJob(job.getId(), null);
    Assert.assertEquals(204, response.getStatus());

    response = jobService.getJob(job.getId());  // it should still be present in DB
    Assert.assertEquals(200, response.getStatus());
  }

  @Test
  public void testKillJobWithRemove() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createStrictMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_id_##";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);
    Assert.assertEquals(201, response.getStatus());

    reset(api);
    api.killJob(eq("job_id_##"));
    expect(api.checkJob(anyString())).andReturn(api.new JobInfo()).anyTimes();
    replay(api);
    JSONObject obj = (JSONObject)response.getEntity();
    PigJob job = ((PigJob)obj.get("job"));
    response = jobService.killJob(job.getId(), "true");
    Assert.assertEquals(204, response.getStatus());

    thrown.expect(NotFoundFormattedException.class); // it should not be present in DB
    jobService.getJob(job.getId());
  }

  @Test
  public void testJobStatusFlow() throws Exception {
    HdfsApi hdfsApi = createNiceMock(HdfsApi.class);
    hdfsApi.copy(eq("/tmp/script.pig"), startsWith("/tmp/.pigjobs/"));

    ByteArrayOutputStream do_stream = new ByteArrayOutputStream();

    FSDataOutputStream stream = new FSDataOutputStream(do_stream, null);
    expect(hdfsApi.create(anyString(), eq(true))).andReturn(stream);
    replay(hdfsApi);
    UserLocalObjects.setHdfsApi(hdfsApi, context);

    TempletonApi api = createNiceMock(TempletonApi.class);
    UserLocalObjects.setTempletonApi(api, context);
    TempletonApi.JobData data = api.new JobData();
    data.id = "job_id_#";
    expect(api.runPigQuery((File) anyObject(), anyString(), eq("-useHCatalog"))).andReturn(data);
    replay(api);

    Response response = doCreateJob("Test", "/tmp/script.pig", "-useHCatalog", jobService);

    Assert.assertEquals("-useHCatalog", do_stream.toString());
    Assert.assertEquals(201, response.getStatus());

    PigJob job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_SUBMITTED, job.getStatus());
    Assert.assertTrue(job.isInProgress());

    // Retrieve status:
    // SUBMITTED
    reset(api);
    TempletonApi.JobInfo info = api.new JobInfo();
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_SUBMITTED, job.getStatus());

    // RUNNING
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double) JobResourceManager.RUN_STATE_RUNNING);
    info.percentComplete = "30% complete";
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_RUNNING, job.getStatus());
    Assert.assertTrue(job.isInProgress());
    Assert.assertEquals(30, (Object) job.getPercentComplete());

    // SUCCEED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double) JobResourceManager.RUN_STATE_SUCCEEDED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_COMPLETED, job.getStatus());
    Assert.assertFalse(job.isInProgress());
    Assert.assertNull(job.getPercentComplete());

    // PREP
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double) JobResourceManager.RUN_STATE_PREP);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_RUNNING, job.getStatus());

    // FAILED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double) JobResourceManager.RUN_STATE_FAILED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_FAILED, job.getStatus());
    Assert.assertFalse(job.isInProgress());

    // KILLED
    reset(api);
    info = api.new JobInfo();
    info.status = new HashMap<String, Object>();
    info.status.put("runState", (double) JobResourceManager.RUN_STATE_KILLED);
    expect(api.checkJob(eq("job_id_#"))).andReturn(info);
    replay(api);
    response = jobService.getJob(job.getId());
    Assert.assertEquals(200, response.getStatus());
    job = ((PigJob) ((JSONObject)response.getEntity()).get("job"));
    Assert.assertEquals(PigJob.PIG_JOB_STATE_KILLED, job.getStatus());
    Assert.assertFalse(job.isInProgress());
  }
}
