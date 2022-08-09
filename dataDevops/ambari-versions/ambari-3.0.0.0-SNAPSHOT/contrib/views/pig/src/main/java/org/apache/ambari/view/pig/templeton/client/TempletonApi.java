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

package org.apache.ambari.view.pig.templeton.client;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.ambari.view.ViewContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

//TODO: extract to separate JAR outside ambari-views scope
/**
 * Templeton Business Delegate
 */
public class TempletonApi {
  protected final static Logger LOG =
      LoggerFactory.getLogger(TempletonApi.class);

  protected WebResource service;
  private String doAs;
  private ViewContext context;

  /**
   * TempletonApi constructor
   * @param api webhcat url
   * @param doAs doAs argument
   * @param context context with URLStreamProvider
   */
  public TempletonApi(String api, String doAs, ViewContext context) {
    this.doAs = doAs;
    this.context = context;
    ClientConfig config = new DefaultClientConfig();
    config.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
    Client client = Client.create(config);
    this.service = client.resource(api);
  }

  /**
   * @see #TempletonApi(String,String,ViewContext)
   */
  public TempletonApi(String api, ViewContext context) {
    this(api, null, context);
  }

  /**
   * Create and queue a Pig job.
   * @param execute String containing an entire, short pig program to run. (e.g. pwd)
   * @param pigFile HDFS file name of a pig program to run. (One of either "execute" or "file" is required )
   * @param statusDir A directory where Templeton will write the status of the Pig job. If
   *                  provided, it is the caller's responsibility to remove this directory when done.
   * @param arg Set a program argument. Optional None
   * @return id A string containing the job ID similar to "job_201110132141_0001".
   *         info A JSON object containing the information returned when the job was queued.
   */
  public JobData runPigQuery(String execute, File pigFile, String statusDir, String arg) throws IOException {
    MultivaluedMapImpl data = new MultivaluedMapImpl();
    if (execute != null)
      data.add("execute", execute);
    if (pigFile != null)
      data.add("file", pigFile.toString());
    if (statusDir != null)
      data.add("statusdir", statusDir);
    if (arg != null && !arg.isEmpty()) {
      for(String arg1 : arg.split("\t")) {
        data.add("arg", arg1);
      }
    }

    JSONRequest<JobData> request =
        new JSONRequest<JobData>(service.path("pig"), JobData.class, doAs, doAs, context); //FIXME: configurable proxyuser

    return request.post(data);
  }

  /**
   * @see #runPigQuery(String, java.io.File, String, String)
   */
  public JobData runPigQuery(File pigFile, String statusDir, String arg) throws IOException {
    return runPigQuery(null, pigFile, statusDir, arg);
  }

  /**
   * @see #runPigQuery(String, java.io.File, String, String)
   */
  public JobData runPigQuery(String execute, String statusDir, String arg) throws IOException {
    return runPigQuery(execute, null, statusDir, arg);
  }

  /**
   * @see #runPigQuery(String, java.io.File, String, String)
   */
  public JobData runPigQuery(String execute) throws IOException {
    return runPigQuery(execute, null, null, null);
  }

  /**
   * Get Job information
   * @param jobId templeton job identifier
   * @return JobInfo object
   * @throws IOException
   */
  public JobInfo checkJob(String jobId) throws IOException {
    JSONRequest<JobInfo> request =
        new JSONRequest<JobInfo>(service.path("jobs").path(jobId), JobInfo.class, doAs, doAs, context);

    return request.get();
  }

  /**
   * Kill templeton job
   * @param jobId templeton job identifier
   * @throws IOException
   */
  public void killJob(String jobId) throws IOException {
    JSONRequest<JobInfo> request =
        new JSONRequest<JobInfo>(service.path("jobs").path(jobId), JobInfo.class, doAs, doAs, context);

    try {
      request.delete();
    } catch (IOException e) {
      LOG.error("Ignoring 500 response from webhcat (see HIVE-5835)");
    }
  }

  /**
   * Get templeton status (version)
   * @return templeton status
   * @throws IOException
   */
  public Status status() throws IOException {
    JSONRequest<Status> request =
        new JSONRequest<Status>(service.path("status"), Status.class,
            doAs, doAs, context);
    return request.get();
  }

  /**
   * Wrapper for json mapping of status request
   */
  public class Status {
    public String status;
    public String version;
  }

  /**
   * Wrapper for json mapping of runPigQuery request
   * @see #runPigQuery(String, java.io.File, String, String)
   */
  public class JobData {
    public String id;
  }

  /**
   * Wrapper for json mapping of job status
   */
  public class JobInfo {
    public Map<String, Object> status;
    public Map<String, Object> profile;
    public Map<String, Object> userargs;

    public String id;
    public String parentId;
    public String percentComplete;
    public Integer exitValue;
    public String user;
    public String callback;
    public String completed;
  }
}
