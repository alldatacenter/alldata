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

import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.storm.spout.SpoutOutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseRichSpout;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Values;

/**
 * A Storm Spout which reads in words.txt + emits a word from it (sequentially)
 */
public class WordSpout extends BaseRichSpout {
    private final List<String> words;
    private SpoutOutputCollector collector;
    private int line = 0;

    public WordSpout() throws Exception {
        java.io.File inputFile = new java.io.File(WordSpout.class.getResource("../../../../../words.txt").toURI());
        words = IOUtils.readLines(new java.io.FileInputStream(inputFile));
    }

    @Override
    public void nextTuple() {
        if (line < words.size()) {
        	String lineVal = words.get(line++);
        	while (lineVal.startsWith("#") && line < words.size()) {
        		lineVal = words.get(line++);
        	}
        	if (lineVal != null) {
        		collector.emit(new Values(lineVal.trim()));
        	}
        }
    }

    @Override
    public void open(Map arg0, TopologyContext arg1, SpoutOutputCollector collector) {
        this.collector = collector;
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("word"));
    }


}
