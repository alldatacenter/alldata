/*
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
*/

package org.apache.griffin.core.job;

import static org.apache.griffin.core.config.PropertiesConfig.livyConfMap;
import static org.apache.griffin.core.job.entity.LivySessionStates.State.NOT_FOUND;
import static org.apache.griffin.core.util.JsonUtil.toEntity;
import static org.apache.griffin.core.util.JsonUtil.toJsonWithFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;

import org.apache.commons.collections.map.HashedMap;
import org.quartz.JobDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.kerberos.client.KerberosRestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
public class LivyTaskSubmitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LivyTaskSubmitHelper.class);
    private static final String REQUEST_BY_HEADER = "X-Requested-By";
    public static final int DEFAULT_QUEUE_SIZE = 20000;
    private static final int SLEEP_TIME = 300;

    @Autowired
    private SparkSubmitJob sparkSubmitJob;
    private ConcurrentMap<Long, Integer> taskAppIdMap = new ConcurrentHashMap<>();
    // Current number of tasks
    private AtomicInteger curConcurrentTaskNum = new AtomicInteger(0);
    private String workerNamePre;
    private RestTemplate restTemplate = new RestTemplate();
    // queue for pub or sub
    private BlockingQueue<JobDetail> queue;
    private String uri;

    @Value("${livy.task.max.concurrent.count:20}")
    private int maxConcurrentTaskCount;
    @Value("${livy.task.submit.interval.second:3}")
    private int batchIntervalSecond;

    @Autowired
    private Environment env;

    /**
     * Initialize related parameters and open consumer threads.
     */
    @PostConstruct
    public void init() {
        startWorker();
        uri = env.getProperty("livy.uri");
        LOGGER.info("Livy uri : {}", uri);
    }

    public LivyTaskSubmitHelper() {
        this.workerNamePre = "livy-task-submit-worker";
    }

    /**
     * Initialize blocking queues and start consumer threads.
     */
    public void startWorker() {
        queue = new LinkedBlockingQueue<>(DEFAULT_QUEUE_SIZE);
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        TaskInner taskInner = new TaskInner(executorService);
        executorService.execute(taskInner);
    }

    /**
     * Put job detail into the queue.
     *
     * @param jd job detail.
     */
    public void addTaskToWaitingQueue(JobDetail jd) throws IOException {
        if (jd == null) {
            LOGGER.warn("task is blank, workerNamePre: {}", workerNamePre);
            return;
        }

        if (queue.remainingCapacity() <= 0) {
            LOGGER.warn("task is discard, workerNamePre: {}, task: {}", workerNamePre, jd);
            sparkSubmitJob.saveJobInstance(null, NOT_FOUND);
            return;
        }

        queue.add(jd);
        LOGGER.info("add_task_to_waiting_queue_success, workerNamePre: {}, task: {}",
            workerNamePre, jd);
    }

    /**
     * Consumer thread.
     */
    class TaskInner implements Runnable {
        private ExecutorService es;

        public TaskInner(ExecutorService es) {
            this.es = es;
        }

        public void run() {
            long insertTime = System.currentTimeMillis();
            while (true) {
                try {
                    if (curConcurrentTaskNum.get() < maxConcurrentTaskCount
                        && (System.currentTimeMillis() - insertTime) >= batchIntervalSecond * 1000) {
                        JobDetail jd = queue.take();
                        sparkSubmitJob.saveJobInstance(jd);
                        insertTime = System.currentTimeMillis();
                    } else {
                        Thread.sleep(SLEEP_TIME);
                    }
                } catch (Exception e) {
                    LOGGER.error("Async_worker_doTask_failed, {}", e.getMessage(), e);
                    es.execute(this);
                }
            }
        }
    }

    /**
     * Add the batch id returned by Livy.
     *
     * @param scheduleId livy batch id.
     */
    public void increaseCurTaskNum(Long scheduleId) {
        curConcurrentTaskNum.incrementAndGet();
        if (scheduleId != null) {
            taskAppIdMap.put(scheduleId, 1);
        }
    }

    /**
     * Remove tasks after job status updates.
     *
     * @param scheduleId livy batch id.
     */
    public void decreaseCurTaskNum(Long scheduleId) {
        if (scheduleId != null && taskAppIdMap.containsKey(scheduleId)) {
            curConcurrentTaskNum.decrementAndGet();
            taskAppIdMap.remove(scheduleId);
        }
    }

    protected Map<String, Object> retryLivyGetAppId(String result, int appIdRetryCount)
        throws IOException {

        int retryCount = appIdRetryCount;
        TypeReference<HashMap<String, Object>> type =
            new TypeReference<HashMap<String, Object>>() {
            };
        Map<String, Object> resultMap = toEntity(result, type);

        if (retryCount <= 0) {
            return null;
        }

        if (resultMap.get("appId") != null) {
            return resultMap;
        }

        Object livyBatchesId = resultMap.get("id");
        if (livyBatchesId == null) {
            return resultMap;
        }

        while (retryCount-- > 0) {
            try {
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
            resultMap = getResultByLivyId(livyBatchesId, type);
            LOGGER.info("retry get livy resultMap: {}, batches id : {}", resultMap, livyBatchesId);

            if (resultMap.get("appId") != null) {
                break;
            }
        }

        return resultMap;
    }

    private Map<String, Object> getResultByLivyId(Object livyBatchesId, TypeReference<HashMap<String, Object>> type)
        throws IOException {
        Map<String, Object> resultMap = new HashedMap();
        String livyUri = uri + "/" + livyBatchesId;
        String result = getFromLivy(livyUri);
        LOGGER.info(result);
        return result == null ? resultMap : toEntity(result, type);
    }

    public String postToLivy(String uri) {
        LOGGER.info("Post To Livy URI is: " + uri);
        String needKerberos = env.getProperty("livy.need.kerberos");
        LOGGER.info("Need Kerberos:" + needKerberos);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(REQUEST_BY_HEADER, "admin");

        if (needKerberos == null || needKerberos.isEmpty()) {
            LOGGER.error("The property \"livy.need.kerberos\" is empty");
            return null;
        }

        if (needKerberos.equalsIgnoreCase("false")) {
            LOGGER.info("The livy server doesn't need Kerberos Authentication");
            String result = null;
            try {
                HttpEntity<String> springEntity = new HttpEntity<>(toJsonWithFormat(livyConfMap), headers);
                result = restTemplate.postForObject(uri, springEntity, String.class);
                LOGGER.info(result);
            } catch (HttpClientErrorException e) {
                LOGGER.error("Post to livy ERROR. \n  response status : " + e.getMessage()
                    + "\n  response header : " + e.getResponseHeaders()
                    + "\n  response body : " + e.getResponseBodyAsString());
            } catch (JsonProcessingException e) {
                LOGGER.error("Json Parsing failed, {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Post to livy ERROR. \n {}", e);
            }
            return result;
        } else {
            LOGGER.info("The livy server needs Kerberos Authentication");
            String userPrincipal = env.getProperty("livy.server.auth.kerberos.principal");
            String keyTabLocation = env.getProperty("livy.server.auth.kerberos.keytab");
            LOGGER.info("principal:{}, lcoation:{}", userPrincipal, keyTabLocation);

            KerberosRestTemplate restTemplate = new KerberosRestTemplate(keyTabLocation, userPrincipal);
            HttpEntity<String> springEntity = null;
            try {
                springEntity = new HttpEntity<>(toJsonWithFormat(livyConfMap), headers);
            } catch (HttpClientErrorException e) {
                LOGGER.error("Post to livy ERROR. \n  response status : " + e.getMessage()
                    + "\n  response header : " + e.getResponseHeaders()
                    + "\n  response body : " + e.getResponseBodyAsString());
            } catch (JsonProcessingException e) {
                LOGGER.error("Json Parsing failed, {}", e.getMessage(), e);
            } catch (Exception e) {
                LOGGER.error("Post to livy ERROR. {}", e.getMessage(), e);
            }
            String result = restTemplate.postForObject(uri, springEntity, String.class);
            LOGGER.info(result);
            return result;
        }
    }

    public String getFromLivy(String uri) {
        LOGGER.info("Get From Livy URI is: " + uri);
        String needKerberos = env.getProperty("livy.need.kerberos");
        LOGGER.info("Need Kerberos:" + needKerberos);

        if (needKerberos == null || needKerberos.isEmpty()) {
            LOGGER.error("The property \"livy.need.kerberos\" is empty");
            return null;
        }

        if (needKerberos.equalsIgnoreCase("false")) {
            LOGGER.info("The livy server doesn't need Kerberos Authentication");
            return restTemplate.getForObject(uri, String.class);
        } else {
            LOGGER.info("The livy server needs Kerberos Authentication");
            String userPrincipal = env.getProperty("livy.server.auth.kerberos.principal");
            String keyTabLocation = env.getProperty("livy.server.auth.kerberos.keytab");
            LOGGER.info("principal:{}, lcoation:{}", userPrincipal, keyTabLocation);

            KerberosRestTemplate restTemplate = new KerberosRestTemplate(keyTabLocation, userPrincipal);
            String result = restTemplate.getForObject(uri, String.class);
            LOGGER.info(result);
            return result;
        }
    }

    public void deleteByLivy(String uri) {
        LOGGER.info("Delete by Livy URI is: " + uri);
        String needKerberos = env.getProperty("livy.need.kerberos");
        LOGGER.info("Need Kerberos:" + needKerberos);

        if (needKerberos == null || needKerberos.isEmpty()) {
            LOGGER.error("The property \"livy.need.kerberos\" is empty");
            return;
        }

        if (needKerberos.equalsIgnoreCase("false")) {
            LOGGER.info("The livy server doesn't need Kerberos Authentication");
            new RestTemplate().delete(uri);
        } else {
            LOGGER.info("The livy server needs Kerberos Authentication");
            String userPrincipal = env.getProperty("livy.server.auth.kerberos.principal");
            String keyTabLocation = env.getProperty("livy.server.auth.kerberos.keytab");
            LOGGER.info("principal:{}, lcoation:{}", userPrincipal, keyTabLocation);

            KerberosRestTemplate restTemplate = new KerberosRestTemplate(keyTabLocation, userPrincipal);
            restTemplate.delete(uri);
        }
    }
}
