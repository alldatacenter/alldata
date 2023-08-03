/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.patch.cliutil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.Arrays;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXAccessAudit;
import org.apache.ranger.entity.XXAccessAuditBase;
import org.apache.ranger.entity.XXAccessAuditV4;
import org.apache.ranger.entity.XXAccessAuditV5;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.solr.SolrAccessAuditsService;
import org.apache.ranger.audit.utils.InMemoryJAASConfiguration;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.PropertiesUtil;
import org.apache.ranger.util.CLIUtil;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.BinaryRequestWriter;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.Krb5HttpClientBuilder;
import org.apache.solr.client.solrj.impl.SolrHttpClientBuilder;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.security.auth.login.Configuration;

@Component
public class DbToSolrMigrationUtil extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(DbToSolrMigrationUtil.class);
	private final static String CHECK_FILE_NAME = "migration_check_file.txt";
	private final static Charset ENCODING = StandardCharsets.UTF_8;

	public static SolrClient solrClient = null;
	public final static String SOLR_URLS_PROP = "ranger.audit.solr.urls";
	public final static String SOLR_ZK_HOSTS = "ranger.audit.solr.zookeepers";
	public final static String SOLR_COLLECTION_NAME = "ranger.audit.solr.collection.name";
	public final static String PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG   = "java.security.auth.login.config";
	public final static String DEFAULT_COLLECTION_NAME = "ranger_audits";

	@Autowired
	RangerDaoManager daoManager;
	@Autowired
	SolrAccessAuditsService solrAccessAuditsService;

	public static void main(String[] args) throws Exception {
		logger.info("main()");
		logger.info("Note: If migrating to Secure Solr, make sure SolrClient JAAS Properites are configured in ranger-admin-site.xml");
		try {
			DbToSolrMigrationUtil loader = (DbToSolrMigrationUtil) CLIUtil
					.getBean(DbToSolrMigrationUtil.class);

			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting!!!");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		} finally {
			if (solrClient != null) {
				solrClient.close();
			}
		}
	}

	@Override
	public void init() throws Exception {
		logger.info("==> DbToSolrMigrationUtil.init() Start.");
		solrClient = createSolrClient();
		logger.info("<== DbToSolrMigrationUtil.init() End.");
	}

	@Override
	public void execLoad() {
		logger.info("==> DbToSolrMigrationUtil.execLoad() Start.");
		migrateAuditDbLogsToSolr();
		logger.info("<== DbToSolrMigrationUtil.execLoad() End.");
	}

	public void migrateAuditDbLogsToSolr() {
		System.out.println("Migration process is started..");
		long maxXXAccessAuditID = daoManager.getXXAccessAudit().getMaxIdOfXXAccessAudit();
		if(maxXXAccessAuditID==0){
			logger.info("Access Audit log does not exist.");
			System.out.println("Access Audit log does not exist in db.");
			return;
		}
		long maxMigratedID=0;
		try {
			maxMigratedID = readMigrationStatusFile(CHECK_FILE_NAME);
		} catch (IOException ex) {
			logger.error("Failed to read migration status from file " + CHECK_FILE_NAME, ex);
		}
		logger.info("ID of the last available audit log: "+ maxXXAccessAuditID);
		if(maxMigratedID > 0) {
			  logger.info("ID of the last migrated audit log: "+ maxMigratedID);
		}
		if(maxMigratedID>=maxXXAccessAuditID){
			logger.info("No more DB Audit logs to migrate. Last migrated audit log ID: " + maxMigratedID);
			System.out.println("No more DB Audit logs to migrate. Last migrated audit log ID: " + maxMigratedID);
			return;
		}
		String db_flavor=AppConstants.getLabelFor_DatabaseFlavor(RangerBizUtil.getDBFlavor());
		logger.info("DB flavor: " + db_flavor);
		List<String> columnList=daoManager.getXXAccessAudit().getColumnNames(db_flavor);
		int auditTableVersion=4;
		if(columnList!=null){
			if(columnList.contains("tags")){
				auditTableVersion=6;
			}else if(columnList.contains("seq_num") && columnList.contains("event_count") && columnList.contains("event_dur_ms")){
				auditTableVersion=5;
			}
		}
		logger.info("Columns Name:"+columnList);
		long maxRowsPerBatch=10000;
		//To ceil the actual division result i.e noOfBatches=maxXXAccessAuditID/maxRowsPerBatch
		long noOfBatches=((maxXXAccessAuditID-maxMigratedID)+maxRowsPerBatch-1)/maxRowsPerBatch;
		long rangeStart=maxMigratedID;
		long rangeEnd=maxXXAccessAuditID-maxMigratedID<=maxRowsPerBatch ? maxXXAccessAuditID : rangeStart+maxRowsPerBatch;
		long startTimeInMS=0;
		long timeTaken=0;
		long lastMigratedID=0;
		long totalMigratedLogs=0;
		for(long index=1;index<=noOfBatches;index++){
			logger.info("Batch "+ index+" of total "+noOfBatches);
			System.out.println("Processing batch "+ index+" of total "+noOfBatches);
			startTimeInMS=System.currentTimeMillis();
			//rangeStart and rangeEnd both exclusive, if we add +1 in maxRange
			if(auditTableVersion==4){
				List<XXAccessAuditV4> xXAccessAuditV4List=daoManager.getXXAccessAudit().getByIdRangeV4(rangeStart,rangeEnd+1);
				if(!CollectionUtils.isEmpty(xXAccessAuditV4List)){
					for(XXAccessAuditV4 xXAccessAudit:xXAccessAuditV4List){
						if(xXAccessAudit!=null){
							try {
								send2solr(xXAccessAudit);
								lastMigratedID=xXAccessAudit.getId();
								totalMigratedLogs++;
							} catch (Throwable e) {
								logger.error("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.", e);
								writeMigrationStatusFile(lastMigratedID,CHECK_FILE_NAME);
								logger.info("Stopping migration process!");
								System.out.println("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.");
								System.out.println("Migration process failed, Please refer ranger_db_patch.log file.");
								return;
							}
						}
					}
				}
			}else if(auditTableVersion==5){
				List<XXAccessAuditV5>  xXAccessAuditV5List=daoManager.getXXAccessAudit().getByIdRangeV5(rangeStart,rangeEnd+1);
				if(!CollectionUtils.isEmpty(xXAccessAuditV5List)){
					for(XXAccessAuditV5 xXAccessAudit:xXAccessAuditV5List){
						if(xXAccessAudit!=null){
							try {
								send2solr(xXAccessAudit);
								lastMigratedID=xXAccessAudit.getId();
								totalMigratedLogs++;
							} catch (Throwable e) {
								logger.error("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.", e);
								writeMigrationStatusFile(lastMigratedID,CHECK_FILE_NAME);
								logger.info("Stopping migration process!");
								System.out.println("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.");
								System.out.println("Migration process failed, Please refer ranger_db_patch.log file.");
								return;
							}
						}
					}
				}
			}
			else if(auditTableVersion==6){
				List<XXAccessAudit> xXAccessAuditV6List=daoManager.getXXAccessAudit().getByIdRangeV6(rangeStart,rangeEnd+1);
				if(!CollectionUtils.isEmpty(xXAccessAuditV6List)){
					for(XXAccessAudit xXAccessAudit:xXAccessAuditV6List){
						if(xXAccessAudit!=null){
							try {
								send2solr(xXAccessAudit);
								lastMigratedID=xXAccessAudit.getId();
								totalMigratedLogs++;
							} catch (Throwable e) {
								logger.error("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.", e);
								writeMigrationStatusFile(lastMigratedID,CHECK_FILE_NAME);
								logger.info("Stopping migration process!");
								System.out.println("Error while writing audit log id '"+xXAccessAudit.getId()+"' to Solr.");
								System.out.println("Migration process failed, Please refer ranger_db_patch.log file.");
								return;
							}
						}
					}
				}
			}
			timeTaken=(System.currentTimeMillis()-startTimeInMS);
			logger.info("Batch #" + index + ": time taken:"+timeTaken+" ms");
			if(rangeEnd<maxXXAccessAuditID){
				writeMigrationStatusFile(rangeEnd,CHECK_FILE_NAME);
			}else{
				writeMigrationStatusFile(maxXXAccessAuditID,CHECK_FILE_NAME);
			}
			rangeStart=rangeEnd;
			rangeEnd=rangeEnd+maxRowsPerBatch;
		}
		if(totalMigratedLogs>0){
			System.out.println("Total Number of Migrated Audit logs:"+totalMigratedLogs);
			logger.info("Total Number of Migrated Audit logs:"+totalMigratedLogs);
		}
		if(solrClient!=null){
			try {
				solrClient.close();
			} catch (IOException e) {
				logger.error("Error while closing solr connection", e);
			}finally{
				solrClient=null;
			}
		}
		System.out.println("Migration process finished!!");
	}

	public void send2solr(XXAccessAuditV4 xXAccessAudit) throws Throwable {
		SolrInputDocument document = new SolrInputDocument();
		toSolrDocument(xXAccessAudit,document);
		UpdateResponse response = solrClient.add(document);
		if (response.getStatus() != 0) {
			logger.info("Response=" + response.toString() + ", status= "
					+ response.getStatus() + ", event=" + xXAccessAudit.toString());
			throw new Exception("Failed to send audit event ID=" + xXAccessAudit.getId());
		}
	}

	public void send2solr(XXAccessAuditV5 xXAccessAudit) throws Throwable {
		SolrInputDocument document = new SolrInputDocument();
		toSolrDocument(xXAccessAudit,document);
		UpdateResponse response = solrClient.add(document);
		if (response.getStatus() != 0) {
			logger.info("Response=" + response.toString() + ", status= "
					+ response.getStatus() + ", event=" + xXAccessAudit.toString());
			throw new Exception("Failed to send audit event ID=" + xXAccessAudit.getId());
		}
	}

	public void send2solr(XXAccessAudit xXAccessAudit) throws Throwable {
		SolrInputDocument document = new SolrInputDocument();
		toSolrDocument(xXAccessAudit,document);
		UpdateResponse response = solrClient.add(document);
		if (response.getStatus() != 0) {
			logger.info("Response=" + response.toString() + ", status= "
					+ response.getStatus() + ", event=" + xXAccessAudit.toString());
			throw new Exception("Failed to send audit event ID=" + xXAccessAudit.getId());
		}
	}

	private void toSolrDocument(XXAccessAuditBase xXAccessAudit,  SolrInputDocument document) {
		// add v4 fields
		document.addField("id", xXAccessAudit.getId());
		document.addField("access", xXAccessAudit.getAccessType());
		document.addField("enforcer", xXAccessAudit.getAclEnforcer());
		document.addField("agent", xXAccessAudit.getAgentId());
		document.addField("repo", xXAccessAudit.getRepoName());
		document.addField("sess", xXAccessAudit.getSessionId());
		document.addField("reqUser", xXAccessAudit.getRequestUser());
		document.addField("reqData", xXAccessAudit.getRequestData());
		document.addField("resource", xXAccessAudit.getResourcePath());
		document.addField("cliIP", xXAccessAudit.getClientIP());
		document.addField("logType", "RangerAudit");
		document.addField("result", xXAccessAudit.getAccessResult());
		document.addField("policy", xXAccessAudit.getPolicyId());
		document.addField("repoType", xXAccessAudit.getRepoType());
		document.addField("resType", xXAccessAudit.getResourceType());
		document.addField("reason", xXAccessAudit.getResultReason());
		document.addField("action", xXAccessAudit.getAction());
		document.addField("evtTime", DateUtil.getLocalDateForUTCDate(xXAccessAudit.getEventTime()));
		SolrInputField idField = document.getField("id");
		boolean uidIsString = true;
		if( idField == null) {
			Object uid = null;
			if(uidIsString) {
				uid = UUID.randomUUID().toString();
			}
			document.setField("id", uid);
		}
	}

	private void toSolrDocument(XXAccessAuditV5 xXAccessAudit,  SolrInputDocument document) {
		toSolrDocument((XXAccessAuditBase)xXAccessAudit, document);
		// add v5 fields
		document.addField("seq_num", xXAccessAudit.getSequenceNumber());
		document.addField("event_count", xXAccessAudit.getEventCount());
		document.addField("event_dur_ms", xXAccessAudit.getEventDuration());
	}

	private void toSolrDocument(XXAccessAudit xXAccessAudit,SolrInputDocument document) {
		toSolrDocument((XXAccessAuditBase)xXAccessAudit, document);
		// add v6 fields
		document.addField("seq_num", xXAccessAudit.getSequenceNumber());
		document.addField("event_count", xXAccessAudit.getEventCount());
		document.addField("event_dur_ms", xXAccessAudit.getEventDuration());
		document.addField("tags", xXAccessAudit.getTags());
	}
	private Long readMigrationStatusFile(String aFileName) throws IOException {
		Long migratedDbID=0L;
		Path path = Paths.get(aFileName);
		if (Files.exists(path) && Files.isRegularFile(path)) {
			List<String> fileContents=Files.readAllLines(path, ENCODING);
			if(fileContents!=null && fileContents.size()>=1){
				String line=fileContents.get(fileContents.size()-1).trim();
				if(!StringUtil.isEmpty(line)){
					try{
						migratedDbID=Long.parseLong(line);
					}catch(Exception ex){
					}
				}
			}
		}
	   return migratedDbID;
	}

	private void writeMigrationStatusFile(Long DbID, String aFileName) {
		try{
			Path path = Paths.get(aFileName);
			List<String> fileContents=new ArrayList<String>();
			fileContents.add(String.valueOf(DbID));
			Files.write(path, fileContents, ENCODING);
		}catch(IOException ex){
			logger.error("Failed to update migration status to file " + CHECK_FILE_NAME, ex);
		}catch(Exception ex){
			logger.error("Error while updating migration status to file " + CHECK_FILE_NAME, ex);
		}
	}
	@Override
	public void printStats() {
	}

	private SolrClient createSolrClient() throws Exception {
		SolrClient solrClient = null;

		registerSolrClientJAAS();
		String zkHosts = PropertiesUtil
				.getProperty(SOLR_ZK_HOSTS);
		if (zkHosts == null) {
			zkHosts = PropertiesUtil
					.getProperty("ranger.audit.solr.zookeeper");
		}
		if (zkHosts == null) {
			zkHosts = PropertiesUtil
					.getProperty("ranger.solr.zookeeper");
		}

		String solrURL = PropertiesUtil
				.getProperty(SOLR_URLS_PROP);
		if (solrURL == null) {
			// Try with url
			solrURL = PropertiesUtil
					.getProperty("ranger.audit.solr.url");
		}
		if (solrURL == null) {
			// Let's try older property name
			solrURL = PropertiesUtil
					.getProperty("ranger.solr.url");
		}

		if (zkHosts != null && !"".equals(zkHosts.trim())
				&& !"none".equalsIgnoreCase(zkHosts.trim())) {
			zkHosts = zkHosts.trim();
			String collectionName = PropertiesUtil
					.getProperty(SOLR_COLLECTION_NAME);
			if (collectionName == null
					|| "none".equalsIgnoreCase(collectionName)) {
				collectionName = DEFAULT_COLLECTION_NAME;
			}

			logger.info("Solr zkHosts=" + zkHosts
					+ ", collectionName=" + collectionName);

			try {
				// Instantiate
				Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
				SolrHttpClientBuilder kb = krbBuild.getBuilder();
				HttpClientUtil.setHttpClientBuilder(kb);
				final List<String> zkhosts = new ArrayList<String>(Arrays.asList(zkHosts.split(",")));
				CloudSolrClient solrCloudClient = new CloudSolrClient.Builder(zkhosts, null).build();
				solrCloudClient
						.setDefaultCollection(collectionName);
				return solrCloudClient;
			} catch (Exception e) {
				logger.error(
						"Can't connect to Solr server. ZooKeepers="
								+ zkHosts + ", collection="
								+ collectionName, e);
				throw e;
			}
		} else {
			if (solrURL == null || solrURL.isEmpty()
					|| "none".equalsIgnoreCase(solrURL)) {
				logger.error("Solr ZKHosts and URL for Audit are empty. Please set property "
						+ SOLR_ZK_HOSTS
						+ " or "
						+ SOLR_URLS_PROP);
			} else {
				try {
					Krb5HttpClientBuilder krbBuild = new Krb5HttpClientBuilder();
					SolrHttpClientBuilder kb = krbBuild.getBuilder();
					HttpClientUtil.setHttpClientBuilder(kb);
					HttpSolrClient.Builder builder = new HttpSolrClient.Builder();
					builder.withBaseSolrUrl(solrURL);
					builder.allowCompression(true);
					builder.withConnectionTimeout(1000);
					HttpSolrClient httpSolrClient = builder.build();
					httpSolrClient.setRequestWriter(new BinaryRequestWriter());
					solrClient = httpSolrClient;

					} catch (Exception e) {
					logger.error(
							"Can't connect to Solr server. URL="
									+ solrURL, e);
					throw e;
				}
			}
		}
		return solrClient;
	}

	private void registerSolrClientJAAS() {
		logger.info("==> createSolrClient.registerSolrClientJAAS()" );
		Properties  props = PropertiesUtil.getProps();
		try {
			 // SolrJ requires "java.security.auth.login.config"  property to be set to identify itself that it is kerberized. So using a dummy property for it
			 // Acutal solrclient JAAS configs are read from the ranger-admin-site.xml in ranger admin config folder and set by InMemoryJAASConfiguration
			 // Refer InMemoryJAASConfiguration doc for JAAS Configuration
			 if ( System.getProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG) == null ) {
				 System.setProperty(PROP_JAVA_SECURITY_AUTH_LOGIN_CONFIG, "/dev/null");
			 }
			 logger.info("Loading SolrClient JAAS config from Ranger audit config if present...");

			 Configuration conf = InMemoryJAASConfiguration.init(props);

			 if (conf != null) {
				 Configuration.setConfiguration(conf);
			 }
		} catch (Exception e) {
			logger.error("ERROR: Unable to load SolrClient JAAS config from ranger admin config file. Audit migration to Secure Solr will fail...",e);
		}
		logger.info("<==createSolrClient.registerSolrClientJAAS()" );
	}
}
