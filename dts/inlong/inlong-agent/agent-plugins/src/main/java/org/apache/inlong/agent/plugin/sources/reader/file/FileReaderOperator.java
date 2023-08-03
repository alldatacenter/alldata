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

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.DataCollectType;
import org.apache.inlong.agent.constant.JobConstants;
import org.apache.inlong.agent.core.task.MemoryManager;
import org.apache.inlong.agent.core.task.PositionManager;
import org.apache.inlong.agent.except.FileException;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.reader.AbstractReader;
import org.apache.inlong.agent.plugin.utils.FileDataUtils;
import org.apache.inlong.agent.utils.AgentUtils;

import com.google.gson.Gson;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.apache.inlong.agent.constant.CommonConstants.COMMA;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_DATA;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_PACKAGE_MAX_SIZE;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_SEND_PARTITION_KEY;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_CHANNEL_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_READER_QUEUE_PERMIT;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_GLOBAL_READER_SOURCE_PERMIT;
import static org.apache.inlong.agent.constant.JobConstants.DEFAULT_JOB_READ_WAIT_TIMEOUT;
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

/**
 * File reader entrance
 */
public class FileReaderOperator extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileReaderOperator.class);

    public static final int NEVER_STOP_SIGN = -1;
    public static final int BATCH_READ_LINE_COUNT = 10000;
    public static final int BATCH_READ_LINE_TOTAL_LEN = 1024 * 1024;
    public static final int CACHE_QUEUE_SIZE = 10 * BATCH_READ_LINE_COUNT;
    public static int DEFAULT_BUFFER_SIZE = 64 * 1024;
    private final SimpleDateFormat RECORD_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private final Gson GSON = new Gson();

    public File file;
    public long position = 0;
    public long bytePosition = 0;
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
    public volatile long monitorUpdateTime;
    private long lastTime = 0;
    private final byte[] inBuf = new byte[DEFAULT_BUFFER_SIZE];
    private int maxPackSize;
    private final long monitorActiveInterval = 60 * 1000;
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(CACHE_QUEUE_SIZE);
    private final StringBuffer sb = new StringBuffer();
    private boolean needMetadata = false;

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

    @Override
    public Message read() {
        String data = null;
        try {
            data = queue.poll(DEFAULT_JOB_READ_WAIT_TIMEOUT, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.warn("poll {} data get interrupted.", file.getPath(), e);
        }
        if (data == null) {
            keepMonitorActive();
            return null;
        } else {
            MemoryManager.getInstance().release(AGENT_GLOBAL_READER_QUEUE_PERMIT, data.length());
        }
        Message finalMsg = createMessage(data);
        if (finalMsg == null) {
            return null;
        }
        boolean channelPermit = MemoryManager.getInstance()
                .tryAcquire(AGENT_GLOBAL_CHANNEL_PERMIT, finalMsg.getBody().length);
        if (channelPermit == false) {
            LOGGER.warn("channel tryAcquire failed");
            MemoryManager.getInstance().printDetail(AGENT_GLOBAL_CHANNEL_PERMIT);
            AgentUtils.silenceSleepInSeconds(1);
            return null;
        }
        return finalMsg;
    }

    private Message createMessage(String data) {
        String msgWithMetaData = fillMetaData(data);
        AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, inlongGroupId, inlongStreamId,
                System.currentTimeMillis(), 1, msgWithMetaData.length());
        readerMetric.pluginReadSuccessCount.incrementAndGet();
        readerMetric.pluginReadCount.incrementAndGet();
        String proxyPartitionKey = jobConf.get(PROXY_SEND_PARTITION_KEY, DigestUtils.md5Hex(inlongGroupId));
        Map<String, String> header = new HashMap<>();
        header.put(PROXY_KEY_DATA, proxyPartitionKey);
        Message finalMsg = new DefaultMessage(msgWithMetaData.getBytes(StandardCharsets.UTF_8), header);
        // if the message size is greater than max pack size,should drop it.
        if (finalMsg.getBody().length > maxPackSize) {
            LOGGER.warn("message size is {}, greater than max pack size {}, drop it!",
                    finalMsg.getBody().length, maxPackSize);
            return null;
        }
        return finalMsg;
    }

    public void keepMonitorActive() {
        if (!isMonitorActive()) {
            LOGGER.error("monitor not active, create a new one");
            MonitorTextFile.getInstance().monitor(this);
        }
    }

    private boolean isMonitorActive() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - monitorUpdateTime > monitorActiveInterval) {
            return false;
        }
        return true;
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

    @Override
    public void init(JobProfile jobConf) {
        try {
            LOGGER.info("FileReaderOperator init: {}", jobConf.toJsonStr());
            this.jobConf = jobConf;
            super.init(jobConf);
            this.instanceId = jobConf.getInstanceId();
            this.maxPackSize = jobConf.getInt(PROXY_PACKAGE_MAX_SIZE, DEFAULT_PROXY_PACKAGE_MAX_SIZE);
            initReadTimeout(jobConf);
            String md5 = AgentUtils.getFileMd5(file);
            if (StringUtils.isNotBlank(this.md5) && !this.md5.equals(md5)) {
                LOGGER.warn("md5 is differ from origin, origin: {}, new {}", this.md5, md5);
            }
            LOGGER.info("file name for task is {}, md5 is {}", file, md5);
            monitorUpdateTime = System.currentTimeMillis();
            MonitorTextFile.getInstance().monitor(this);
            if (!jobConf.get(JOB_FILE_MONITOR_STATUS, JOB_FILE_MONITOR_DEFAULT_STATUS)
                    .equals(JOB_FILE_MONITOR_DEFAULT_STATUS)) {
                readEndpoint = Files.lines(file.toPath()).count();
            }
            try {
                position = PositionManager.getInstance().getPosition(getReadSource(), instanceId);
            } catch (Exception ex) {
                position = 0;
                LOGGER.error("get position from position manager error, only occur in ut: {}", ex.getMessage());
            }
            this.bytePosition = getStartBytePosition(position);
            LOGGER.info("FileReaderOperator init file {} instanceId {} history position {} readEndpoint {}",
                    getReadSource(),
                    instanceId,
                    position, readEndpoint);
            if (isIncrement(jobConf)) {
                LOGGER.info("FileReaderOperator DataCollectType INCREMENT: start bytePosition {},{}",
                        file.length(), file.getAbsolutePath());
                this.bytePosition = file.length();
                try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file.getPath()))) {
                    lineNumberReader.skip(Long.MAX_VALUE);
                    position = lineNumberReader.getLineNumber();
                    PositionManager.getInstance().updateSinkPosition(
                            getJobInstanceId(), getReadSource(), position, true);
                    LOGGER.info("for increment update {}, position to {}", file.getAbsolutePath(), position);

                } catch (IOException ex) {
                    LOGGER.error("get position error, file absolute path: {}", file.getAbsolutePath());
                }
            }
            try {
                registerMeta(jobConf);
            } catch (Exception ex) {
                LOGGER.error("init metadata error", ex);
            }
            inited = true;
        } catch (Exception ex) {
            throw new FileException("error init stream for " + file.getPath(), ex);
        }
    }

    private long getStartBytePosition(long lineNum) throws IOException {
        long pos = 0;
        long readCount = 0;
        RandomAccessFile input = null;
        try {
            input = new RandomAccessFile(file, "r");
            while (readCount < lineNum) {
                List<String> lines = new ArrayList<>();
                pos = readLines(input, pos, lines, Math.min((int) (lineNum - readCount), BATCH_READ_LINE_COUNT),
                        BATCH_READ_LINE_TOTAL_LEN, true);
                readCount += lines.size();
                if (lines.size() == 0) {
                    LOGGER.error("getStartBytePosition LineNum {} larger than the real file");
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("getStartBytePosition error {}", e.getMessage());
        } finally {
            if (input != null) {
                input.close();
            }
        }
        LOGGER.info("getStartBytePosition {} LineNum {} position {}", getReadSource(), lineNum, pos);
        return pos;
    }

    private boolean isIncrement(JobProfile jobConf) {
        if (jobConf.hasKey(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE) && DataCollectType.INCREMENT
                .equalsIgnoreCase(jobConf.get(JobConstants.JOB_FILE_CONTENT_COLLECT_TYPE))
                && isFirstStore(jobConf)) {
            return true;
        }
        return false;
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
        LOGGER.info("destroy read source name {}", getReadSource());
        finished = true;
        while (!queue.isEmpty()) {
            String data = null;
            try {
                data = queue.poll(DEFAULT_JOB_READ_WAIT_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                LOGGER.warn("poll {} data get interrupted.", file.getPath(), e);
            }
            if (data != null) {
                MemoryManager.getInstance().release(AGENT_GLOBAL_READER_QUEUE_PERMIT, data.length());
            }
        }
        queue.clear();
        LOGGER.info("destroy read source name {} end", getReadSource());
        LOGGER.info("destroy reader with read {} num {}",
                metricName, readerMetric == null ? 0 : readerMetric.pluginReadCount.get());
    }

    public String fillMetaData(String message) {
        if (!needMetadata) {
            return message;
        }
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

    public void registerMeta(JobProfile jobConf) {
        if (!jobConf.hasKey(JOB_FILE_META_ENV_LIST)) {
            return;
        }
        String[] env = jobConf.get(JOB_FILE_META_ENV_LIST).split(COMMA);
        Arrays.stream(env).forEach(data -> {
            if (data.equalsIgnoreCase(KUBERNETES)) {
                needMetadata = true;
                new KubernetesMetadataProvider(this).getData();
            } else if (data.equalsIgnoreCase(ENV_CVM)) {
                needMetadata = true;
                metadata.put(METADATA_HOST_NAME, AgentUtils.getLocalHost());
                metadata.put(METADATA_SOURCE_IP, AgentUtils.fetchLocalIp());
                metadata.put(METADATA_FILE_NAME, file.getName());
            }
        });
    }

    public void fetchData() throws IOException {
        boolean readFromPosPermit = false;
        while (readFromPosPermit == false) {
            readFromPosPermit = MemoryManager.getInstance()
                    .tryAcquire(AGENT_GLOBAL_READER_SOURCE_PERMIT, BATCH_READ_LINE_TOTAL_LEN);
            if (readFromPosPermit == false) {
                LOGGER.warn("fetchData tryAcquire failed");
                MemoryManager.getInstance().printDetail(AGENT_GLOBAL_READER_SOURCE_PERMIT);
                AgentUtils.silenceSleepInSeconds(1);
            }
        }
        List<String> lines = readFromPos(bytePosition);
        if (!lines.isEmpty()) {
            LOGGER.info("path is {}, line is {}, byte position is {}, reads data lines {}",
                    file.getName(), position, bytePosition, lines.size());
        }
        List<String> resultLines = lines;
        resultLines.forEach(line -> {
            boolean offerPermit = false;
            while (offerPermit != true) {
                offerPermit = MemoryManager.getInstance().tryAcquire(AGENT_GLOBAL_READER_QUEUE_PERMIT, line.length());
                if (offerPermit != true) {
                    LOGGER.warn("offerPermit tryAcquire failed");
                    MemoryManager.getInstance().printDetail(AGENT_GLOBAL_READER_QUEUE_PERMIT);
                    AgentUtils.silenceSleepInSeconds(1);
                }
            }
            try {
                boolean offerSuc = false;
                while (offerSuc != true) {
                    offerSuc = queue.offer(line, 1, TimeUnit.SECONDS);
                }
                LOGGER.debug("Read from file {} for {}", getReadSource(), line);
            } catch (InterruptedException e) {
                LOGGER.error("fetchData offer failed {}", e.getMessage());
            }
        });
        MemoryManager.getInstance().release(AGENT_GLOBAL_READER_SOURCE_PERMIT, BATCH_READ_LINE_TOTAL_LEN);
        if (position >= readEndpoint) {
            LOGGER.info("read to the end, set finished position {} readEndpoint {}", position, readEndpoint);
            finished = true;
        }
    }

    private List<String> readFromPos(long pos) throws IOException {
        List<String> lines = new ArrayList<>();
        RandomAccessFile input = null;
        try {
            input = new RandomAccessFile(file, "r");
            bytePosition = readLines(input, pos, lines, BATCH_READ_LINE_COUNT, BATCH_READ_LINE_TOTAL_LEN, false);
            position += lines.size();
        } catch (Exception e) {
            LOGGER.error("readFromPos error {}", e.getMessage());
        } finally {
            if (input != null) {
                input.close();
            }
        }
        return lines;
    }

    /**
     * Read new lines.
     *
     * @param reader The file to read
     * @return The new position after the lines have been read
     * @throws java.io.IOException if an I/O error occurs.
     */
    private long readLines(RandomAccessFile reader, long pos, List<String> lines, int maxLineCount, int maxLineTotalLen,
            boolean isCounting)
            throws IOException {
        if (maxLineCount == 0) {
            return pos;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        reader.seek(pos);
        long rePos = pos; // position to re-read
        int num;
        int lineTotalLen = 0;
        LOGGER.debug("readLines from {}", pos);
        boolean overLen = false;
        while ((num = reader.read(inBuf)) != -1) {
            int i = 0;
            for (; i < num; i++) {
                byte ch = inBuf[i];
                switch (ch) {
                    case '\n':
                        if (isCounting) {
                            lines.add(new String(""));
                        } else {
                            String temp = new String(baos.toByteArray(), StandardCharsets.UTF_8);
                            lines.add(temp);
                            lineTotalLen += temp.length();
                        }
                        rePos = pos + i + 1;
                        if (overLen) {
                            LOGGER.warn("readLines over len finally string len {}",
                                    new String(baos.toByteArray()).length());
                        }
                        baos.reset();
                        overLen = false;
                        break;
                    case '\r':
                        break;
                    default:
                        if (baos.size() < maxPackSize) {
                            baos.write(ch);
                        } else {
                            overLen = true;
                        }
                }
                if (lines.size() >= maxLineCount || lineTotalLen >= maxLineTotalLen) {
                    break;
                }
            }
            if (lines.size() >= maxLineCount || lineTotalLen >= maxLineTotalLen) {
                break;
            }
            if (i == num) {
                pos = reader.getFilePointer();
            }
        }
        baos.close();
        reader.seek(rePos); // Ensure we can re-read if necessary
        return rePos;
    }

    private boolean isFirstStore(JobProfile jobConf) {
        boolean isFirst = true;
        if (jobConf.hasKey(JobConstants.JOB_STORE_TIME)) {
            long jobStoreTime = Long.parseLong(jobConf.get(JobConstants.JOB_STORE_TIME));
            long storeTime = AgentConfiguration.getAgentConf().getLong(
                    AgentConstants.AGENT_JOB_STORE_TIME, AgentConstants.DEFAULT_JOB_STORE_TIME);
            if (System.currentTimeMillis() - jobStoreTime > storeTime) {
                isFirst = false;
            }
        }
        LOGGER.info("isFirst {}, {}", file.getAbsolutePath(), isFirst);
        return isFirst;
    }
}
