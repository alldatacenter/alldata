/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.authorization.kafka.authorizer;

import java.io.File;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import kafka.server.KafkaServer;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.utils.Time;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kafka.server.KafkaConfig;
import scala.Some;

/**
 * A simple test that starts a Kafka broker, creates "test" and "dev" topics,
 * sends a message to them and consumes it.
 * The RangerKafkaAuthorizer enforces the following authorization rules:
 *
 *  - The "IT" group can do anything
 *  - The "public" group can "read/describe/write" on the "test" topic.
 *
 * Policies available from admin via:
 *
 * http://localhost:6080/service/plugins/policies/download/cl1_kafka
 *
 * Authentication is done via Kerberos/GSS.
 */
public class KafkaRangerAuthorizerGSSTest {
    private final static Logger LOG = LoggerFactory.getLogger(KafkaRangerAuthorizerGSSTest.class);

    private static KafkaServer kafkaServer;
    private static TestingServer zkServer;
    private static int port;
    private static Path tempDir;
    private static SimpleKdcServer kerbyServer;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        configureKerby(basedir);

        // JAAS Config file - We need to point to the correct keytab files
        Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/kafka_kerberos.jaas");
        String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        content = content.replaceAll("<basedir>", basedir);
        //content = content.replaceAll("zookeeper/localhost", "zookeeper/" + address);

        Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/kafka_kerberos.jaas");
        Files.write(path2, content.getBytes(StandardCharsets.UTF_8));

        System.setProperty("java.security.auth.login.config", path2.toString());

        // Set up Zookeeper to require SASL
        Map<String,Object> zookeeperProperties = new HashMap<>();
        zookeeperProperties.put("authProvider.1", "org.apache.zookeeper.server.auth.SASLAuthenticationProvider");
        zookeeperProperties.put("requireClientAuthScheme", "sasl");
        zookeeperProperties.put("jaasLoginRenew", "3600000");

        InstanceSpec instanceSpec = new InstanceSpec(null, -1, -1, -1, true, 1,-1, -1, zookeeperProperties, "localhost");

        zkServer = new TestingServer(instanceSpec, true);

        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();

        tempDir = Files.createTempDirectory("kafka");

        LOG.info("Port is {}", port);
        LOG.info("Temporary directory is at {}", tempDir);

        final Properties props = new Properties();
        props.put("broker.id", 1);
        props.put("host.name", "localhost");
        props.put("port", port);
        props.put("log.dir", tempDir.toString());
        props.put("zookeeper.connect", zkServer.getConnectString());
        props.put("replica.socket.timeout.ms", "1500");
        props.put("controlled.shutdown.enable", Boolean.TRUE.toString());
        // Enable SASL_PLAINTEXT
        props.put("listeners", "SASL_PLAINTEXT://localhost:" + port);
        props.put("security.inter.broker.protocol", "SASL_PLAINTEXT");
        props.put("sasl.enabled.mechanisms", "GSSAPI");
        props.put("sasl.mechanism.inter.broker.protocol", "GSSAPI");
        props.put("sasl.kerberos.service.name", "kafka");
        props.put("offsets.topic.replication.factor", (short) 1);
        props.put("offsets.topic.num.partitions", 1);

        // Plug in Apache Ranger authorizer
        props.put("authorizer.class.name", "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer");

        // Create users for testing
        UserGroupInformation.createUserForTesting("client@kafka.apache.org", new String[] {"public"});
        UserGroupInformation.createUserForTesting("kafka/localhost@kafka.apache.org", new String[] {"IT"});

        KafkaConfig config = new KafkaConfig(props);
        kafkaServer = new KafkaServer(config, Time.SYSTEM, new Some<String>("KafkaRangerAuthorizerGSSTest"), false);
        kafkaServer.startup();

        // Create some topics
        final Properties adminProps = new Properties();
        adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + port);
        adminProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        adminProps.put(SaslConfigs.SASL_MECHANISM, "GSSAPI");
        KafkaTestUtils.createSomeTopics(adminProps);
    }

    private static void configureKerby(String baseDir) throws Exception {

        //System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.conf", baseDir + "/target/krb5.conf");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("kafka.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(baseDir + "/target"));

        kerbyServer.init();

        // Create principals
        String zookeeper = "zookeeper/localhost@kafka.apache.org";
        String kafka = "kafka/localhost@kafka.apache.org";
        String client = "client@kafka.apache.org";

        kerbyServer.createPrincipal(zookeeper, "zookeeper");
        File keytabFile = new File(baseDir + "/target/zookeeper.keytab");
        kerbyServer.exportPrincipal(zookeeper, keytabFile);

        kerbyServer.createPrincipal(kafka, "kafka");
        keytabFile = new File(baseDir + "/target/kafka.keytab");
        kerbyServer.exportPrincipal(kafka, keytabFile);

        kerbyServer.createPrincipal(client, "client");
        keytabFile = new File(baseDir + "/target/client.keytab");
        kerbyServer.exportPrincipal(client, keytabFile);

        kerbyServer.start();
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        if (kafkaServer != null) {
            kafkaServer.shutdown();
        }
        if (zkServer != null) {
            zkServer.stop();
        }
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    // The "public" group can write to and read from "test"
    @Test
    public void testAuthorizedRead() {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        producerProps.put("sasl.mechanism", "GSSAPI");
        producerProps.put("sasl.kerberos.service.name", "kafka");

        final Producer<String, String> producer = new KafkaProducer<>(producerProps);

        // Create the Consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:" + port);
        consumerProps.put("group.id", "consumerTestGroup");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        consumerProps.put("sasl.mechanism", "GSSAPI");
        consumerProps.put("sasl.kerberos.service.name", "kafka");

        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        checkTopicExists(consumer);
        LOG.info("Subscribing to 'test'");
        consumer.subscribe(Arrays.asList("test"));

        sendMessage(producer);

        // Poll until we consume it
        ConsumerRecord<String, String> record = null;
        for (int i = 0; i < 1000; i++) {
            LOG.info("Waiting for messages {}. try", i);
            ConsumerRecords<String, String> records = consumer.poll(100);
            if (records.count() > 0) {
                LOG.info("Found {} messages", records.count());
                record = records.iterator().next();
                break;
            }
            sleep();
        }

        Assert.assertNotNull(record);
        Assert.assertEquals("somevalue", record.value());

        producer.close();
        consumer.close();
    }

    private void checkTopicExists(final KafkaConsumer<String, String> consumer) {
        Map<String, List<PartitionInfo>> topics = consumer.listTopics();
        while (!topics.containsKey("test")) {
            LOG.warn("Required topic is not available, only {} present", topics.keySet());
            sleep();
            topics = consumer.listTopics();
        }
        LOG.warn("Available topics: {}", topics.keySet());
    }

    private void sendMessage(final Producer<String, String> producer) {
        // Send a message
        try {
            LOG.info("Send a message to 'test'");
            producer.send(new ProducerRecord<String, String>("test", "somekey", "somevalue"));
            producer.flush();
        } catch (RuntimeException e) {
            LOG.error("Unable to send message to topic 'test' ", e);
        }
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            LOG.info("Interrupted sleep, nothing important");
        }
    }

    // The "public" group can't write to "dev"
    @Test
    public void testUnauthorizedWrite() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        producerProps.put("sasl.mechanism", "GSSAPI");
        producerProps.put("sasl.kerberos.service.name", "kafka");

        final Producer<String, String> producer = new KafkaProducer<>(producerProps);

        // Send a message
        try {
            Future<RecordMetadata> record =
                producer.send(new ProducerRecord<String, String>("dev", "somekey", "somevalue"));
            producer.flush();
            record.get();
        } catch (Exception ex) {
            Assert.assertTrue(ex.getMessage().contains("Not authorized to access topics"));
        }

        producer.close();
    }


    @Test
    public void testAuthorizedIdempotentWrite() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        producerProps.put("sasl.mechanism", "GSSAPI");
        producerProps.put("sasl.kerberos.service.name", "kafka");
        producerProps.put("enable.idempotence", "true");

        final Producer<String, String> producer = new KafkaProducer<>(producerProps);

        // Send a message
        producer.send(new ProducerRecord<String, String>("test", "somekey", "somevalue"));
        producer.flush();
        producer.close();
    }
}
