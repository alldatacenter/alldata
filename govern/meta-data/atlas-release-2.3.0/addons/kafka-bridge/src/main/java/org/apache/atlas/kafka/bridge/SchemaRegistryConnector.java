/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.kafka.bridge;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class SchemaRegistryConnector {
    private static final String SCHEMA_KEY = "schema";
    private static final Logger LOG        = LoggerFactory.getLogger(SchemaRegistryConnector.class);

    static ArrayList<Integer> getVersionsKafkaSchemaRegistry(CloseableHttpClient httpClient, String subject) throws IOException {
        ArrayList<Integer> list = new ArrayList<>();
        JSONParser parser = new JSONParser();

        HttpGet getRequest = new HttpGet("http://" + KafkaBridge.KAFKA_SCHEMA_REGISTRY_HOSTNAME + "/subjects/" + subject + "/versions/");
        getRequest.addHeader("accept", "application/json");
        getRequest.addHeader("Content-Type", "application/vnd.schemaregistry.v1+json");

        try {
            CloseableHttpResponse response = httpClient.execute(getRequest);

            if (response.getStatusLine().getStatusCode() == 200) {
                //found corresponding Schema version in Registry
                try {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));
                    JSONArray output = (JSONArray) parser.parse(br.readLine());
                    int len = output.size();
                    for (int i = 0; i < len; i++) {
                        list.add(((Long) output.get(i)).intValue());
                    }

                    System.out.println("---Found following versions to schema: " + subject + " Versions: " + list.toString());
                    LOG.info("Found following versions to schema: {} Versions: {}", subject, list.toString());

                    EntityUtils.consumeQuietly(response.getEntity());
                    response.close(); // close response
                    return list;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("---Error reading versions to schema: " + subject + " in Kafka");
                    LOG.error("Error reading versions to schema: " + subject + " in Kafka: ", e.getMessage());
                }

            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                // did not find any schema to the topic
                System.out.println("---No schema versions found for schema: " + subject + " in Schema Registry");
                LOG.info("No schema versions found for schema: {} in Schema Registry", subject);
            } else {
                // no connection to schema registry
                System.out.println("---Cannot connect to schema registry");
                LOG.warn("Cannot connect to schema registry");
            }

            EntityUtils.consumeQuietly(response.getEntity());
            response.close();
        }
        catch(Exception e) {
            System.out.println("---Error getting versions to schema: " + subject + " from Kafka");
            LOG.error("Error getting versions to schema: " + subject + " from Kafka: ", e);
        }
        return list;
    }

    static String getSchemaFromKafkaSchemaRegistry(CloseableHttpClient httpClient, String subject, int version) throws IOException {
        HttpGet getRequest = new HttpGet("http://" + KafkaBridge.KAFKA_SCHEMA_REGISTRY_HOSTNAME + "/subjects/" + subject + "/versions/" + version);
        getRequest.addHeader("accept", "application/json");
        getRequest.addHeader("Content-Type", "application/vnd.schemaregistry.v1+json");
        JSONParser parser = new JSONParser();

        CloseableHttpResponse response = httpClient.execute(getRequest);

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK){
            //found corresponding Schema in Registry
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader((response.getEntity().getContent()), StandardCharsets.UTF_8));
                JSONObject output = (JSONObject) parser.parse(br.readLine());

                EntityUtils.consumeQuietly(response.getEntity());
                response.close();
                return output.get(SCHEMA_KEY).toString();
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("---Error reading versions to schema: " + subject + " in Kafka");
                LOG.error("Error reading versions to schema: " + subject + " in Kafka: ", e);
            }

        }

        else if (response.getStatusLine().getStatusCode() == 404) {
            // did not find any schema to the topic
            System.out.println("---Cannot find the corresponding schema to: " + subject + "in Kafka");
            LOG.info("Cannot find the corresponding schema to: {} in Kafka", subject);
        }

        else {
            // any other error when connecting to schema registry
            System.out.println("---Cannot connect to schema registry at: " + KafkaBridge.KAFKA_SCHEMA_REGISTRY_HOSTNAME);
            LOG.warn("Cannot connect to schema registry at: {}", KafkaBridge.KAFKA_SCHEMA_REGISTRY_HOSTNAME);
        }

        EntityUtils.consumeQuietly(response.getEntity());
        response.close();
        return null;
    }
}
