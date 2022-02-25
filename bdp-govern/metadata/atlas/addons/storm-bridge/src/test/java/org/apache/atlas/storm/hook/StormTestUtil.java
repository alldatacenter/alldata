/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.storm.hook;

import org.apache.storm.Config;
import org.apache.storm.ILocalCluster;
import org.apache.storm.Testing;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.testing.TestGlobalCount;
import org.apache.storm.testing.TestWordCounter;
import org.apache.storm.testing.TestWordSpout;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.utils.Utils;

import java.util.HashMap;

/**
 * An until to create a test topology.
 */
final class StormTestUtil {

    private StormTestUtil() {
    }

    public static ILocalCluster createLocalStormCluster() {
        // start a local storm cluster
        HashMap<String,Object> localClusterConf = new HashMap<>();
        localClusterConf.put("nimbus-daemon", true);
        return Testing.getLocalCluster(localClusterConf);
    }

    public static StormTopology createTestTopology() {
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("words", new TestWordSpout(), 10);
        builder.setBolt("count", new TestWordCounter(), 3).shuffleGrouping("words");
        builder.setBolt("globalCount", new TestGlobalCount(), 2).shuffleGrouping("count");

        return builder.createTopology();
    }

    public static Config submitTopology(ILocalCluster stormCluster, String topologyName,
                                        StormTopology stormTopology) throws Exception {
        Config stormConf = new Config();
        stormConf.putAll(Utils.readDefaultConfig());
        stormConf.put("storm.cluster.mode", "local");
        stormConf.setDebug(true);
        stormConf.setMaxTaskParallelism(3);
        stormConf.put(Config.STORM_TOPOLOGY_SUBMISSION_NOTIFIER_PLUGIN,
                org.apache.atlas.storm.hook.StormAtlasHook.class.getName());

        stormCluster.submitTopology(topologyName, stormConf, stormTopology);

        Thread.sleep(10000);
        return stormConf;
    }
}
