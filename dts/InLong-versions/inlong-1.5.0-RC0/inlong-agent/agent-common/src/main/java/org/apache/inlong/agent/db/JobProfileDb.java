/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.db;

import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.JobConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.JobConstants.JOB_ID;

/**
 * Wrapper for job conf persistence.
 */
public class JobProfileDb {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobProfileDb.class);
    private final Db db;

    public JobProfileDb(Db db) {
        this.db = db;
    }

    /**
     * get all restart jobs from db
     *
     * @Deprecated Use {@link JobProfileDb#getJobsByState(Set)}
     */
    @Deprecated
    public List<JobProfile> getRestartJobs() {
        List<JobProfile> jobsByState = getJobsByState(StateSearchKey.ACCEPTED);
        jobsByState.addAll(getJobsByState(StateSearchKey.RUNNING));
        LOGGER.info("try to get restart jobs from db {}", jobsByState);
        return jobsByState;
    }

    /**
     * update job state and search it by key name
     *
     * @param jobInstanceId job key name
     * @param stateSearchKey job state
     */
    public void updateJobState(String jobInstanceId, StateSearchKey stateSearchKey) {
        KeyValueEntity entity = db.get(jobInstanceId);
        if (entity != null) {
            entity.setStateSearchKey(stateSearchKey);
            db.put(entity);
        }
    }

    /**
     * store job profile
     *
     * @param jobProfile job profile
     */
    public void storeJobFirstTime(JobProfile jobProfile) {
        if (jobProfile.allRequiredKeyExist()) {
            String keyName = jobProfile.get(JobConstants.JOB_INSTANCE_ID);
            jobProfile.setLong(JobConstants.JOB_STORE_TIME, System.currentTimeMillis());
            KeyValueEntity entity = new KeyValueEntity(keyName,
                    jobProfile.toJsonStr(), jobProfile.get(JobConstants.JOB_DIR_FILTER_PATTERNS, ""));
            entity.setStateSearchKey(StateSearchKey.ACCEPTED);
            LOGGER.info("store job {} to db", jobProfile.toJsonStr());
            db.put(entity);
        }
    }

    /**
     * update job profile
     *
     * @param jobProfile
     */
    public void updateJobProfile(JobProfile jobProfile) {
        String instanceId = jobProfile.getInstanceId();
        KeyValueEntity entity = db.get(instanceId);
        if (entity == null) {
            LOGGER.warn("job profile {} doesn't exist, update job profile fail {}", instanceId, jobProfile.toJsonStr());
            return;
        }
        entity.setJsonValue(jobProfile.toJsonStr());
        db.put(entity);
    }

    /**
     * check whether job is finished, note that non-exist job is regarded as finished.
     */
    public boolean checkJobfinished(JobProfile jobProfile) {
        KeyValueEntity entity = db.get(jobProfile.getInstanceId());
        if (entity == null) {
            LOGGER.info("job profile {} doesn't exist", jobProfile.getInstanceId());
            return true;
        }
        return entity.checkFinished();
    }

    /**
     * delete job by keyName
     */
    public void deleteJob(String keyName) {
        db.remove(keyName);
    }

    /**
     * get job profile by jobId
     *
     * @param jobId jobId
     * @return JobProfile
     */
    public JobProfile getJobById(String jobId) {
        KeyValueEntity keyValueEntity = db.get(jobId);
        if (keyValueEntity != null) {
            return keyValueEntity.getAsJobProfile();
        }
        return null;
    }

    /**
     * remove all expired jobs from db, including success and failed
     *
     * @param expireTime expireTime
     */
    public void removeExpireJobs(long expireTime) {
        // remove finished tasks
        List<KeyValueEntity> successEntityList = db.search(StateSearchKey.SUCCESS);
        List<KeyValueEntity> failedEntityList = db.search(StateSearchKey.FAILED);
        List<KeyValueEntity> entityList = new ArrayList<>(successEntityList);
        entityList.addAll(failedEntityList);
        for (KeyValueEntity entity : entityList) {
            if (entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
                JobProfile profile = entity.getAsJobProfile();
                long storeTime = profile.getLong(JobConstants.JOB_STORE_TIME, 0);
                long currentTime = System.currentTimeMillis();
                if (storeTime == 0 || currentTime - storeTime > expireTime) {
                    LOGGER.info("delete job {} because of timeout store time: {}, expire time: {}",
                            entity.getKey(), storeTime, expireTime);
                    deleteJob(entity.getKey());
                }
            }
        }
    }

    /**
     * get job conf by state
     *
     * @param stateSearchKey state index for searching.
     * @return JobProfile
     */
    public JobProfile getJob(StateSearchKey stateSearchKey) {
        KeyValueEntity entity = db.searchOne(stateSearchKey);
        if (entity != null && entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
            return entity.getAsJobProfile();
        }
        return null;
    }

    /**
     * get job reading specific file
     */
    public JobProfile getJobByFileName(String fileName) {
        KeyValueEntity entity = db.searchOne(fileName);
        if (entity != null && entity.getKey().startsWith(JobConstants.JOB_ID_PREFIX)) {
            return entity.getAsJobProfile();
        }
        return null;
    }

    /**
     * get list of job profiles.
     *
     * @param stateSearchKey state search key.
     * @return list of job profile.
     * @Deprecated Use {@link JobProfileDb#getJobsByState(Set)}
     */
    @Deprecated
    public List<JobProfile> getJobsByState(StateSearchKey stateSearchKey) {
        List<KeyValueEntity> entityList = db.searchWithKeyPrefix(stateSearchKey, JobConstants.JOB_ID_PREFIX);
        List<JobProfile> profileList = new ArrayList<>();
        for (KeyValueEntity entity : entityList) {
            profileList.add(entity.getAsJobProfile());
        }
        return profileList;
    }

    /**
     * get list of job profiles by some state.
     *
     * @param stateSearchKeys state search keys.
     * @return list of job profile.
     */
    public List<JobProfile> getJobsByState(Set<StateSearchKey> stateSearchKeys) {
        return stateSearchKeys.stream()
                .flatMap(stateSearchKey -> db.searchWithKeyPrefix(stateSearchKey, JobConstants.JOB_ID_PREFIX).stream())
                .map(KeyValueEntity::getAsJobProfile)
                .collect(Collectors.toList());
    }

    /**
     * get all jobs.
     *
     * @return list of job profile.
     */
    public List<JobProfile> getAllJobs() {
        return getJobsByState(Stream.of(StateSearchKey.values()).collect(Collectors.toSet()));
    }

    /**
     * check local job state from rocksDB.
     *
     * @return KV, key is job id and value is subtask config of job
     */
    public Map<String, List<String>> getJobsState() {
        List<KeyValueEntity> entityList = db.search(Arrays.asList(StateSearchKey.values()));
        Map<String, List<String>> jobStateMap = new HashMap<>();
        for (KeyValueEntity entity : entityList) {
            List<String> tmpList = new ArrayList<>();
            JobProfile jobProfile = entity.getAsJobProfile();
            String jobState = entity.getStateSearchKey().name().concat(":").concat(jobProfile.toJsonStr());
            tmpList.add(jobState);
            List<String> jobStates = jobStateMap.putIfAbsent(jobProfile.get(JOB_ID), tmpList);
            if (Objects.nonNull(jobStates) && !jobStates.contains(jobState)) {
                jobStates.addAll(tmpList);
                jobStateMap.put(jobProfile.get(JOB_ID), jobStates);
            }
        }
        return jobStateMap;
    }
}
