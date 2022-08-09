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

package org.apache.ambari.view.pig.resources.jobs;

import org.apache.ambari.view.ViewContext;
import org.apache.ambari.view.pig.persistence.utils.FilteringStrategy;
import org.apache.ambari.view.pig.persistence.utils.Indexed;
import org.apache.ambari.view.pig.resources.PersonalCRUDResourceManager;
import org.apache.ambari.view.pig.resources.jobs.models.PigJob;
import org.apache.ambari.view.pig.resources.jobs.utils.JobPolling;
import org.apache.ambari.view.pig.templeton.client.TempletonApi;
import org.apache.ambari.view.pig.templeton.client.TempletonApiFactory;
import org.apache.ambari.view.pig.utils.MisconfigurationFormattedException;
import org.apache.ambari.view.pig.utils.ServiceFormattedException;
import org.apache.ambari.view.pig.utils.UserLocalObjects;
import org.apache.ambari.view.utils.ambari.AmbariApiException;
import org.apache.ambari.view.utils.hdfs.HdfsApiException;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Object that provides operations for templeton jobs
 * CRUD overridden to support
 */
public class JobResourceManager extends PersonalCRUDResourceManager<PigJob> {
  protected TempletonApi api;

  private final static Logger LOG =
      LoggerFactory.getLogger(JobResourceManager.class);

  /**
   * Constructor
   * @param context View Context instance
   */
  public JobResourceManager(ViewContext context) {
    super(PigJob.class, context);

    setupPolling();
  }

  private void setupPolling() {
    List<PigJob> notCompleted = this.readAll(new FilteringStrategy() {
      @Override
      public boolean isConform(Indexed item) {
        PigJob job = (PigJob) item;
        return job.isInProgress();
      }
    });

    for(PigJob job : notCompleted) {
      JobPolling.pollJob(this, job);
    }
  }

  @Override
  public PigJob create(PigJob object) {
    object.setStatus(PigJob.PIG_JOB_STATE_SUBMITTING);
    PigJob job = super.create(object);
    LOG.debug("Submitting job...");

    try {
      submitJob(object);
    } catch (RuntimeException e) {
      object.setStatus(PigJob.PIG_JOB_STATE_SUBMIT_FAILED);
      save(object);
      LOG.debug("Job submit FAILED");
      throw e;
    }
    LOG.debug("Job submit OK");
    object.setStatus(PigJob.PIG_JOB_STATE_SUBMITTED);
    save(object);
    return job;
  }

  /**
   * Kill Templeton Job
   * @param object job object
   * @throws IOException network error
   */
  public void killJob(PigJob object) throws IOException {
    LOG.debug("Killing job...");

    if (object.getJobId() != null) {
      try {
        UserLocalObjects.getTempletonApi(context).killJob(object.getJobId());
      } catch (IOException e) {
        LOG.debug("Job kill FAILED");
        throw e;
      }
      LOG.debug("Job kill OK");
    } else {
      LOG.debug("Job was not submitted, ignoring kill request...");
    }
  }

  /**
   * Running job
   * @param job job bean
   */
  private void submitJob(PigJob job) {
    String date = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss").format(new Date());
    String jobsBaseDir = context.getProperties().get("jobs.dir");
    String storeBaseDir = context.getProperties().get("store.dir");
    if (storeBaseDir == null || storeBaseDir.compareTo("null") == 0 || storeBaseDir.compareTo("") == 0)
      storeBaseDir = context.getProperties().get("jobs.dir");
    String jobNameCleaned = job.getTitle().toLowerCase().replaceAll("[^a-zA-Z0-9 ]+", "").replace(" ", "_");
    String storedir = String.format(storeBaseDir + "/%s_%s", jobNameCleaned, date);
    String statusdir = String.format(jobsBaseDir + "/%s_%s", jobNameCleaned, date);

    String newPigScriptPath = storedir + "/script.pig";
    String newSourceFilePath = storedir + "/source.pig";
    String newPythonScriptPath = storedir + "/udf.py";
    String templetonParamsFilePath = storedir + "/params";

    try {
      // additional file can be passed to copy into work directory
      if (job.getSourceFileContent() != null && !job.getSourceFileContent().isEmpty()) {
        String sourceFileContent = job.getSourceFileContent();
        job.setSourceFileContent(null); // we should not store content in DB
        save(job);

        FSDataOutputStream stream = UserLocalObjects.getHdfsApi(context).create(newSourceFilePath, true);
        stream.writeBytes(sourceFileContent);
        stream.close();
      } else {
        if (job.getSourceFile() != null && !job.getSourceFile().isEmpty()) {
          // otherwise, just copy original file
          UserLocalObjects.getHdfsApi(context).copy(job.getSourceFile(), newSourceFilePath);
        }
      }
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create/copy source file: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create/copy source file: " + e.toString(), e);
    } catch (HdfsApiException e) {
      throw new ServiceFormattedException("Can't copy source file from " + job.getSourceFile() +
          " to " + newPigScriptPath, e);
    }

    try {
      // content can be passed from front-end with substituted arguments
      if (job.getForcedContent() != null && !job.getForcedContent().isEmpty()) {
        String forcedContent = job.getForcedContent();
        String defaultUrl = context.getProperties().get("webhdfs.url");
        URI uri = new URI(defaultUrl);

        // variable for sourceFile can be passed from front-end
        // cannot use webhdfs url as webhcat does not support http authentication
        // using url hdfs://host/sourcefilepath
        if(uri.getScheme().equals("webhdfs")){
          defaultUrl = "hdfs://" + uri.getHost();
        }

        forcedContent = forcedContent.replace("${sourceFile}",
            defaultUrl + newSourceFilePath);
        job.setForcedContent(null); // we should not store content in DB
        save(job);

        FSDataOutputStream stream = UserLocalObjects.getHdfsApi(context).create(newPigScriptPath, true);
        stream.writeBytes(forcedContent);
        stream.close();
      } else {
        // otherwise, just copy original file
        UserLocalObjects.getHdfsApi(context).copy(job.getPigScript(), newPigScriptPath);
      }
    } catch (HdfsApiException e) {
      throw new ServiceFormattedException("Can't copy pig script file from " + job.getPigScript() +
          " to " + newPigScriptPath, e);
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create/copy pig script file: " + e.getMessage(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create/copy pig script file: " + e.getMessage(), e);
    } catch (URISyntaxException e) {
      throw new ServiceFormattedException("Can't create/copy pig script file: " + e.getMessage(), e);
    }

    if (job.getPythonScript() != null && !job.getPythonScript().isEmpty()) {
      try {
        UserLocalObjects.getHdfsApi(context).copy(job.getPythonScript(), newPythonScriptPath);
      } catch (HdfsApiException e) {
        throw new ServiceFormattedException("Can't copy python udf script file from " + job.getPythonScript() +
            " to " + newPythonScriptPath);
      } catch (IOException e) {
        throw new ServiceFormattedException("Can't create/copy python udf file: " + e.toString(), e);
      } catch (InterruptedException e) {
        throw new ServiceFormattedException("Can't create/copy python udf file: " + e.toString(), e);
      }
    }

    try {
      FSDataOutputStream stream = UserLocalObjects.getHdfsApi(context).create(templetonParamsFilePath, true);
      if (job.getTempletonArguments() != null) {
        stream.writeBytes(job.getTempletonArguments());
      }
      stream.close();
    } catch (IOException e) {
      throw new ServiceFormattedException("Can't create params file: " + e.toString(), e);
    } catch (InterruptedException e) {
      throw new ServiceFormattedException("Can't create params file: " + e.toString(), e);
    }
    job.setPigScript(newPigScriptPath);

    job.setStatusDir(statusdir);
    job.setDateStarted(System.currentTimeMillis() / 1000L);

    TempletonApi.JobData data;
    try {
      data = UserLocalObjects.getTempletonApi(context).runPigQuery(new File(job.getPigScript()), statusdir, job.getTempletonArguments());
      if (data.id != null) {
        job.setJobId(data.id);
        JobPolling.pollJob(this, job);
      } else {
        throw new AmbariApiException("Cannot get id for the Job.");
      }
    } catch (IOException templetonBadResponse) {
      String msg = String.format("Templeton bad response: %s", templetonBadResponse.toString());
      LOG.debug(msg);
      throw new ServiceFormattedException(msg, templetonBadResponse);
    }
  }

  /**
   * Get job status
   * @param job job object
   */
  public void retrieveJobStatus(PigJob job) {
    TempletonApi.JobInfo info;
    try {
      info = UserLocalObjects.getTempletonApi(context).checkJob(job.getJobId());
    } catch (IOException e) {
      LOG.warn(String.format("IO Exception: %s", e));
      return;
    }

    if (info.status != null && (info.status.containsKey("runState"))) {
      //TODO: retrieve from RM
      Long time = System.currentTimeMillis() / 1000L;
      Long currentDuration = time - job.getDateStarted();
      int runState = ((Double) info.status.get("runState")).intValue();
      boolean isStatusChanged = false;
      switch (runState) {
        case RUN_STATE_KILLED:
          LOG.debug(String.format("Job KILLED: %s", job.getJobId()));
          isStatusChanged = !job.getStatus().equals(PigJob.PIG_JOB_STATE_KILLED);
          job.setStatus(PigJob.PIG_JOB_STATE_KILLED);
          break;
        case RUN_STATE_FAILED:
          LOG.debug(String.format("Job FAILED: %s", job.getJobId()));
          isStatusChanged = !job.getStatus().equals(PigJob.PIG_JOB_STATE_FAILED);
          job.setStatus(PigJob.PIG_JOB_STATE_FAILED);
          break;
        case RUN_STATE_PREP:
        case RUN_STATE_RUNNING:
          isStatusChanged = !job.getStatus().equals(PigJob.PIG_JOB_STATE_RUNNING);
          job.setStatus(PigJob.PIG_JOB_STATE_RUNNING);
          break;
        case RUN_STATE_SUCCEEDED:
          LOG.debug(String.format("Job COMPLETED: %s", job.getJobId()));
          isStatusChanged = !job.getStatus().equals(PigJob.PIG_JOB_STATE_COMPLETED);
          job.setStatus(PigJob.PIG_JOB_STATE_COMPLETED);
          break;
        default:
          LOG.debug(String.format("Job in unknown state: %s", job.getJobId()));
          isStatusChanged = !job.getStatus().equals(PigJob.PIG_JOB_STATE_UNKNOWN);
          job.setStatus(PigJob.PIG_JOB_STATE_UNKNOWN);
          break;
      }
      if (isStatusChanged) {
        job.setDuration(currentDuration);
      }
    }
    Pattern pattern = Pattern.compile("\\d+");
    Matcher matcher = null;
    if (info.percentComplete != null) {
      matcher = pattern.matcher(info.percentComplete);
    }
    if (matcher != null && matcher.find()) {
      job.setPercentComplete(Integer.valueOf(matcher.group()));
    } else {
      job.setPercentComplete(null);
    }
    save(job);
  }

  /**
   * Checks connection to WebHCat
   * @param context View Context
   */
  public static void webhcatSmokeTest(ViewContext context) {
    try {
      TempletonApiFactory templetonApiFactory = new TempletonApiFactory(context);
      TempletonApi api = templetonApiFactory.connectToTempletonApi();
      api.status();
    } catch (WebApplicationException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new ServiceFormattedException(ex.getMessage(), ex);
    }
  }

  public static final int RUN_STATE_RUNNING = 1;
  public static final int RUN_STATE_SUCCEEDED = 2;
  public static final int RUN_STATE_FAILED = 3;
  public static final int RUN_STATE_PREP = 4;
  public static final int RUN_STATE_KILLED = 5;
}
