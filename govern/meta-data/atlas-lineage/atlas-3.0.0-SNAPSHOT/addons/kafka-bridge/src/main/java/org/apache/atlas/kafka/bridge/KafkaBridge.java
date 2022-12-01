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

import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.kafka.model.KafkaDataTypes;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.utils.AtlasConfigurationUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.KafkaUtils;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import org.apache.avro.Schema;
import java.io.IOException;

public class KafkaBridge {
    private static final Logger LOG                               = LoggerFactory.getLogger(KafkaBridge.class);
    private static final String KAFKA_SCHEMA_REGISTRY_ENV_VARIABLE= System.getenv("KAFKA_SCHEMA_REGISTRY");
    public static        String KAFKA_SCHEMA_REGISTRY_HOSTNAME    = "localhost";
    private static final int    EXIT_CODE_SUCCESS                 = 0;
    private static final int    EXIT_CODE_FAILED                  = 1;
    private static final String ATLAS_ENDPOINT                    = "atlas.rest.address";
    private static final String DEFAULT_ATLAS_URL                 = "http://localhost:21000/";
    private static final String CLUSTER_NAME_KEY                  = "atlas.cluster.name";
    private static final String KAFKA_METADATA_NAMESPACE          = "atlas.metadata.namespace";
    private static final String DEFAULT_CLUSTER_NAME              = "primary";
    private static final String ATTRIBUTE_QUALIFIED_NAME          = "qualifiedName";
    private static final String DESCRIPTION_ATTR                  = "description";
    private static final String PARTITION_COUNT                   = "partitionCount";
    private static final String REPLICATION_FACTOR                = "replicationFactor";
    private static final String NAME                              = "name";
    private static final String URI                               = "uri";
    private static final String CLUSTERNAME                       = "clusterName";
    private static final String TOPIC                             = "topic";
    private static final String FORMAT_KAKFA_TOPIC_QUALIFIED_NAME = "%s@%s";
    private static final String TYPE                              = "type";
    private static final String NAMESPACE                         = "namespace";
    private static final String FIELDS                            = "fields";
    private static final String AVRO_SCHEMA                       = "avroSchema";
    private static final String SCHEMA_VERSION_ID                 = "versionId";

    private static final String FORMAT_KAKFA_SCHEMA_QUALIFIED_NAME      = "%s@%s@%s";
    private static final String FORMAT_KAKFA_FIELD_QUALIFIED_NAME       = "%s@%s@%s@%s";

    private final List<String>  availableTopics;
    private final String        metadataNamespace;
    private final AtlasClientV2 atlasClientV2;
    private final KafkaUtils    kafkaUtils;
    private final CloseableHttpClient httpClient;

    public static void main(String[] args) {
        int           exitCode      = EXIT_CODE_FAILED;
        AtlasClientV2 atlasClientV2 = null;
        KafkaUtils    kafkaUtils    = null;
        CloseableHttpClient httpClient = null;

        System.out.print("\n################################\n");
        System.out.print("# Custom Kafka bridge #\n");
        System.out.print("################################\n\n");

        try {
            Options options = new Options();
            options.addOption("t","topic", true, "topic");
            options.addOption("f", "filename", true, "filename");

            CommandLineParser parser        = new BasicParser();
            CommandLine       cmd           = parser.parse(options, args);
            String            topicToImport = cmd.getOptionValue("t");
            String            fileToImport  = cmd.getOptionValue("f");
            Configuration     atlasConf     = ApplicationProperties.get();
            String[]          urls          = atlasConf.getStringArray(ATLAS_ENDPOINT);

            if (urls == null || urls.length == 0) {
                urls = new String[] { DEFAULT_ATLAS_URL };
            }


            if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
                String[] basicAuthUsernamePassword = AuthenticationUtil.getBasicAuthenticationInput();

                atlasClientV2 = new AtlasClientV2(urls, basicAuthUsernamePassword);
            } else {
                UserGroupInformation ugi = UserGroupInformation.getCurrentUser();

                atlasClientV2 = new AtlasClientV2(ugi, ugi.getShortUserName(), urls);
            }

            kafkaUtils = new KafkaUtils(atlasConf);

            KafkaBridge importer = new KafkaBridge(atlasConf, atlasClientV2, kafkaUtils);

            if(!KAFKA_SCHEMA_REGISTRY_ENV_VARIABLE.isEmpty()){
                KAFKA_SCHEMA_REGISTRY_HOSTNAME = KAFKA_SCHEMA_REGISTRY_ENV_VARIABLE;
            }

            if (StringUtils.isNotEmpty(fileToImport)) {
                File f = new File(fileToImport);

                if (f.exists() && f.canRead()) {
                    BufferedReader br   = new BufferedReader(new FileReader(f));
                    String         line;

                    while ((line = br.readLine()) != null) {
                        topicToImport = line.trim();

                        importer.importTopic(topicToImport);
                    }

                    exitCode = EXIT_CODE_SUCCESS;
                } else {
                    LOG.error("Failed to read the file");
                }
            } else {
                importer.importTopic(topicToImport);

                exitCode = EXIT_CODE_SUCCESS;
            }
        } catch(ParseException e) {
            LOG.error("Failed to parse arguments. Error: ", e.getMessage());

            printUsage();
        } catch(Exception e) {
            System.out.println("ImportKafkaEntities failed. Please check the log file for the detailed error message");

            e.printStackTrace();

            LOG.error("ImportKafkaEntities failed", e);
        } finally {
            if (atlasClientV2 != null) {
                atlasClientV2.close();
            }

            if (kafkaUtils != null) {
                kafkaUtils.close();
            }

            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (IOException e) {
                    LOG.error("Could not close http client: ", e);
                }
            }
        }

        System.out.print("\n\n");

        System.exit(exitCode);
    }

    public KafkaBridge(Configuration atlasConf, AtlasClientV2 atlasClientV2, KafkaUtils kafkaUtils) throws Exception {
        this.atlasClientV2     = atlasClientV2;
        this.metadataNamespace = getMetadataNamespace(atlasConf);
        this.kafkaUtils        = kafkaUtils;
        this.availableTopics   = this.kafkaUtils.listAllTopics();
        this.httpClient        = HttpClientBuilder.create().build();
    }

    private String getMetadataNamespace(Configuration config) {
        return AtlasConfigurationUtil.getRecentString(config, KAFKA_METADATA_NAMESPACE, getClusterName(config));
    }

    private String getClusterName(Configuration config) {
        return config.getString(CLUSTER_NAME_KEY, DEFAULT_CLUSTER_NAME);
    }

    public void importTopic(String topicToImport) throws Exception {
        List<String> topics = availableTopics;

        if (StringUtils.isNotEmpty(topicToImport)) {
            List<String> topics_subset = new ArrayList<>();

            for (String topic : topics) {
                if (Pattern.compile(topicToImport).matcher(topic).matches()) {
                    topics_subset.add(topic);
                }
            }

            topics = topics_subset;
        }

        if (CollectionUtils.isNotEmpty(topics)) {
            for (String topic : topics) {
                createOrUpdateTopic(topic);
            }
        }
    }

    @VisibleForTesting
    AtlasEntityWithExtInfo createOrUpdateTopic(String topic) throws Exception {
        String                 topicQualifiedName = getTopicQualifiedName(metadataNamespace, topic);
        AtlasEntityWithExtInfo topicEntity        = findEntityInAtlas(KafkaDataTypes.KAFKA_TOPIC.getName(),topicQualifiedName);

        System.out.print("\n"); // add a new line for each topic

        if (topicEntity == null) {
            System.out.println("Adding Kafka topic " + topic);
            LOG.info("Importing Kafka topic: {}", topicQualifiedName);

            AtlasEntity entity = getTopicEntity(topic, null);

            topicEntity = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
        } else {
            System.out.println("Updating Kafka topic "  + topic);
            LOG.info("Kafka topic {} already exists in Atlas. Updating it..", topicQualifiedName);

            AtlasEntity entity = getTopicEntity(topic, topicEntity.getEntity());

            topicEntity.setEntity(entity);

            topicEntity = updateEntityInAtlas(topicEntity);
        }

        return topicEntity;
    }

    @VisibleForTesting
    AtlasEntityWithExtInfo createOrUpdateSchema(String schema, String schemaName, String namespace, int version) throws Exception {
        String                 schemaQualifiedName = getSchemaQualifiedName(metadataNamespace, schemaName + "-value", "v" + version);
        AtlasEntityWithExtInfo schemaEntity        = findEntityInAtlas(KafkaDataTypes.AVRO_SCHEMA.getName(), schemaQualifiedName);

        if (schemaEntity == null) {
            System.out.println("---Adding Kafka schema " + schema);
            LOG.info("Importing Kafka schema: {}", schemaQualifiedName);

            AtlasEntity entity = getSchemaEntity(schema, schemaName, namespace, version, null);

            schemaEntity = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
        } else {
            System.out.println("---Updating Kafka schema "  + schema);
            LOG.info("Kafka schema {} already exists in Atlas. Updating it..", schemaQualifiedName);

            AtlasEntity entity = getSchemaEntity(schema, schemaName, namespace, version, schemaEntity.getEntity());

            schemaEntity.setEntity(entity);

            schemaEntity = updateEntityInAtlas(schemaEntity);
        }

        return schemaEntity;
    }

    @VisibleForTesting
    AtlasEntityWithExtInfo createOrUpdateField(Schema.Field field, String schemaName, String namespace, int version, String fullname) throws Exception {
        fullname = concatFullname(field.name(), fullname, "");
        String                 fieldQualifiedName = getFieldQualifiedName(metadataNamespace, fullname, schemaName + "-value", "v" + version);
        AtlasEntityWithExtInfo fieldEntity        = findEntityInAtlas(KafkaDataTypes.AVRO_FIELD.getName(), fieldQualifiedName);

        if (fieldEntity == null) {
            System.out.println("---Adding Avro field " + fullname);
            LOG.info("Importing Avro field: {}", fieldQualifiedName);

            AtlasEntity entity = getFieldEntity(field, schemaName, namespace, version ,null, fullname);

            fieldEntity = createEntityInAtlas(new AtlasEntityWithExtInfo(entity));
        } else {
            System.out.println("---Updating Avro field "  + fullname);
            LOG.info("Avro field {} already exists in Atlas. Updating it..", fieldQualifiedName);

            AtlasEntity entity = getFieldEntity(field, schemaName, namespace, version, fieldEntity.getEntity(), fullname);

            fieldEntity.setEntity(entity);

            fieldEntity = updateEntityInAtlas(fieldEntity);
        }

        return fieldEntity;
    }


    @VisibleForTesting
    AtlasEntity getTopicEntity(String topic, AtlasEntity topicEntity) throws Exception {
        final AtlasEntity ret;
        List<AtlasEntity> createdSchemas;

        if (topicEntity == null) {
            ret = new AtlasEntity(KafkaDataTypes.KAFKA_TOPIC.getName());
        } else {
            ret = topicEntity;
        }

        String qualifiedName = getTopicQualifiedName(metadataNamespace, topic);

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(CLUSTERNAME, metadataNamespace);
        ret.setAttribute(TOPIC, topic);
        ret.setAttribute(NAME,topic);
        ret.setAttribute(DESCRIPTION_ATTR, topic);
        ret.setAttribute(URI, topic);

        try {
            ret.setAttribute(PARTITION_COUNT, kafkaUtils.getPartitionCount(topic));
            ret.setAttribute(REPLICATION_FACTOR, kafkaUtils.getReplicationFactor(topic));
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Error while getting partition data for topic :" + topic, e);

            throw new Exception("Error while getting partition data for topic :" + topic, e);
        }

        createdSchemas = findOrCreateAtlasSchema(topic);

        if(createdSchemas.size() > 0) {
            ret.setAttribute(AVRO_SCHEMA, createdSchemas);
            ret.setRelationshipAttribute(AVRO_SCHEMA, createdSchemas);
        }

        return ret;
    }

    @VisibleForTesting
    AtlasEntity getSchemaEntity(String schema, String schemaName, String namespace, int version, AtlasEntity schemaEntity) throws Exception {
        final AtlasEntity ret;
        List<AtlasEntity> createdFields = new ArrayList<>();


        if (schemaEntity == null) {
            ret = new AtlasEntity(KafkaDataTypes.AVRO_SCHEMA.getName());
        } else {
            ret = schemaEntity;
        }

        Schema parsedSchema = new Schema.Parser().parse(schema);

        String qualifiedName = getSchemaQualifiedName(metadataNamespace, schemaName + "-value", "v" + version);

        if (namespace == null) {
            namespace = (parsedSchema.getNamespace() != null) ? parsedSchema.getNamespace() : KAFKA_METADATA_NAMESPACE;
        }

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(TYPE, parsedSchema.getType());
        ret.setAttribute(NAMESPACE, namespace);
        ret.setAttribute(NAME,parsedSchema.getName() + "(v" + version + ")");
        ret.setAttribute(SCHEMA_VERSION_ID, version);

        createdFields = createNestedFields(parsedSchema, schemaName, namespace, version, "");

        if(createdFields.size() > 0) {
            ret.setRelationshipAttribute(FIELDS, createdFields);
        }

        return ret;
    }

    List<AtlasEntity> createNestedFields(Schema parsedSchema, String schemaName, String namespace, int version, String fullname) throws Exception {
        List<AtlasEntity>  entityArray = new ArrayList<>();
        AtlasEntityWithExtInfo fieldInAtlas;
        JSONParser parser = new JSONParser();

        for (Schema.Field field:parsedSchema.getFields()) {

            if(field.schema().getType() == Schema.Type.ARRAY){
                System.out.println("ARRAY DETECTED");
                String subfields = ((JSONObject) parser.parse(field.schema().toString())).get("items").toString();
                Schema parsedSubSchema = new Schema.Parser().parse(subfields);

                fullname = concatFullname(field.name(), fullname, parsedSubSchema.getName());

                entityArray.addAll(createNestedFields(parsedSubSchema, schemaName, namespace, version, fullname));
            }

            else if(field.schema().getType() == Schema.Type.RECORD && !schemaName.equals(field.name())) {
                    System.out.println("NESTED RECORD DETECTED");
                    fullname = concatFullname(field.name(), fullname, "");
                    entityArray.addAll(createNestedFields(field.schema(), schemaName, namespace, version, fullname));
            }

            else{
                fieldInAtlas = createOrUpdateField(field, schemaName, namespace, version, fullname);

                entityArray.add(fieldInAtlas.getEntity());
            }
        }
        entityArray.sort((o1, o2) -> {
            if (o1.getAttribute(NAME) != null && o2.getAttribute(NAME) != null) {
                String str1 = o1.getAttribute(NAME).toString();
                String str2 = o2.getAttribute(NAME).toString();

                return str1.compareTo(str2);
            } else {
                return 0;
            }
        });

        return entityArray;
    }

    @VisibleForTesting
    AtlasEntity getFieldEntity(Schema.Field field, String schemaName, String namespace, int version, AtlasEntity fieldEntity, String fullname) throws Exception {
        AtlasEntity ret;

        if (fieldEntity == null) {
            ret = new AtlasEntity(KafkaDataTypes.AVRO_FIELD.getName());
        } else {
            ret = fieldEntity;
        }

        String qualifiedName = getFieldQualifiedName(metadataNamespace, fullname, schemaName + "-value", "v" + version);

        ret.setAttribute(ATTRIBUTE_QUALIFIED_NAME, qualifiedName);
        ret.setAttribute(NAME,fullname + "(v" + version + ")");
        //ret.setAttribute(field.schema().getType()); --> does not work, since type expects array<avro_type>. Instead setting Description
        ret.setAttribute(DESCRIPTION_ATTR, field.schema().getType());
        return ret;
    }

    @VisibleForTesting
    static String getTopicQualifiedName(String metadataNamespace, String topic) {
        return String.format(FORMAT_KAKFA_TOPIC_QUALIFIED_NAME, topic.toLowerCase(), metadataNamespace);
    }

    @VisibleForTesting
    static String getSchemaQualifiedName(String metadataNamespace, String schema, String version) {
        return String.format(FORMAT_KAKFA_SCHEMA_QUALIFIED_NAME, schema.toLowerCase(), version, metadataNamespace);
    }

    @VisibleForTesting
    static String getFieldQualifiedName(String metadataNamespace, String field, String schemaName, String version) {
        return String.format(FORMAT_KAKFA_FIELD_QUALIFIED_NAME , field.toLowerCase(), schemaName.toLowerCase(), version, metadataNamespace);
    }

    @VisibleForTesting
     AtlasEntityWithExtInfo findEntityInAtlas(String typeName, String qualifiedName) throws Exception {
        AtlasEntityWithExtInfo ret = null;

        try {
            ret = atlasClientV2.getEntityByAttribute(typeName, Collections.singletonMap(ATTRIBUTE_QUALIFIED_NAME, qualifiedName));
        }
        catch (Exception e){
            LOG.info("Exception on finding Atlas Entity: {}", e);
        }

        return ret;
    }

    @VisibleForTesting
    AtlasEntityWithExtInfo createEntityInAtlas(AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntityWithExtInfo  ret      = null;
        EntityMutationResponse  response = atlasClientV2.createEntity(entity);
        List<AtlasEntityHeader> entities = response.getCreatedEntities();
        if (CollectionUtils.isNotEmpty(entities)) {
            AtlasEntityWithExtInfo getByGuidResponse = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

            ret = getByGuidResponse;

            LOG.info("Created {} entity: name={}, guid={}", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
        }

        return ret;
    }

    @VisibleForTesting
    AtlasEntityWithExtInfo updateEntityInAtlas(AtlasEntityWithExtInfo entity) throws Exception {
        AtlasEntityWithExtInfo ret;
        EntityMutationResponse response = atlasClientV2.updateEntity(entity);

        if (response != null) {
            List<AtlasEntityHeader> entities = response.getUpdatedEntities();

            if (CollectionUtils.isNotEmpty(entities)) {
                AtlasEntityWithExtInfo getByGuidResponse = atlasClientV2.getEntityByGuid(entities.get(0).getGuid());

                ret = getByGuidResponse;

                LOG.info("Updated {} entity: name={}, guid={} ", ret.getEntity().getTypeName(), ret.getEntity().getAttribute(ATTRIBUTE_QUALIFIED_NAME), ret.getEntity().getGuid());
            } else {
                LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );

                ret = entity;
            }
        } else {
            LOG.info("Entity: name={} ", entity.toString() + " not updated as it is unchanged from what is in Atlas" );

            ret = entity;
        }

        return ret;
    }

    private static  void printUsage(){
        System.out.println("Usage 1: import-kafka.sh");
        System.out.println("Usage 2: import-kafka.sh [-t <topic regex> OR --topic <topic regex>]");
        System.out.println("Usage 3: import-kafka.sh [-f <filename>]" );
        System.out.println("   Format:");
        System.out.println("        topic1 OR topic1 regex");
        System.out.println("        topic2 OR topic2 regex");
        System.out.println("        topic3 OR topic3 regex");
    }


    private void clearRelationshipAttributes(AtlasEntityWithExtInfo entity) {
        if (entity != null) {
            clearRelationshipAttributes(entity.getEntity());

            if (entity.getReferredEntities() != null) {
                clearRelationshipAttributes(entity.getReferredEntities().values());
            }
        }
    }

    private void clearRelationshipAttributes(Collection<AtlasEntity> entities) {
        if (entities != null) {
            for (AtlasEntity entity : entities) {
                clearRelationshipAttributes(entity);
            }
        }
    }

    private void clearRelationshipAttributes(AtlasEntity entity) {
        if (entity != null && entity.getRelationshipAttributes() != null) {
            entity.getRelationshipAttributes().clear();
        }
    }

    private List<AtlasEntity> findOrCreateAtlasSchema(String schemaName) throws Exception {
        List<AtlasEntity> entities = new ArrayList<>();
        // Handling Schemas
        ArrayList<Integer> versions = SchemaRegistryConnector.getVersionsKafkaSchemaRegistry(httpClient,schemaName + "-value");

        for (int version:versions) {
            String kafkaSchema = SchemaRegistryConnector.getSchemaFromKafkaSchemaRegistry(httpClient, schemaName + "-value", version);

            if(kafkaSchema != null) {
                // Schema exists in Kafka Schema Registry
                System.out.println("---Found Schema " + schemaName + "-value in Kafka Schema Registry with Version " + version);
                LOG.info("Found Schema {}-value in Kafka Schema Registry with Version {}", schemaName, version);

                AtlasEntityWithExtInfo atlasSchemaEntity = findEntityInAtlas(KafkaDataTypes.AVRO_SCHEMA.getName(), getSchemaQualifiedName(metadataNamespace, schemaName  + "-value", "v" + version));

                if(atlasSchemaEntity != null) {
                    // Schema exists in Kafka Schema Registry AND in Atlas

                    System.out.println("---Found Entity avro_schema " + schemaName + " in Atlas");
                    LOG.info("Found Entity avro_schema {} in Atlas", schemaName);

                    AtlasEntityWithExtInfo createdSchema = createOrUpdateSchema(kafkaSchema, schemaName, null, version);

                    entities.add(createdSchema.getEntity());
                }
                else {
                    // Schema exists in Kafka Schema Registry but NOT in Atlas
                    System.out.println("---NOT Found Entity avro_schema " + schemaName + " in Atlas");
                    LOG.info("NOT Found Entity avro_schema {} in Atlas", schemaName);

                    AtlasEntityWithExtInfo createdSchema = createOrUpdateSchema(kafkaSchema, schemaName, null, version);

                    entities.add(createdSchema.getEntity());
                }
            }
        }

        return entities;
    }

    private String concatFullname(String fieldName,String fullname, String subSchemaName){
        if(fullname.isEmpty()){
            if(subSchemaName.isEmpty()) {
                fullname = fieldName;
            }
            else {
                fullname = fieldName + "." + subSchemaName;
            }

        }
        else{
            if(subSchemaName.isEmpty()) {
                fullname = fullname + "." + fieldName;
            }
            else {
                fullname = fullname + "." + subSchemaName + "." + fieldName;
            }
        }

        return fullname;
    }
}