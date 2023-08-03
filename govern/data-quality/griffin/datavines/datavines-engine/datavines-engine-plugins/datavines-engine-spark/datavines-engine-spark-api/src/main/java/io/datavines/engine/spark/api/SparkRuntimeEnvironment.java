/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.engine.spark.api;

import io.datavines.engine.spark.api.dialect.HiveSqlDialect;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.jdbc.JdbcDialects;
import org.apache.spark.streaming.Seconds;
import org.apache.spark.streaming.StreamingContext;

import io.datavines.common.config.CheckResult;
import io.datavines.common.config.Config;
import io.datavines.engine.api.env.Execution;
import io.datavines.engine.api.env.RuntimeEnvironment;
import io.datavines.engine.spark.api.batch.SparkBatchExecution;

public class SparkRuntimeEnvironment implements RuntimeEnvironment {

    private static final String TYPE = "type";
    private static final String STREAM = "stream";
    private static final String BATCH = "batch";

    private SparkSession sparkSession;

    private StreamingContext streamingContext;

    private Config config = new Config();

    @Override
    public void setConfig(Config config) {
        if(config != null) {
            this.config = config;
        }
    }

    @Override
    public Config getConfig() {
        return this.config;
    }

    @Override
    public CheckResult checkConfig() {
        return new CheckResult(true, "");
    }

    @Override
    public void prepare() {
        sparkSession = SparkSession.builder().config(createSparkConf()).getOrCreate();
        JdbcDialects.registerDialect(new HiveSqlDialect());
        this.createStreamingContext();
    }

    private SparkConf createSparkConf() {
        SparkConf conf = new SparkConf();
        this.config.entrySet()
                .forEach(entry -> {
                    conf.set(entry.getKey(), String.valueOf(entry.getValue()));
        });
        conf.set("spark.sql.crossJoin.enabled","true");
        return conf;
    }

    private void createStreamingContext() {
        SparkConf conf = sparkSession.sparkContext().getConf();
        long duration = conf.getLong("spark.stream.batchDuration", 5);
        if (streamingContext == null) {
            streamingContext =
                    new StreamingContext(sparkSession.sparkContext(), Seconds.apply(duration));
        }
    }

    public SparkSession sparkSession() {
        return sparkSession;
    }

    public StreamingContext streamingContext() {
        return streamingContext;
    }

    @Override
    public Execution getExecution() {
        Execution execution = null;
        if (BATCH.equals(config.getString(TYPE).toLowerCase())) {
            execution = new SparkBatchExecution(this);
        }
        return execution;
    }
}
