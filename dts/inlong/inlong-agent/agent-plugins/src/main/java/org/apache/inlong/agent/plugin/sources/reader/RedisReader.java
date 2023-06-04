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

package org.apache.inlong.agent.plugin.sources.reader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.moilioncircle.redis.replicator.RedisReplicator;
import com.moilioncircle.redis.replicator.Replicator;
import com.moilioncircle.redis.replicator.cmd.CommandName;
import com.moilioncircle.redis.replicator.cmd.impl.DefaultCommand;
import com.moilioncircle.redis.replicator.cmd.parser.DefaultCommandParser;
import com.moilioncircle.redis.replicator.event.Event;
import com.moilioncircle.redis.replicator.event.EventListener;
import com.moilioncircle.redis.replicator.event.PostRdbSyncEvent;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueHash;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueList;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueSet;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueString;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyStringValueZSet;
import com.moilioncircle.redis.replicator.rdb.datatype.KeyValuePair;
import com.moilioncircle.redis.replicator.rdb.datatype.ZSetEntry;
import org.apache.commons.lang.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Redis data reader
 */
public class RedisReader extends AbstractReader {

    public static final String REDIS_READER_TAG_NAME = "AgentRedisMetric";
    public static final String JOB_REDIS_PORT = "job.redisJob.port";
    public static final String JOB_REDIS_HOSTNAME = "job.redisJob.hostname";
    public static final String JOB_REDIS_SSL = "job.redisJob.ssl";
    public static final String JOB_REDIS_AUTHUSER = "job.redisJob.authUser";
    public static final String JOB_REDIS_AUTHPASSWORD = "job.redisJob.authPassword";
    public static final String JOB_REDIS_READTIMEOUT = "job.redisJob.readTimeout";
    public static final String JOB_REDIS_QUEUE_SIZE = "job.redisJob.queueSize";
    public static final String JOB_REDIS_REPLID = "job.redisJob.replId";
    public static final String JOB_REDIS_OFFSET = "job.redisJob.offset";
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisReader.class);
    private String port;
    private String hostName;
    private boolean ssl;
    private String authUser;
    private String authPassword;
    private String readTimeout;
    private String instanceId;
    private String replId;
    private String snapShot;
    private boolean destroyed;
    private Replicator redisReplicator;
    private LinkedBlockingQueue<String> redisMessageQueue;
    private boolean finished = false;
    private ExecutorService executor;
    private Gson gson;

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        LOGGER.info("Init redis reader with jobConf {}", jobConf.toJsonStr());
        port = jobConf.get(JOB_REDIS_PORT);
        hostName = jobConf.get(JOB_REDIS_HOSTNAME);
        ssl = jobConf.getBoolean(JOB_REDIS_SSL, false);
        authUser = jobConf.get(JOB_REDIS_AUTHUSER, "");
        authPassword = jobConf.get(JOB_REDIS_AUTHPASSWORD, "");
        readTimeout = jobConf.get(JOB_REDIS_READTIMEOUT, "");
        replId = jobConf.get(JOB_REDIS_REPLID, "");
        snapShot = jobConf.get(JOB_REDIS_OFFSET, "-1");
        instanceId = jobConf.getInstanceId();
        finished = false;
        redisMessageQueue = new LinkedBlockingQueue<>(jobConf.getInt(JOB_REDIS_QUEUE_SIZE, 10000));
        initGson();
        String uri = getRedisUri();
        try {
            redisReplicator = new RedisReplicator(uri);
            initReplicator();
            redisReplicator.addEventListener(new EventListener() {

                @Override
                public void onEvent(Replicator replicator, Event event) {
                    try {
                        if (event instanceof DefaultCommand || event instanceof KeyValuePair<?, ?>) {
                            String eventJson = gson.toJson(event);
                            redisMessageQueue.put(eventJson);
                            AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, inlongGroupId, inlongStreamId,
                                    System.currentTimeMillis(), 1, eventJson.length());
                            readerMetric.pluginReadCount.incrementAndGet();
                        }
                        if (event instanceof PostRdbSyncEvent) {
                            snapShot = String.valueOf(replicator.getConfiguration().getReplOffset());
                            LOGGER.info("after rdb snapShot is: {}", snapShot);
                        }
                    } catch (InterruptedException e) {
                        readerMetric.pluginReadFailCount.incrementAndGet();
                        LOGGER.error("Read redis data error", e);
                    }
                }
            });
            executor = Executors.newSingleThreadExecutor();
            executor.execute(new Thread(() -> {
                try {
                    redisReplicator.open();
                } catch (IOException e) {
                    LOGGER.error("Redis source error", e);
                }
            }));
        } catch (URISyntaxException | IOException e) {
            readerMetric.pluginReadFailCount.addAndGet(1);
            LOGGER.error("Connect to redis {}:{} failed.", hostName, port);
        }
    }

    private String getRedisUri() {
        StringBuffer sb = new StringBuffer("redis://");
        sb.append(hostName).append(":").append(port);
        sb.append("?");
        if (!StringUtils.isEmpty(authPassword)) {
            sb.append("authPassword=").append(authPassword).append("&");
        }
        if (!StringUtils.isEmpty(authUser)) {
            sb.append("authUser=").append(authUser).append("&");
        }
        if (!StringUtils.isEmpty(readTimeout)) {
            sb.append("readTimeout=").append(readTimeout).append("&");
        }
        if (ssl) {
            sb.append("ssl=").append("yes").append("&");
        }
        if (!StringUtils.isEmpty(snapShot)) {
            sb.append("replOffset=").append(snapShot).append("&");
        }
        if (!StringUtils.isEmpty(replId)) {
            sb.append("replId=").append(replId).append("&");
        }
        if (sb.charAt(sb.length() - 1) == '?' || sb.charAt(sb.length() - 1) == '&') {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                try {
                    executor.shutdown();
                    redisReplicator.close();
                } catch (IOException e) {
                    LOGGER.error("Redis reader close failed.");
                }
                destroyed = true;
            }
        }
    }

    @Override
    public Message read() {
        if (!redisMessageQueue.isEmpty()) {
            readerMetric.pluginReadCount.incrementAndGet();
            return new DefaultMessage(redisMessageQueue.poll().getBytes());
        } else {
            return null;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getReadSource() {
        return instanceId;
    }

    public void setReadSource(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void setReadTimeout(long mill) {

    }

    @Override
    public void setWaitMillisecond(long millis) {

    }

    @Override
    public String getSnapshot() {
        return snapShot;
    }

    @Override
    public void finishRead() {
        finished = true;
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    /**
     * init GSON parser
     */
    private void initGson() {
        gson = new GsonBuilder().registerTypeAdapter(KeyStringValueHash.class, new TypeAdapter<KeyStringValueHash>() {

            @Override
            public void write(JsonWriter out, KeyStringValueHash kv) throws IOException {
                out.beginObject();
                out.name("DB").beginObject();
                out.name("dbNumber").value(kv.getDb().getDbNumber());
                out.name("dbSize").value(kv.getDb().getDbsize());
                out.name("expires").value(kv.getDb().getExpires());
                out.endObject();
                out.name("valueRdbType").value(kv.getValueRdbType());
                out.name("key").value(new String(kv.getKey()));
                out.name("value").beginObject();
                for (byte[] b : kv.getValue().keySet()) {
                    out.name(new String(b)).value(new String(kv.getValue().get(b)));
                }
                out.endObject();
                out.endObject();
            }

            @Override
            public KeyStringValueHash read(JsonReader in) throws IOException {
                return null;
            }
        }).registerTypeAdapter(DefaultCommand.class, new TypeAdapter<DefaultCommand>() {

            @Override
            public void write(JsonWriter out, DefaultCommand dc) throws IOException {
                out.beginObject();
                out.name("key").value(new String(dc.getCommand()));
                out.name("value").beginArray();
                for (byte[] bytes : dc.getArgs()) {
                    out.value(new String(bytes));
                }
                out.endArray();
                out.endObject();
            }

            @Override
            public DefaultCommand read(JsonReader in) throws IOException {
                return null;
            }
        })
                .registerTypeAdapter(KeyStringValueList.class, new TypeAdapter<KeyStringValueList>() {

                    @Override
                    public void write(JsonWriter out, KeyStringValueList kv) throws IOException {
                        out.beginObject();
                        out.name("key").value(new String(kv.getKey()));
                        out.name("value").beginArray();
                        for (byte[] bytes : kv.getValue()) {
                            out.value(new String(bytes));
                        }
                        out.endArray();
                        out.endObject();
                    }

                    @Override
                    public KeyStringValueList read(JsonReader in) throws IOException {
                        return null;
                    }
                })
                .registerTypeAdapter(KeyStringValueSet.class, new TypeAdapter<KeyStringValueSet>() {

                    @Override
                    public void write(JsonWriter out, KeyStringValueSet kv) throws IOException {
                        out.beginObject();
                        out.name("key").value(new String(kv.getKey()));
                        out.name("value").beginArray();
                        for (byte[] bytes : kv.getValue()) {
                            out.value(new String(bytes));
                        }
                        out.endArray();
                        out.endObject();
                    }

                    @Override
                    public KeyStringValueSet read(JsonReader in) throws IOException {
                        return null;
                    }
                })
                .registerTypeAdapter(KeyStringValueString.class, new TypeAdapter<KeyStringValueString>() {

                    @Override
                    public void write(JsonWriter out, KeyStringValueString kv) throws IOException {
                        out.beginObject();
                        out.name("key").value(new String(kv.getKey()));
                        out.name("value").value(new String(kv.getValue()));
                        out.endObject();
                    }

                    @Override
                    public KeyStringValueString read(JsonReader in) throws IOException {
                        return null;
                    }
                })
                .registerTypeAdapter(KeyStringValueZSet.class, new TypeAdapter<KeyStringValueZSet>() {

                    @Override
                    public void write(JsonWriter out, KeyStringValueZSet kv) throws IOException {
                        out.beginObject();
                        out.name("key").value(new String(kv.getKey()));
                        out.name("value").beginArray();
                        for (ZSetEntry entry : kv.getValue()) {
                            out.beginObject();
                            out.name("element").value(new String(entry.getElement()));
                            out.name("score").value(entry.getScore());
                            out.endObject();
                        }
                        out.endArray();
                        out.endObject();
                    }

                    @Override
                    public KeyStringValueZSet read(JsonReader in) throws IOException {
                        return null;
                    }
                })
                .create();
    }

    /**
     * init replicator's commandParser
     */
    private void initReplicator() {
        DefaultCommandParser defaultCommandParser = new DefaultCommandParser();
        redisReplicator.addCommandParser(CommandName.name("APPEND"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SETEX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("MSET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("DEL"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SADD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("HMSET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("HSET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LSET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("EXPIRE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("EXPIREAT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("GETSET"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("HSETNX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("MSETNX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PSETEX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SETNX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SETRANGE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("HDEL"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LPOP"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LPUSH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LPUSHX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LRem"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RPOP"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RPUSH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RPUSHX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZREM"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RENAME"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("INCR"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("DECR"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("INCRBY"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("DECRBY"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PERSIST"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SELECT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("FLUSHALL"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("FLUSHDB"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("HINCRBY"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZINCRBY"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("MOVE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SMOVE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PFADD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PFCOUNT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PFMERGE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SDIFFSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SINTERSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SUNIONSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZADD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZINTERSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZUNIONSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("BRPOPLPUSH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LINSERT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RENAMENX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RESTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PEXPIRE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PEXPIREAT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("GEOADD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("EVAL"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("EVALSHA"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SCRIPT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("PUBLISH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("BITOP"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("BITFIELD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SETBIT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SREM"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("UNLINK"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SWAPDB"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("MULTI"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("EXEC"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZREMRANGEBYSCORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZREMRANGEBYRANK"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZREMRANGEBYLEX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LTRIM"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("SORT"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("RPOPLPUSH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZPOPMIN"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZPOPMAX"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("REPLCONF"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XACK"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XADD"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XCLAIM"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XDEL"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XGROUP"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XTRIM"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("XSETID"), defaultCommandParser);
        // since redis 6.2
        redisReplicator.addCommandParser(CommandName.name("COPY"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("LMOVE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("BLMOVE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("ZDIFFSTORE"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("GEOSEARCHSTORE"), defaultCommandParser);
        // since redis 7.0
        redisReplicator.addCommandParser(CommandName.name("SPUBLISH"), defaultCommandParser);
        redisReplicator.addCommandParser(CommandName.name("FUNCTION"), defaultCommandParser);
    }
}
