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

package org.apache.inlong.agent.plugin.sources.reader.file;

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Validator;
import org.apache.inlong.agent.plugin.except.FileException;
import org.apache.inlong.agent.plugin.sources.reader.AbstractReader;
import org.apache.inlong.agent.plugin.validator.PatternValidator;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.apache.inlong.agent.constant.CommonConstants.COMMA;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MAX_WAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_ENV_LIST;
import static org.apache.inlong.agent.constant.MetadataConstants.KUBERNETES;

/**
 * File reader entrance
 */
public class FileReaderOperator extends AbstractReader {

    public static final int NEVER_STOP_SIGN = -1;
    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderOperator.class);
    private static final String TEXT_FILE_READER_TAG_NAME = "AgentTextMetric";
    public File file;
    public int position;
    public String md5;
    public Stream<String> stream;
    public Map<String, String> metadata;
    public JobProfile jobConf;
    public Iterator<String> iterator;
    public volatile boolean finished = false;
    private long timeout;
    private long waitTimeout;
    private long lastTime = 0;

    private List<Validator> validators = new ArrayList<>();

    public FileReaderOperator(File file, int position) {
        this(file, position, "");
    }

    public FileReaderOperator(File file, int position, String md5) {
        this.file = file;
        this.position = position;
        this.md5 = md5;
    }

    public FileReaderOperator(File file) {
        this(file, 0);
    }

    @Override
    public Message read() {
        if (iterator != null && iterator.hasNext()) {
            String message = iterator.next();
            if (validateMessage(message)) {
                AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS,
                        inlongGroupId, inlongStreamId, System.currentTimeMillis());
                readerMetric.pluginReadCount.incrementAndGet();
                return new DefaultMessage(message.getBytes(StandardCharsets.UTF_8));
            }
        }
        AgentUtils.silenceSleepInMs(waitTimeout);
        return null;
    }

    private boolean validateMessage(String message) {
        if (validators.isEmpty()) {
            return true;
        }
        return validators.stream().allMatch(v -> v.validate(message));
    }

    @Override
    public boolean isFinished() {
        if (finished) {
            return true;
        }
        if (timeout == NEVER_STOP_SIGN) {
            return false;
        }
        if (iterator == null) {
            return true;
        }
        if (iterator.hasNext()) {
            lastTime = 0;
            return false;
        } else {
            if (lastTime == 0) {
                lastTime = System.currentTimeMillis();
            }
            return System.currentTimeMillis() - lastTime > timeout;
        }
    }

    @Override
    public String getReadSource() {
        return file.getAbsolutePath();
    }

    @Override
    public void setReadTimeout(long millis) {
        timeout = millis;
    }

    @Override
    public void setWaitMillisecond(long millis) {
        waitTimeout = millis;
    }

    @Override
    public String getSnapshot() {
        return StringUtils.EMPTY;
    }

    @Override
    public void finishRead() {
        destroy();
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    public void addPatternValidator(String pattern) {
        if (pattern.isEmpty()) {
            return;
        }
        validators.add(new PatternValidator(pattern));
    }

    @Override
    public void init(JobProfile jobConf) {
        try {
            this.jobConf = jobConf;
            super.init(jobConf);
            initReadTimeout(jobConf);
            String md5 = AgentUtils.getFileMd5(file);
            if (StringUtils.isNotBlank(this.md5) && !this.md5.equals(md5)) {
                LOGGER.warn("md5 is differ from origin, origin: {}, new {}", this.md5, md5);
            }
            LOGGER.info("file name for task is {}, md5 is {}", file, md5);
            List<AbstractFileReader> fileReaders = getInstance(this, jobConf);
            fileReaders.forEach(fileReader -> {
                try {
                    fileReader.getData();
                    fileReader.mergeData(this);
                } catch (Exception ex) {
                    LOGGER.error("read file data error", ex);
                }
            });
            if (Objects.nonNull(stream)) {
                iterator = stream.iterator();
            }
        } catch (Exception ex) {
            throw new FileException("error init stream for " + file.getPath(), ex);
        }
    }

    // default value is -1 and never stop task
    private void initReadTimeout(JobProfile jobConf) {
        int waitTime = jobConf.getInt(JOB_FILE_MAX_WAIT,
                NEVER_STOP_SIGN);
        if (waitTime == NEVER_STOP_SIGN) {
            timeout = NEVER_STOP_SIGN;
        } else {
            timeout = TimeUnit.MINUTES.toMillis(waitTime);
        }
    }

    @Override
    public void destroy() {
        finished = true;
        if (stream == null) {
            return;
        }
        AgentUtils.finallyClose(stream);
        LOGGER.info("destroy reader with read {} num {}",
                metricName, readerMetric.pluginReadCount.get());
    }

    public List<AbstractFileReader> getInstance(FileReaderOperator reader, JobProfile jobConf) {
        List<AbstractFileReader> fileReaders = new ArrayList<>();
        fileReaders.add(new TextFileReader(reader));
        if (!jobConf.hasKey(JOB_FILE_META_ENV_LIST)) {
            return fileReaders;
        }
        String[] env = jobConf.get(JOB_FILE_META_ENV_LIST).split(COMMA);
        Arrays.stream(env).forEach(data -> {
            if (data.equalsIgnoreCase(KUBERNETES)) {
                fileReaders.add(new KubernetesFileReader(reader));
            }
        });
        return fileReaders;
    }
}
