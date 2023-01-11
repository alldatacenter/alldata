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
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.Future;

import kafka.server.KafkaServer;
import org.apache.commons.io.FileUtils;
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
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.utils.Time;
import org.junit.Assert;
import org.junit.Test;

import kafka.server.KafkaConfig;
import scala.Some;

/**
 * A simple test that starts a Kafka broker, creates "test" and "dev" topics, sends a message to them and consumes it. We also plug in a 
 * CustomAuthorizer that enforces some authorization rules:
 * 
 *  - The "IT" group can do anything
 *  - The "public" group can "read/describe/write" on the "test" topic.
 *  - The "public" group can only "read/describe" on the "dev" topic, but not write.
 * 
 * Policies available from admin via:
 * 
 * http://localhost:6080/service/plugins/policies/download/cl1_kafka
 * 
 * Clients and services authenticate to Kafka using the SASL SSL protocol as part of this test.
 */
@org.junit.Ignore("Causing JVM to abort on some platforms")
public class KafkaRangerAuthorizerSASLSSLTest {
    
    private static KafkaServer kafkaServer;
    private static TestingServer zkServer;
    private static int port;
    private static String serviceKeystorePath;
    private static String clientKeystorePath;
    private static String truststorePath;
    
    @org.junit.BeforeClass
    public static void setup() throws Exception {
    	// JAAS Config file
        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        File f = new File(basedir + "/src/test/resources/kafka_plain.jaas");
        System.setProperty("java.security.auth.login.config", f.getPath());
        
    	// Create keys
    	String serviceDN = "CN=Service,O=Apache,L=Dublin,ST=Leinster,C=IE";
    	String clientDN = "CN=Client,O=Apache,L=Dublin,ST=Leinster,C=IE";
    	
    	// Create a truststore
    	KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    	keystore.load(null, "security".toCharArray());
    	
    	serviceKeystorePath = 
    			KafkaTestUtils.createAndStoreKey(serviceDN, serviceDN, BigInteger.valueOf(30), 
    					"sspass", "myservicekey", "skpass", keystore);
    	clientKeystorePath = 
    			KafkaTestUtils.createAndStoreKey(clientDN, clientDN, BigInteger.valueOf(31), 
    					"cspass", "myclientkey", "ckpass", keystore);
    	
    	File truststoreFile = File.createTempFile("kafkatruststore", ".jks");
    	try (OutputStream output = new FileOutputStream(truststoreFile)) {
    		keystore.store(output, "security".toCharArray());
    	}
    	truststorePath = truststoreFile.getPath();
    			
        zkServer = new TestingServer();
        
        // Get a random port
        ServerSocket serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
        serverSocket.close();
        
        final Properties props = new Properties();
        props.put("broker.id", 1);
        props.put("host.name", "localhost");
        props.put("port", port);
        props.put("log.dir", "/tmp/kafka");
        props.put("zookeeper.connect", zkServer.getConnectString());
        props.put("replica.socket.timeout.ms", "1500");
        props.put("controlled.shutdown.enable", Boolean.TRUE.toString());
        // Enable SASL_SSL
        props.put("listeners", "SASL_SSL://localhost:" + port);
        props.put("security.inter.broker.protocol", "SASL_SSL");
        props.put("sasl.enabled.mechanisms", "PLAIN");
        props.put("sasl.mechanism.inter.broker.protocol", "PLAIN");

        props.put("offsets.topic.replication.factor", (short) 1);
        props.put("offsets.topic.num.partitions", 1);

        props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, serviceKeystorePath);
        props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        // Plug in Apache Ranger authorizer
        props.put("authorizer.class.name", "org.apache.ranger.authorization.kafka.authorizer.RangerKafkaAuthorizer");
        
        // Create users for testing
        UserGroupInformation.createUserForTesting("alice", new String[] {"IT"});
        
        KafkaConfig config = new KafkaConfig(props);
        kafkaServer = new KafkaServer(config, Time.SYSTEM, new Some<String>("KafkaRangerAuthorizerSASLSSLTest"), false);
        kafkaServer.startup();

        // Create some topics
        final Properties adminProps = new Properties();
        adminProps.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + port);
        adminProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        adminProps.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        // ssl
        adminProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, serviceKeystorePath);
        adminProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        adminProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        adminProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        adminProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        KafkaTestUtils.createSomeTopics(adminProps);
    }
    
    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        if (kafkaServer != null) {
            kafkaServer.shutdown();
        }
        if (zkServer != null) {
            zkServer.stop();
        }
        
        File clientKeystoreFile = new File(clientKeystorePath);
        if (clientKeystoreFile.exists()) {
        	FileUtils.forceDelete(clientKeystoreFile);
        }
        File serviceKeystoreFile = new File(serviceKeystorePath);
        if (serviceKeystoreFile.exists()) {
        	FileUtils.forceDelete(serviceKeystoreFile);
        }
        File truststoreFile = new File(truststorePath);
        if (truststoreFile.exists()) {
        	FileUtils.forceDelete(truststoreFile);
        }
    }
    
    @Test
    public void testAuthorizedRead() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        producerProps.put("sasl.mechanism", "PLAIN");
        
        producerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        producerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, serviceKeystorePath);
        producerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        producerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final Producer<String, String> producer = new KafkaProducer<>(producerProps);
        
        // Create the Consumer
        Properties consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", "localhost:" + port);
        consumerProps.put("group.id", "test");
        consumerProps.put("enable.auto.commit", "true");
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("auto.commit.interval.ms", "1000");
        consumerProps.put("session.timeout.ms", "30000");
        consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        consumerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        consumerProps.put("sasl.mechanism", "PLAIN");
        
        consumerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        consumerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, clientKeystorePath);
        consumerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "cspass");
        consumerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "ckpass");
        consumerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        consumerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumer.subscribe(Arrays.asList("test"));
        
        // Send a message
        producer.send(new ProducerRecord<String, String>("test", "somekey", "somevalue"));
        producer.flush();
        
        // Poll until we consume it
        
        ConsumerRecord<String, String> record = null;
        for (int i = 0; i < 1000; i++) {
            ConsumerRecords<String, String> records = consumer.poll(100);
            if (records.count() > 0) {
                record = records.iterator().next();
                break;
            }
            Thread.sleep(1000);
        }

        Assert.assertNotNull(record);
        Assert.assertEquals("somevalue", record.value());

        producer.close();
        consumer.close();
    }
    
    @Test
    public void testAuthorizedWrite() throws Exception {
        // Create the Producer
        Properties producerProps = new Properties();
        producerProps.put("bootstrap.servers", "localhost:" + port);
        producerProps.put("acks", "all");
        producerProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        producerProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        producerProps.put("sasl.mechanism", "PLAIN");
        
        producerProps.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "JKS");
        producerProps.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, serviceKeystorePath);
        producerProps.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "sspass");
        producerProps.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "skpass");
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststorePath);
        producerProps.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "security");
        
        final Producer<String, String> producer = new KafkaProducer<>(producerProps);
        
        // Send a message
        Future<RecordMetadata> record = 
            producer.send(new ProducerRecord<String, String>("dev", "somekey", "somevalue"));
        producer.flush();
        record.get();

        producer.close();
    }
    
}
