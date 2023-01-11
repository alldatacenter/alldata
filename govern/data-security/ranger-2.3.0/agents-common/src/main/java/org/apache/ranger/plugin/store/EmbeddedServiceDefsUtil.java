/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.plugin.store;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.hadoop.config.RangerAdminConfig;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.ranger.plugin.util.ServiceDefUtil;

/*
 * This utility class deals with service-defs embedded in ranger-plugins-common
 * library (hdfs/hbase/hive/knox/storm/..). If any of these service-defs
 * don't exist in the given service store, they will be created in the store
 * using the embedded definitions.
 *
 * init() method should be called from ServiceStore implementations to
 * initialize embedded service-defs.
 */
public class EmbeddedServiceDefsUtil {
	private static final Logger LOG = LoggerFactory.getLogger(EmbeddedServiceDefsUtil.class);


	// following servicedef list should be reviewed/updated whenever a new embedded service-def is added
	public static final String DEFAULT_BOOTSTRAP_SERVICEDEF_LIST = "tag,hdfs,hbase,hive,kms,knox,storm,yarn,kafka,solr,atlas,nifi,nifi-registry,sqoop,kylin,elasticsearch,presto,trino,ozone,kudu,schema-registry";
	private static final String PROPERTY_SUPPORTED_SERVICE_DEFS = "ranger.supportedcomponents";
	private Set<String> supportedServiceDefs;
	public static final String EMBEDDED_SERVICEDEF_TAG_NAME  = "tag";
	public static final String EMBEDDED_SERVICEDEF_HDFS_NAME  = "hdfs";
	public static final String EMBEDDED_SERVICEDEF_HBASE_NAME = "hbase";
	public static final String EMBEDDED_SERVICEDEF_HIVE_NAME  = "hive";
	public static final String EMBEDDED_SERVICEDEF_KMS_NAME   = "kms";
	public static final String EMBEDDED_SERVICEDEF_KNOX_NAME  = "knox";
	public static final String EMBEDDED_SERVICEDEF_STORM_NAME = "storm";
	public static final String EMBEDDED_SERVICEDEF_YARN_NAME  = "yarn";
	public static final String EMBEDDED_SERVICEDEF_KAFKA_NAME = "kafka";
	public static final String EMBEDDED_SERVICEDEF_SOLR_NAME  = "solr";
	public static final String EMBEDDED_SERVICEDEF_SCHEMA_REGISTRY_NAME  = "schema-registry";
	public static final String EMBEDDED_SERVICEDEF_NIFI_NAME  = "nifi";
	public static final String EMBEDDED_SERVICEDEF_NIFI_REGISTRY_NAME  = "nifi-registry";
	public static final String EMBEDDED_SERVICEDEF_ATLAS_NAME  = "atlas";
	public static final String EMBEDDED_SERVICEDEF_WASB_NAME  = "wasb";
	public static final String EMBEDDED_SERVICEDEF_SQOOP_NAME = "sqoop";
	public static final String EMBEDDED_SERVICEDEF_KYLIN_NAME  = "kylin";
	public static final String EMBEDDED_SERVICEDEF_ABFS_NAME  = "abfs";
	public static final String EMBEDDED_SERVICEDEF_ELASTICSEARCH_NAME = "elasticsearch";
	public static final String EMBEDDED_SERVICEDEF_PRESTO_NAME  = "presto";
	public static final String EMBEDDED_SERVICEDEF_TRINO_NAME  = "trino";
	public static final String EMBEDDED_SERVICEDEF_OZONE_NAME  = "ozone";
	public static final String EMBEDDED_SERVICEDEF_KUDU_NAME  = "kudu";

	public static final String PROPERTY_CREATE_EMBEDDED_SERVICE_DEFS = "ranger.service.store.create.embedded.service-defs";

	public static final String HDFS_IMPL_CLASS_NAME  = "org.apache.ranger.services.hdfs.RangerServiceHdfs";
	public static final String HBASE_IMPL_CLASS_NAME = "org.apache.ranger.services.hbase.RangerServiceHBase";
	public static final String HIVE_IMPL_CLASS_NAME  = "org.apache.ranger.services.hive.RangerServiceHive";
	public static final String KMS_IMPL_CLASS_NAME   = "org.apache.ranger.services.kms.RangerServiceKMS";
	public static final String KNOX_IMPL_CLASS_NAME  = "org.apache.ranger.services.knox.RangerServiceKnox";
	public static final String STORM_IMPL_CLASS_NAME = "org.apache.ranger.services.storm.RangerServiceStorm";
	public static final String YARN_IMPL_CLASS_NAME  = "org.apache.ranger.services.yarn.RangerServiceYarn";
	public static final String KAFKA_IMPL_CLASS_NAME = "org.apache.ranger.services.kafka.RangerServiceKafka";
	public static final String SOLR_IMPL_CLASS_NAME  = "org.apache.ranger.services.solr.RangerServiceSolr";
	public static final String SCHEMA_REGISTRY_IMPL_CLASS_NAME  = "org.apache.ranger.services.schemaregistry.RangerServiceSchemaRegistry";
	public static final String NIFI_IMPL_CLASS_NAME  = "org.apache.ranger.services.nifi.RangerServiceNiFi";
	public static final String ATLAS_IMPL_CLASS_NAME  = "org.apache.ranger.services.atlas.RangerServiceAtlas";
	public static final String PRESTO_IMPL_CLASS_NAME  = "org.apache.ranger.services.presto.RangerServicePresto";
	public static final String TRINO_IMPL_CLASS_NAME  = "org.apache.ranger.services.trino.RangerServiceTrino";
	public static final String OZONE_IMPL_CLASS_NAME  = "org.apache.ranger.services.ozone.RangerServiceOzone";
	public static final String KUDU_IMPL_CLASS_NAME  = "org.apache.ranger.services.kudu.RangerServiceKudu";

	private static EmbeddedServiceDefsUtil instance = new EmbeddedServiceDefsUtil();

	private boolean          createEmbeddedServiceDefs = true;
	private RangerServiceDef hdfsServiceDef;
	private RangerServiceDef hBaseServiceDef;
	private RangerServiceDef hiveServiceDef;
	private RangerServiceDef kmsServiceDef;
	private RangerServiceDef knoxServiceDef;
	private RangerServiceDef stormServiceDef;
	private RangerServiceDef yarnServiceDef;
	private RangerServiceDef kafkaServiceDef;
	private RangerServiceDef solrServiceDef;
	private RangerServiceDef schemaRegistryServiceDef;
	private RangerServiceDef nifiServiceDef;
	private RangerServiceDef nifiRegistryServiceDef;
	private RangerServiceDef atlasServiceDef;
	private RangerServiceDef wasbServiceDef;
	private RangerServiceDef sqoopServiceDef;
	private RangerServiceDef kylinServiceDef;
	private RangerServiceDef abfsServiceDef;
	private RangerServiceDef elasticsearchServiceDef;
	private RangerServiceDef prestoServiceDef;
	private RangerServiceDef trinoServiceDef;
	private RangerServiceDef ozoneServiceDef;
	private RangerServiceDef kuduServiceDef;

	private RangerServiceDef tagServiceDef;

	private final Gson              gsonBuilder;
	private final RangerAdminConfig config;

	/** Private constructor to restrict instantiation of this singleton utility class. */
	private EmbeddedServiceDefsUtil() {
		gsonBuilder = new GsonBuilder().setDateFormat("yyyyMMdd-HH:mm:ss.SSS-Z").setPrettyPrinting().create();
		config      = RangerAdminConfig.getInstance();
	}

	public static EmbeddedServiceDefsUtil instance() {
		return instance;
	}

	public void init(ServiceStore store) {
		LOG.info("==> EmbeddedServiceDefsUtil.init()");

		try {
			createEmbeddedServiceDefs = config.getBoolean(PROPERTY_CREATE_EMBEDDED_SERVICE_DEFS, true);

			supportedServiceDefs =getSupportedServiceDef();
			/*
			 * Maintaining the following service-def create-order is critical for the
			 * the legacy service-defs (HDFS/HBase/Hive/Knox/Storm) to be assigned IDs
			 * that were used in earlier version (0.4) */
			hdfsServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_HDFS_NAME);
			hBaseServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_HBASE_NAME);
			hiveServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_HIVE_NAME);
			kmsServiceDef   = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_KMS_NAME);
			knoxServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_KNOX_NAME);
			stormServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_STORM_NAME);
			yarnServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_YARN_NAME);
			kafkaServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_KAFKA_NAME);
			solrServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_SOLR_NAME);
			schemaRegistryServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_SCHEMA_REGISTRY_NAME);
			nifiServiceDef  = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_NIFI_NAME);
			nifiRegistryServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_NIFI_REGISTRY_NAME);
			atlasServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_ATLAS_NAME);

			tagServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_TAG_NAME);
			wasbServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_WASB_NAME);
			sqoopServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_SQOOP_NAME);
			kylinServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_KYLIN_NAME);
			abfsServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_ABFS_NAME);
			elasticsearchServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_ELASTICSEARCH_NAME);
			trinoServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_TRINO_NAME);
			prestoServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_PRESTO_NAME);
			ozoneServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_OZONE_NAME);
			kuduServiceDef = getOrCreateServiceDef(store, EMBEDDED_SERVICEDEF_KUDU_NAME);

			// Ensure that tag service def is updated with access types of all service defs
			store.updateTagServiceDefForAccessTypes();
		} catch(Throwable excp) {
			LOG.error("EmbeddedServiceDefsUtil.init(): failed", excp);
		}

		LOG.info("<== EmbeddedServiceDefsUtil.init()");
	}

	public long getHdfsServiceDefId() {
		return getId(hdfsServiceDef);
	}

	public long getHBaseServiceDefId() {
		return getId(hBaseServiceDef);
	}

	public long getHiveServiceDefId() {
		return getId(hiveServiceDef);
	}

	public long getKmsServiceDefId() {
		return getId(kmsServiceDef);
	}

	public long getKnoxServiceDefId() {
		return getId(knoxServiceDef);
	}

	public long getStormServiceDefId() {
		return getId(stormServiceDef);
	}

	public long getYarnServiceDefId() {
		return getId(yarnServiceDef);
	}
	
	public long getKafkaServiceDefId() {
		return getId(kafkaServiceDef);
	}

	public long getSolrServiceDefId() {
		return getId(solrServiceDef);
	}

	public long getSchemaRegistryServiceDefId() {
		return getId(schemaRegistryServiceDef);
	}

	public long getNiFiServiceDefId() {
		return getId(nifiServiceDef);
	}

	public long getNiFiRegistryServiceDefId() {
		return getId(nifiRegistryServiceDef);
	}

	public long getAtlasServiceDefId() {
        return getId(atlasServiceDef);
    }

	public long getSqoopServiceDefId() {
		return getId(sqoopServiceDef);
	}

	public long getKylinServiceDefId() {
		return getId(kylinServiceDef);
	}

	public long getElasticsearchServiceDefId() {
		return getId(elasticsearchServiceDef);
	}
	public long getTagServiceDefId() { return getId(tagServiceDef); }

	public long getWasbServiceDefId() { return getId(wasbServiceDef); }

	public long getAbfsServiceDefId() { return getId(abfsServiceDef); }

	public long getTrinoServiceDefId() { return getId(trinoServiceDef); }

	public long getPrestoServiceDefId() { return getId(prestoServiceDef); }

	public long getOzoneServiceDefId() { return getId(ozoneServiceDef); }

	public long getKuduServiceDefId() { return getId(kuduServiceDef); }

	public RangerServiceDef getEmbeddedServiceDef(String defType) throws Exception {
		RangerServiceDef serviceDef=null;
		if(StringUtils.isNotEmpty(defType)){
			serviceDef=loadEmbeddedServiceDef(defType);
		}
		return serviceDef;
	}

	public static boolean isRecursiveEnabled(final RangerServiceDef rangerServiceDef, final String resourceDefName) {
		boolean ret = false;
		List<RangerServiceDef.RangerResourceDef>  resourceDefs = rangerServiceDef.getResources();
		for(RangerServiceDef.RangerResourceDef resourceDef:resourceDefs) {
			if (resourceDefName.equals(resourceDef.getName())) {
				ret =  resourceDef.getRecursiveSupported();
				break;
			}
		}
		return ret;
	}

	private long getId(RangerServiceDef serviceDef) {
		return serviceDef == null || serviceDef.getId() == null ? -1 : serviceDef.getId().longValue();
	}

	private RangerServiceDef getOrCreateServiceDef(ServiceStore store, String serviceDefName) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> EmbeddedServiceDefsUtil.getOrCreateServiceDef(" + serviceDefName + ")");
		}

		RangerServiceDef ret = null;
		boolean createServiceDef = (CollectionUtils.isEmpty(supportedServiceDefs) || supportedServiceDefs.contains(serviceDefName));
		try {
			ret = store.getServiceDefByName(serviceDefName);
			if(ret == null && createEmbeddedServiceDefs && createServiceDef) {
				ret = ServiceDefUtil.normalize(loadEmbeddedServiceDef(serviceDefName));

				LOG.info("creating embedded service-def " + serviceDefName);
				if (ret.getId() != null) {
					store.setPopulateExistingBaseFields(true);
					try {
						ret = store.createServiceDef(ret);
					} finally {
						store.setPopulateExistingBaseFields(false);
					}
				} else {
					ret = store.createServiceDef(ret);
				}
				LOG.info("created embedded service-def " + serviceDefName);
			}
		} catch(Exception excp) {
			LOG.error("EmbeddedServiceDefsUtil.getOrCreateServiceDef(): failed to load/create serviceType " + serviceDefName, excp);
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== EmbeddedServiceDefsUtil.getOrCreateServiceDef(" + serviceDefName + "): " + ret);
		}

		return ret;
	}

	private RangerServiceDef loadEmbeddedServiceDef(String serviceType) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> EmbeddedServiceDefsUtil.loadEmbeddedServiceDef(" + serviceType + ")");
		}

		RangerServiceDef ret = null;
	
		String resource = "/service-defs/ranger-servicedef-" + serviceType + ".json";

		InputStream inStream = getClass().getResourceAsStream(resource);

		InputStreamReader reader = new InputStreamReader(inStream);

		ret = gsonBuilder.fromJson(reader, RangerServiceDef.class);

		//Set DEFAULT displayName if missing
		if (ret != null && StringUtils.isBlank(ret.getDisplayName())) {
		    ret.setDisplayName(ret.getName());
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> EmbeddedServiceDefsUtil.loadEmbeddedServiceDef(" + serviceType + ")");
		}

		return ret;
	}

	private Set<String> getSupportedServiceDef(){
		Set<String> supportedServiceDef =new HashSet<>();
		try{
			String ranger_supportedcomponents = config.get(PROPERTY_SUPPORTED_SERVICE_DEFS, DEFAULT_BOOTSTRAP_SERVICEDEF_LIST);
			if(StringUtils.isBlank(ranger_supportedcomponents) || "all".equalsIgnoreCase(ranger_supportedcomponents)){
				ranger_supportedcomponents=DEFAULT_BOOTSTRAP_SERVICEDEF_LIST;
			}
			String[] supportedComponents=ranger_supportedcomponents.split(",");
			if(supportedComponents!=null && supportedComponents.length>0){
				for(String element:supportedComponents){
					if(!StringUtils.isBlank(element)){
						element=element.toLowerCase();
						supportedServiceDef.add(element);
					}
				}
			}
		}catch(Exception ex){
			LOG.error("EmbeddedServiceDefsUtil.getSupportedServiceDef(): failed", ex);
		}
		return supportedServiceDef;
	}
}
