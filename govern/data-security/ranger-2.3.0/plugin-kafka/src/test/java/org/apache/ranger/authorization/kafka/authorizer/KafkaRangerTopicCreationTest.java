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

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.utils.Time;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Some;

import java.io.File;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class KafkaRangerTopicCreationTest {
    private final static Logger LOG = LoggerFactory.getLogger(KafkaRangerTopicCreationTest.class);

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
        System.out.println("Base Dir " + basedir);

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
        kafkaServer = new KafkaServer(config, Time.SYSTEM, new Some<String>("KafkaRangerTopicCreationTest"), false);
        kafkaServer.startup();
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

    @Test
    public void testCreateTopic() throws Exception {
            final String topic = "test";
            Properties properties = new Properties();
            properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + port);
            properties.put("client.id", "test-consumer-id");
            properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
            AdminClient client = KafkaAdminClient.create(properties);
            CreateTopicsResult result = client.createTopics(Arrays.asList(new NewTopic(topic, 1, (short) 1)));
            result.values().get(topic).get();
            for (Map.Entry<String, KafkaFuture<Void>> entry : result.values().entrySet()) {
                System.out.println("Create Topic : " + entry.getKey() + " " +
                        "isCancelled : " + entry.getValue().isCancelled() + " " +
                        "isCompletedExceptionally : " + entry.getValue().isCompletedExceptionally() + " " +
                        "isDone : " + entry.getValue().isDone());
            }
    }
}
