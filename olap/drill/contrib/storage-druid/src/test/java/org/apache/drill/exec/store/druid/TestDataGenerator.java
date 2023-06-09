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

package org.apache.drill.exec.store.druid;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.InputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class TestDataGenerator {
  private static final Logger logger = LoggerFactory.getLogger(TestDataGenerator.class);

  private static final OkHttpClient httpClient = new OkHttpClient();

  private static final ObjectMapper mapper = new ObjectMapper();

  private static final String RESPONSE_SUCCESS = "SUCCESS";

  public static void startImport(DruidStoragePluginConfig druidStoragePluginConfig) throws Exception {
    if (isDruidRunning(druidStoragePluginConfig)) {
      logger.debug("Starting Test Data Import");
      String taskId = startImportTask(druidStoragePluginConfig);
      waitForIndexingTaskToFinish(taskId, druidStoragePluginConfig);
      logger.debug("Finished Test Data Import");
    }
    else {
      logger.error("DRUID does not seem to be running...");
    }
  }

  private static boolean isDruidRunning(DruidStoragePluginConfig druidStoragePluginConfig) {
    try {
      String healthCheckUrl = druidStoragePluginConfig.getCoordinatorAddress() + "/status/health";
      Request get = new Request.Builder()
        .url(healthCheckUrl)
        .build();

      try (Response resp  = httpClient.newCall(get).execute()) {
        return resp.isSuccessful() && resp.body().string().equalsIgnoreCase("true");
      }
    } catch (Exception ex) {
      logger.error("Error getting druid status", ex);
      return false;
    }
  }

  private static String taskUrl(DruidStoragePluginConfig druidStoragePluginConfig) {
    return druidStoragePluginConfig.getCoordinatorAddress() + "/druid/indexer/v1/task";
  }

  private static String startImportTask(DruidStoragePluginConfig druidStoragePluginConfig) throws URISyntaxException, IOException {
    try {
      String url = taskUrl(druidStoragePluginConfig);
      RequestBody postBody = RequestBody.create(
        Files.readAllBytes(Paths.get(Resources.getResource("wikipedia-index.json").toURI()))
      );
      Request post = new Request.Builder()
        .url(url)
        .addHeader("Content-Type", "application/json")
        .post(postBody)
        .build();

      try (Response resp = httpClient.newCall(post).execute()) {
        String respBodyStr = resp.body().string();
        TaskStartResponse taskStartResponse = mapper.readValue(
          respBodyStr,
          TaskStartResponse.class
        );
        logger.debug("Started Indexing Task - {}", taskStartResponse.getTaskId());
        return taskStartResponse.getTaskId();
      }
    } catch (Exception ex) {
      logger.error("Error starting Indexing Task");
      throw ex;
    }
  }

  private static void waitForIndexingTaskToFinish(String taskId, DruidStoragePluginConfig druidStoragePluginConfig) throws Exception {
    int sleepMinutes = 1;
    logger.info("Waiting {} minute(s) for Indexing Task - {} to finish", sleepMinutes, taskId);
    Thread.sleep(TimeUnit.MINUTES.toMillis(sleepMinutes));

    String url = taskUrl(druidStoragePluginConfig) + "/" + taskId + "/status";
    Request get = new Request.Builder()
      .url(url)
      .addHeader("Content-Type", "application/json")
      .build();

    try (Response resp = httpClient.newCall(get).execute()) {
      InputStream jsonStream = resp.body().byteStream();
      TaskStatusResponse taskStatusResponse = mapper.readValue(jsonStream, TaskStatusResponse.class);
      if (!taskStatusResponse.taskStatus.status.equalsIgnoreCase(RESPONSE_SUCCESS)) {
        throw new Exception(String.format("Task %s finished with status %s", taskId, taskStatusResponse.taskStatus.status));
      }
    }

    logger.debug("Task {} finished successfully", taskId);
  }

  private static class TaskStartResponse {
    @JsonProperty("task")
    private final String taskId;

    @JsonCreator
    public TaskStartResponse(@JsonProperty("task") String taskId) {
      this.taskId = taskId;
    }

    public String getTaskId() {
      return taskId;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private static class TaskStatus {
    @JsonProperty
    String id;

    @JsonProperty
    String statusCode;

    @JsonProperty
    String status;

    @JsonProperty
    int duration;

    @JsonProperty
    String dataSource;

    @JsonCreator
    public TaskStatus(@JsonProperty("id") String id, @JsonProperty("statusCode") String statusCode, @JsonProperty("status") String status, @JsonProperty("duration") int duration, @JsonProperty("dataSource") String dataSource) {
      this.id = id;
      this.statusCode = statusCode;
      this.status = status;
      this.duration = duration;
      this.dataSource = dataSource;
    }
  }

  private static class TaskStatusResponse {
    @JsonProperty("task")
    String taskId;

    @JsonProperty("status")
    TaskStatus taskStatus;

    public TaskStatusResponse(@JsonProperty("task") String taskId, @JsonProperty("status") TaskStatus taskStatus) {
      this.taskId = taskId;
      this.taskStatus = taskStatus;
    }
  }
}
