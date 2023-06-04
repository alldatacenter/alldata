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

import static org.apache.inlong.agent.constant.CommonConstants.COMMA;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_DATA;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_SEND_PARTITION_KEY;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_READ_WAIT_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_LINE_END_PATTERN;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MAX_WAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_META_ENV_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MONITOR_DEFAULT_STATUS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_FILE_MONITOR_STATUS;
import static org.apache.inlong.agent.constant.KubernetesConstants.KUBERNETES;
import static org.apache.inlong.agent.constant.MetadataConstants.DATA_CONTENT;
import static org.apache.inlong.agent.constant.MetadataConstants.DATA_CONTENT_TIME;
import static org.apache.inlong.agent.constant.MetadataConstants.ENV_CVM;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_FILE_NAME;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_HOST_NAME;
import static org.apache.inlong.agent.constant.MetadataConstants.METADATA_SOURCE_IP;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.core.task.TaskPositionManager;
import org.apache.inlong.agent.except.FileException;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.Validator;
import org.apache.inlong.agent.plugin.sources.reader.AbstractReader;
import org.apache.inlong.agent.plugin.utils.FileDataUtils;
import org.apache.inlong.agent.plugin.validator.PatternValidator;
import org.apache.inlong.agent.utils.AgentUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * File reader entrance
 */
public class FileReaderOperator extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderOperator.class);

    public static final int NEVER_STOP_SIGN = -1;
    public static final int BATCH_READ_SIZE = 10000;
    public static final int CACHE_QUEUE_SIZE = 10 * BATCH_READ_SIZE;
    private static final int LINE_SEPARATOR_SIZE = System.lineSeparator().getBytes(StandardCharsets.UTF_8).length;
    private static final SimpleDateFormat RECORD_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Gson GSON = new Gson();

    public File file;
    public int position = 0;
    public int bytePosition = 0;
    private long readEndpoint = Long.MAX_VALUE;
    public String md5;
    public Map<String, String> metadata;
    public JobProfile jobConf;
    public boolean inited = false;
    public volatile boolean finished = false;
    public String instanceId;
    public String fileKey = null;
    private long timeout;
    private long waitTimeout;
    private long lastTime = 0;
    private List<Validator> validators = new ArrayList<>();
    public boolean firstStored = false;

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(CACHE_QUEUE_SIZE);
    private final StringBuffer sb = new StringBuffer();

    public FileReaderOperator(File file, int position) {
        this(file, position, "");
    }

    public FileReaderOperator(File file, int position, String md5) {
        LOGGER.info("FileReaderOperator fileName {}, init line is {}, md5 is {}", file.getName(), position, md5);
        this.file = file;
        this.position = position;
        this.md5 = md5;
        this.metadata = new HashMap<>();
    }

    public FileReaderOperator(File file) {
        this(file, 0);
    }

    @Override
    public Message read() {
        String data = null;
        try {
            data = queue.poll(DEFAULT_JOB_READ_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("poll {} data get interruptted.", file.getPath(), e);
        }

        return Optional.ofNullable(data)
                .map(this::metadataMessage)
                .filter(this::filterMessage)
                .map(message -> {
                    AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, inlongGroupId, inlongStreamId,
                            System.currentTimeMillis(), 1, message.length());
                    readerMetric.pluginReadSuccessCount.incrementAndGet();
                    readerMetric.pluginReadCount.incrementAndGet();
                    String proxyPartitionKey = jobConf.get(PROXY_SEND_PARTITION_KEY, DigestUtils.md5Hex(inlongGroupId));
                    Map<String, String> header = new HashMap<>();
                    header.put(PROXY_KEY_DATA, proxyPartitionKey);
                    return new DefaultMessage(message.getBytes(StandardCharsets.UTF_8), header);
                }).orElse(null);
    }

    @Override
    public boolean isFinished() {
        if (finished) {
            return true;
        }
        if (timeout == NEVER_STOP_SIGN) {
            return false;
        }
        if (hasDataRemaining()) {
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

    public String getJobInstanceId() {
        if (jobConf.hasKey(JobConstants.JOB_INSTANCE_ID)) {
            return jobConf.get(JobConstants.JOB_INSTANCE_ID);
        }
        return null;
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
            LOGGER.info("FileReaderOperator init: {}", jobConf.toJsonStr());
            this.jobConf = jobConf;
            super.init(jobConf);
            this.instanceId = jobConf.getInstanceId();
            initReadTimeout(jobConf);
            String md5 = AgentUtils.getFileMd5(file);
            if (StringUtils.isNotBlank(this.md5) && !this.md5.equals(md5)) {
                LOGGER.warn("md5 is differ from origin, origin: {}, new {}", this.md5, md5);
            }
            LOGGER.info("file name for task is {}, md5 is {}", file, md5);

            MonitorTextFile.getInstance().monitor(this);
            if (!jobConf.get(JOB_FILE_MONITOR_STATUS, JOB_FILE_MONITOR_DEFAULT_STATUS)
                    .equals(JOB_FILE_MONITOR_DEFAULT_STATUS)) {
                readEndpoint = Files.lines(file.toPath()).count();
            }

            this.bytePosition = getStartBytePosition(this.position);

            isFirstStore();

            if (jobConf.hasKey(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE) && DataCollectType.INCREMENT
                    .equalsIgnoreCase(jobConf.get(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE))
                    && this.firstStored) {
                LOGGER.info("FileReaderOperator DataCollectType INCREMENT: start bytePosition {},{}",
                        file.length(), file.getAbsolutePath());
                this.bytePosition = (int) file.length();

                storeRocksDB();
            }

            try {
                resiterMeta(jobConf);
            } catch (Exception ex) {
                LOGGER.error("init metadata error", ex);
            }
            inited = true;
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
        queue.clear();
        LOGGER.info("destroy reader with read {} num {}",
                metricName, readerMetric == null ? 0 : readerMetric.pluginReadCount.get());
    }

    public boolean filterMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return false;
        }
        if (validators.isEmpty()) {
            return true;
        }
        return validators.stream().allMatch(v -> v.validate(message));
    }

    public String metadataMessage(String message) {
        long timestamp = System.currentTimeMillis();
        boolean isJson = FileDataUtils.isJSON(message);
        Map<String, String> mergeData = new HashMap<>(metadata);
        mergeData.put(DATA_CONTENT, FileDataUtils.getK8sJsonLog(message, isJson));
        mergeData.put(DATA_CONTENT_TIME, RECORD_TIME_FORMAT.format(new Date(timestamp)));
        return GSON.toJson(mergeData);
    }

    public boolean hasDataRemaining() {
        return !queue.isEmpty();
    }

    public void resiterMeta(JobProfile jobConf) {
        if (!jobConf.hasKey(JOB_FILE_META_ENV_LIST)) {
            return;
        }
        String[] env = jobConf.get(JOB_FILE_META_ENV_LIST).split(COMMA);
        Arrays.stream(env).forEach(data -> {
            if (data.equalsIgnoreCase(KUBERNETES)) {
                new KubernetesMetadataProvider(this).getData();
            } else if (data.equalsIgnoreCase(ENV_CVM)) {
                metadata.put(METADATA_HOST_NAME, AgentUtils.getLocalHost());
                metadata.put(METADATA_SOURCE_IP, AgentUtils.fetchLocalIp());
                metadata.put(METADATA_FILE_NAME, file.getName());
            }
        });
    }

    public void fetchData() throws IOException {
        // todo: TaskPositionManager stored position should be changed to byte position.Now it store msg sent, so here
        // every line (include empty line) should be sent, otherwise the read position will be offset when
        // restarting and recovering. In the same time, Regex end line spiltted line also has this problem, because
        // recovering is based on line position.
        List<String> lines = bytePosition == 0 ? readFromLine(position) : readFromPos(bytePosition);
        if (!lines.isEmpty()) {
            LOGGER.info("path is {}, line is {}, byte position is {}, reads data lines {}",
                    file.getName(), position, bytePosition, lines.size());
        }
        List<String> resultLines = lines;
        // TODO line regular expression matching
        if (jobConf.hasKey(JOB_FILE_LINE_END_PATTERN)) {
            Pattern pattern = Pattern.compile(jobConf.get(JOB_FILE_LINE_END_PATTERN));
            resultLines = lines.stream().flatMap(line -> {
                sb.append(line + System.lineSeparator());
                String data = sb.toString();
                Matcher matcher = pattern.matcher(data);
                List<String> tmpResultLines = new ArrayList<>();
                int beginPos = 0;
                while (matcher.find()) {
                    String endLineStr = matcher.group();
                    int endPos = data.indexOf(endLineStr, beginPos);
                    tmpResultLines.add(data.substring(beginPos, endPos));
                    beginPos = endPos + endLineStr.length();
                }
                String lastWord = data.substring(beginPos);
                sb.setLength(0);
                sb.append(lastWord);
                return tmpResultLines.stream();
            }).collect(Collectors.toList());
        }

        resultLines.forEach(line -> {
            try {
                boolean offerSuc = queue.offer(line, 1, TimeUnit.SECONDS);
                while (offerSuc != true) {
                    offerSuc = queue.offer(line, 1, TimeUnit.SECONDS);
                }
            } catch (InterruptedException e) {
                LOGGER.error("fetchData offer failed {}", e.getMessage());
            }
        });
        bytePosition += lines.stream()
                .mapToInt(line -> line.getBytes(StandardCharsets.UTF_8).length + LINE_SEPARATOR_SIZE)
                .sum();
        position += lines.size();
        if (position >= readEndpoint) {
            finished = true;
        }
    }

    private List<String> readFromLine(int lineNum) throws IOException {
        String line = null;
        List<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                    StandardCharsets.UTF_8));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (++count > lineNum) {
                    LOGGER.debug("read from line line-num {},  data {}", lineNum, line);
                    lines.add(line);
                }
                if (lines.size() >= FileReaderOperator.BATCH_READ_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("readFromLine error  {}", e.getMessage());
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return lines;
    }

    private List<String> readFromPos(int pos) throws IOException {
        String line = null;
        List<String> lines = new ArrayList<>();
        RandomAccessFile input = null;
        try {
            input = new RandomAccessFile(file, "r");
            input.skipBytes(pos);
            while ((line = input.readLine()) != null) {
                String lineChart = new String(line.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                LOGGER.debug("read from pos pos-num {}, data {}", pos, lineChart);
                lines.add(lineChart);
                if (lines.size() >= FileReaderOperator.BATCH_READ_SIZE) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("readFromPos error  {}", e.getMessage());
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return lines;
    }

    private int getStartBytePosition(int lineNum) throws IOException {
        int startBytePosition = 0;
        BufferedReader reader = null;
        try {
            LOGGER.info("get start line {}", lineNum);
            String line = null;
            List<String> lines = new ArrayList<>();
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),
                    StandardCharsets.UTF_8));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (++count > lineNum) {
                    LOGGER.info("get startBytePosition end at line {}", count);
                    break;
                }
                startBytePosition = startBytePosition
                        + line.getBytes(StandardCharsets.UTF_8).length + LINE_SEPARATOR_SIZE;
            }
        } catch (Exception e) {
            LOGGER.error("getStartPositon err {}", e);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        LOGGER.info("getStartPositon bytePosition {}", startBytePosition);
        return startBytePosition;
    }

    private void isFirstStore() {
        if (!jobConf.hasKey(JobConstants.JOB_STORE_TIME)) {
            LOGGER.info("isFirstStore {},{}", file.getAbsolutePath(), this.firstStored);
            this.firstStored = true;
            return;
        }
        long jobStoreTime = Long.parseLong(jobConf.get(JobConstants.JOB_STORE_TIME));
        LOGGER.info("jobStoreTime {},{}", file.getAbsolutePath(), jobStoreTime);
        long storeTime = AgentConfiguration.getAgentConf().getLong(
                AgentConstants.AGENT_JOB_STORE_TIME, AgentConstants.DEFAULT_JOB_STORE_TIME);

        if (System.currentTimeMillis() - jobStoreTime > storeTime) {
            this.firstStored = false;
        } else {
            this.firstStored = true;
        }
        LOGGER.info("isFirstStore {},{}", file.getAbsolutePath(), this.firstStored);
    }

    private void storeRocksDB() {
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file.getPath()))) {
            lineNumberReader.skip(Long.MAX_VALUE);
            int seekPosition = lineNumberReader.getLineNumber();

            String jobInstanceId = getJobInstanceId();
            if (jobInstanceId != null) {
                TaskPositionManager.getInstance().updateSinkPosition(
                        jobInstanceId, getReadSource(), seekPosition, true);
                LOGGER.info("storeRocksDB {},{}", file.getAbsolutePath(), seekPosition);
            }
        } catch (IOException ex) {
            LOGGER.error("get position error, file absolute path: {}", file.getAbsolutePath());
        }
    }
}
