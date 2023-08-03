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

package org.apache.ranger.authorization.storm;

import java.security.Principal;
import java.security.PrivilegedExceptionAction;

import javax.security.auth.Subject;

import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.RebalanceOptions;
import org.apache.storm.topology.TopologyBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

/**
 * A simple test that wires a WordSpout + WordCounterBolt into a topology and runs it. The "RangerStormAuthorizer" takes care of authorization.
 * The policies state that "bob" can do anything with the "word-count" topology. In addition, "bob" can create/kill the "temp*" topologies, but do
 * nothing else.
 *
 * In addition we have some TAG based policies created in Atlas and synced into Ranger:
 *
 * a) The tag "StormTopologyTag" is associated with "create/kill" permissions to the "bob" user for the "stormdev" topology.
 */

// TODO to fix Strom Test working with Hadoop 3.0.0
@Ignore
public class StormRangerAuthorizerTest {

    private static LocalCluster cluster;

    @org.junit.BeforeClass
    public static void setup() throws Exception {
        cluster = new LocalCluster();

        final Config conf = new Config();
        conf.setDebug(true);

        final TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("words", new WordSpout());
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");

        // bob can create a new topology
        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                cluster.submitTopology("word-count", conf, builder.createTopology());
                return null;
            }
        });

    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                cluster.killTopology("word-count");
                return null;
            }
        });

        cluster.shutdown();
        System.clearProperty("storm.conf.file");
    }

    // "bob" can't create topologies other than "word-count" and "temp*"
    @Test
    public void testCreateTopologyBob() throws Exception {
        final Config conf = new Config();
        conf.setDebug(true);

        final TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("words", new WordSpout());
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");

        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                try {
                    cluster.submitTopology("word-count2", conf, builder.createTopology());
                    Assert.fail("Authorization failure expected");
                } catch (Exception ex) {
                    // expected
                }

                return null;
            }
        });
    }

    @Test
    public void testTopologyActivation() throws Exception {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {

                // Deactivate "word-count"
                cluster.deactivate("word-count");

                // Create a new topology called "temp1"
                final Config conf = new Config();
                conf.setDebug(true);

                final TopologyBuilder builder = new TopologyBuilder();
                builder.setSpout("words", new WordSpout());
                builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");
                cluster.submitTopology("temp1", conf, builder.createTopology());

                // Try to deactivate "temp1"
                try {
                    cluster.deactivate("temp1");
                    Assert.fail("Authorization failure expected");
                } catch (Exception ex) {
                    // expected
                }

                // Re-activate "word-count"
                cluster.activate("word-count");

                // Kill temp1
                cluster.killTopology("temp1");

                return null;
            }
        });
    }

    @Test
    public void testTopologyRebalancing() throws Exception {
        final Subject subject = new Subject();
        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                RebalanceOptions options = new RebalanceOptions();

                // Create a new topology called "temp2"
                final Config conf = new Config();
                conf.setDebug(true);

                final TopologyBuilder builder = new TopologyBuilder();
                builder.setSpout("words", new WordSpout());
                builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");
                cluster.submitTopology("temp2", conf, builder.createTopology());

                // Try to rebalance "temp2"
                try {
                    cluster.rebalance("temp2", options);
                    Assert.fail("Authorization failure expected");
                } catch (Exception ex) {
                    // expected
                }

                // Kill temp2
                cluster.killTopology("temp2");

                return null;
            }
        });
    }

    @Test
    public void testTAGBasedPolicy() throws Exception {
        final Config conf = new Config();
        conf.setDebug(true);

        final TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("words", new WordSpout());
        builder.setBolt("counter", new WordCounterBolt()).shuffleGrouping("words");

        final Subject subject = new Subject();

        subject.getPrincipals().add(new SimplePrincipal("bob"));
        Subject.doAs(subject, new PrivilegedExceptionAction<Void>() {
            public Void run() throws Exception {
                // bob can create the "stormdev" topology
                cluster.submitTopology("stormdev", conf, builder.createTopology());

                cluster.killTopology("stormdev");

                // but not the "stormdev2" topology
                try {
                    cluster.submitTopology("stormdev2", conf, builder.createTopology());
                    Assert.fail("Authorization failure expected");
                } catch (Exception ex) {
                    // expected
                }

                return null;
            }
        });

    }

    private static class SimplePrincipal implements Principal {

        private final String name;

        public SimplePrincipal(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

    }
}
