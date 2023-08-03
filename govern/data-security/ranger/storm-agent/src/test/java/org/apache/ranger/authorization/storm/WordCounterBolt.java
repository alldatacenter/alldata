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

import java.util.HashMap;
import java.util.Map;

import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

/**
 * A Storm Bolt which reads in a word and counts it + outputs the word + current count
 */
public class WordCounterBolt extends BaseRichBolt {
    private OutputCollector outputCollector;
    private Map<String, Integer> countMap = new HashMap<>();

    @Override
    public void execute(Tuple tuple) {
        String word = tuple.getString(0);

        int count = 0;
        if (countMap.containsKey(word)) {
            count = countMap.get(word);
            count++;
        }
        count++;
        countMap.put(word, count);

        outputCollector.emit(new Values(word, count));
        outputCollector.ack(tuple);

    }

    @Override
    public void prepare(Map arg0, TopologyContext arg1, OutputCollector outputCollector) {
        this.outputCollector = outputCollector;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word", "count"));
    }


}
