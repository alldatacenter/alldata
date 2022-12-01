/*
 * Copyright 2017 ABSA Group Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package za.co.absa.spline.example.batch;

import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import za.co.absa.spline.agent.AgentConfig;
import za.co.absa.spline.harvester.SparkLineageInitializer;
import za.co.absa.spline.harvester.conf.SplineMode;

public class JavaExampleJob {

    public static void main(String[] args) {
        final SparkSession.Builder builder = SparkSession.builder();
        final SparkSession session = builder.appName("java example app").master("local[*]").getOrCreate();

        // Explicitly enable Spline lineage tracking
        // This step is optional - see https://github.com/AbsaOSS/spline-spark-agent#programmatic-initialization
        final AgentConfig splineConfig =
            AgentConfig.builder()
                .splineMode(SplineMode.REQUIRED)
                .build();

        SparkLineageInitializer.enableLineageTracking(session, splineConfig);

        // run a Spark job as usual
        session.read()
            .option("header", "true")
            .option("inferSchema", "true")
            .csv("data/input/batch/wikidata.csv")
            .as("source")
            .write()
            .mode(SaveMode.Overwrite)
            .csv("data/output/batch/java-sample.csv");
    }
}
