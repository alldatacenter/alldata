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

import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

public class MqttReader extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MqttReader.class);

    public static final String JOB_MQTT_USERNAME = "job.mqttJob.userName";
    public static final String JOB_MQTT_PASSWORD = "job.mqttJob.password";
    public static final String JOB_MQTT_SERVER_URI = "job.mqttJob.serverURI";
    public static final String JOB_MQTT_TOPIC = "job.mqttJob.topic";
    public static final String JOB_MQTT_CONNECTION_TIMEOUT = "job.mqttJob.connectionTimeOut";
    public static final String JOB_MQTT_KEEPALIVE_INTERVAL = "job.mqttJob.keepAliveInterval";
    public static final String JOB_MQTT_QOS = "job.mqttJob.qos";
    public static final String JOB_MQTT_CLEAN_SESSION = "job.mqttJob.cleanSession";
    public static final String JOB_MQTT_CLIENT_ID_PREFIX = "job.mqttJob.clientIdPrefix";
    public static final String JOB_MQTT_QUEUE_SIZE = "job.mqttJob.queueSize";
    public static final String JOB_MQTT_AUTOMATIC_RECONNECT = "job.mqttJob.automaticReconnect";
    public static final String JOB_MQTT_VERSION = "job.mqttJob.mqttVersion";

    private boolean finished = false;

    private boolean destroyed = false;

    private MqttClient client;

    private MqttConnectOptions options;

    private String serverURI;
    private String userName;
    private String password;
    private String topic;
    private int qos;
    private boolean cleanSession = false;
    private boolean automaticReconnect = true;
    private JobProfile jobProfile;
    private String instanceId;
    private String clientId;
    private int mqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT;

    private LinkedBlockingQueue<DefaultMessage> mqttMessagesQueue;

    public MqttReader(String topic) {
        this.topic = topic;
    }

    /**
     * set global params value
     *
     * @param jobConf
     */
    private void setGlobalParamsValue(JobProfile jobConf) {
        mqttMessagesQueue = new LinkedBlockingQueue<>(jobConf.getInt(JOB_MQTT_QUEUE_SIZE, 1000));
        instanceId = jobConf.getInstanceId();
        userName = jobConf.get(JOB_MQTT_USERNAME);
        password = jobConf.get(JOB_MQTT_PASSWORD);
        serverURI = jobConf.get(JOB_MQTT_SERVER_URI);
        topic = jobConf.get(JOB_MQTT_TOPIC);
        clientId = jobConf.get(JOB_MQTT_CLIENT_ID_PREFIX, "mqtt_client") + "_" + UUID.randomUUID();
        cleanSession = jobConf.getBoolean(JOB_MQTT_CLEAN_SESSION, false);
        automaticReconnect = jobConf.getBoolean(JOB_MQTT_AUTOMATIC_RECONNECT, true);
        qos = jobConf.getInt(JOB_MQTT_QOS, 1);
        mqttVersion = jobConf.getInt(JOB_MQTT_VERSION, MqttConnectOptions.MQTT_VERSION_DEFAULT);

        options = new MqttConnectOptions();
        options.setCleanSession(cleanSession);
        options.setConnectionTimeout(jobConf.getInt(JOB_MQTT_CONNECTION_TIMEOUT, 10));
        options.setKeepAliveInterval(jobConf.getInt(JOB_MQTT_KEEPALIVE_INTERVAL, 20));
        options.setUserName(userName);
        options.setPassword(password.toCharArray());
        options.setAutomaticReconnect(automaticReconnect);
        options.setMqttVersion(mqttVersion);
    }

    /**
     * connect to MQTT Broker
     */
    private void connect() {
        try {
            synchronized (MqttReader.class) {
                client = new MqttClient(serverURI, clientId, new MemoryPersistence());
                client.setCallback(new MqttCallback() {

                    @Override
                    public void connectionLost(Throwable cause) {
                        LOGGER.error("the mqtt jobId:{}, serverURI:{}, connection lost, {} ", instanceId,
                                serverURI, cause);
                        reconnect();
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        Map<String, String> headerMap = new HashMap<>();
                        headerMap.put("record.topic", topic);
                        headerMap.put("record.messageId", String.valueOf(message.getId()));
                        headerMap.put("record.qos", String.valueOf(message.getQos()));
                        byte[] recordValue = message.getPayload();
                        mqttMessagesQueue.put(new DefaultMessage(recordValue, headerMap));

                        AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, inlongGroupId, inlongStreamId,
                                System.currentTimeMillis(), 1, recordValue.length);

                        readerMetric.pluginReadSuccessCount.incrementAndGet();
                        readerMetric.pluginReadCount.incrementAndGet();
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });
                client.connect(options);
                client.subscribe(topic, qos);
            }
            LOGGER.info("the mqtt subscribe topic is [{}], qos is [{}]", topic, qos);
        } catch (Exception e) {
            LOGGER.error("init mqtt client error {}. jobId:{},serverURI:{},clientId:{}", e, instanceId, serverURI,
                    clientId);
        }
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        jobProfile = jobConf;
        LOGGER.info("init mqtt reader with jobConf {}", jobConf.toJsonStr());
        setGlobalParamsValue(jobConf);
        connect();
    }

    private void reconnect() {
        if (!client.isConnected()) {
            try {
                client.connect(options);
                LOGGER.info("the mqtt client reconnect success. jobId:{}, serverURI:{}, clientId:{}", instanceId,
                        serverURI, clientId);
            } catch (Exception e) {
                LOGGER.error("reconnect mqtt client error {}. jobId:{}, serverURI:{}, clientId:{}", e, instanceId,
                        serverURI, clientId);
            }
        }
    }

    @Override
    public Message read() {
        if (!mqttMessagesQueue.isEmpty()) {
            return getMqttMessage();
        } else {
            return null;
        }
    }

    private DefaultMessage getMqttMessage() {
        return mqttMessagesQueue.poll();
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getReadSource() {
        return instanceId;
    }

    @Override
    public void setReadTimeout(long mill) {
    }

    @Override
    public void setWaitMillisecond(long millis) {
    }

    @Override
    public String getSnapshot() {
        return StringUtils.EMPTY;
    }

    @Override
    public void finishRead() {
        finished = true;
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    private void disconnect() {
        try {
            client.disconnect();
        } catch (MqttException e) {
            LOGGER.error("disconnect mqtt client error {}. jobId:{},serverURI:{},clientId:{}", e, instanceId, serverURI,
                    clientId);
        }
    }

    public void setReadSource(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                disconnect();
                destroyed = true;
            }
        }
    }
}
