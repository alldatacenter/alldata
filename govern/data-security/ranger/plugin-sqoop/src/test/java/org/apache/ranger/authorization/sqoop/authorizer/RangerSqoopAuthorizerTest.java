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

package org.apache.ranger.authorization.sqoop.authorizer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.sqoop.common.SqoopException;
import org.apache.sqoop.core.ConfigurationConstants;
import org.apache.sqoop.core.SqoopConfiguration;
import org.apache.sqoop.model.MJob;
import org.apache.sqoop.model.MLink;
import org.apache.sqoop.repository.Repository;
import org.apache.sqoop.repository.RepositoryManager;
import org.apache.sqoop.security.AuthorizationManager;
import org.apache.sqoop.security.authorization.AuthorizationEngine;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
*
* Here we plug the Ranger RangerSqoopAuthorizer into Sqoop.
*
* A custom RangerAdminClient is plugged into Ranger in turn, which loads security policies from a local file.
* These policies were generated in the Ranger Admin UI for a sqoop service called "sqoopTest":
*
* a) A user "sqoop" can do all permissions(contains the "read" and "write") on all connectors, any link and any job;
* b) A user "zhangqiang" can do a "read" on the connector "kafka-connector",
* and "zhangqiang" is the creator of any job and any link by mock;
* c) A user "yuwen" can do "read" and "write" on the connector "oracle-jdbc-connector" and "hdfs-connector";
* d) A user "yuwen" can do "read" and "write" on the link "oracle-link" and "hdfs-link";
* e) A user "yuwen" can do "read" and "write" on the job "oracle2hdfs-job";
*
*/
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RangerSqoopAuthorizerTest {
	private static final List<String> allConnectors = new ArrayList<>();

	private static final String SQOOP = "sqoop";

	private static final String ZHANGQIANG = "zhangqiang";

	private static final String YUWEN = "yuwen";

	private static final String ORACLE_JDBC_CONNECTOR = "oracle-jdbc-connector";

	private static final String SFTP_CONNECTOR = "sftp-connector";

	private static final String KAFKA_CONNECTOR = "kafka-connector";

	private static final String KITE_CONNECTOR = "kite-connector";

	private static final String FTP_CONNECTOR = "ftp-connector";

	private static final String HDFS_CONNECTOR = "hdfs-connector";

	private static final String GENERIC_JDBC_CONNECTOR = "generic-jdbc-connector";

	private static final String ORACLE_LINK = "oracle-link";

	private static final String HDFS_LINK = "hdfs-link";

	private static final String ORACLE2HDFS_JOB = "oracle2hdfs-job";

	@BeforeClass
	public static void setup() throws Exception {
		// init sqoop all connectors
		addAllConnectors();

		// init sqoop to enable ranger authentication
		initSqoopAuth();
	}

	@AfterClass
	public static void cleanup() throws Exception {
		// do nothing
	}

	/**
	 * Help function: init sqoop all connectors
	 */
	private static void addAllConnectors() {
		allConnectors.add(ORACLE_JDBC_CONNECTOR);
		allConnectors.add(SFTP_CONNECTOR);
		allConnectors.add(KAFKA_CONNECTOR);
		allConnectors.add(KITE_CONNECTOR);
		allConnectors.add(FTP_CONNECTOR);
		allConnectors.add(HDFS_CONNECTOR);
		allConnectors.add(GENERIC_JDBC_CONNECTOR);
	}

	/**
	 * Help function: init sqoop to enable ranger authentication
	 */
	private static void initSqoopAuth() throws IOException, ClassNotFoundException, IllegalAccessException,
			InstantiationException {
		// init sqoop configruation
		String basedir = System.getProperty("basedir");
		if (basedir == null) {
			basedir = new File(".").getCanonicalPath();
		}
		String sqoopConfigDirPath = basedir + "/src/test/resources/";
		System.setProperty(ConfigurationConstants.SYSPROP_CONFIG_DIR, sqoopConfigDirPath);
		SqoopConfiguration.getInstance().initialize();

		// init sqoop authorization
		AuthorizationManager.getInstance().initialize();

		// mock sqoop class for authentication
		RepositoryManager repositoryManager = mock(RepositoryManager.class);
		RepositoryManager.setInstance(repositoryManager);
		Repository repository = mock(Repository.class);
		when(repositoryManager.getRepository()).thenReturn(repository);

		MLink link = mock(MLink.class);
		when(repository.findLink(anyString())).thenReturn(link);
		MJob job = mock(MJob.class);
		when(repository.findJob(anyString())).thenReturn(job);

		// mock user "zhangqiang" as the creator of any link and any job
		when(link.getCreationUser()).thenReturn(ZHANGQIANG);
		when(job.getCreationUser()).thenReturn(ZHANGQIANG);
	}

	/**
	 * Help function: get random link name
	 */
	private String getRandomLinkName() {
		return RandomStringUtils.randomAlphanumeric(10) + "-link";
	}

	/**
	 * Help function: get random job name
	 */
	private String getRandomJobName() {
		return RandomStringUtils.randomAlphanumeric(10) + "-job";
	}

	// No.1 readConnector test start
	/**
	 * sqoop read all connectors success
	 */
	@Test
	public void readConnectorAllWithAllPermissions() {
		String user = SQOOP;
		for (String connector : allConnectors) {
			AuthorizationEngine.readConnector(user, connector);
		}
	}

	/**
	 * zhangqiang read kafka-connector success
	 */
	@Test
	public void readConnectorKafkaWithReadPermission() {
		String user = ZHANGQIANG;
		String connector = KAFKA_CONNECTOR;
		AuthorizationEngine.readConnector(user, connector);
	}

	/**
	 * zhangqiang read hdfs-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void readConnectorHdfsWithoutPermission() {
		String user = ZHANGQIANG;
		String connector = HDFS_CONNECTOR;
		AuthorizationEngine.readConnector(user, connector);
	}

	/**
	 * yuwen read oracle-jdbc-connector success
	 */
	@Test
	public void readConnectorOracleJdbcWithAllPermissions() {
		String user = YUWEN;
		String connector = ORACLE_JDBC_CONNECTOR;
		AuthorizationEngine.readConnector(user, connector);
	}

	/**
	 * yuwen read hdfs-connector success
	 */
	@Test
	public void readConnectorHdfsWithAllPermissions() {
		String user = YUWEN;
		String connector = HDFS_CONNECTOR;
		AuthorizationEngine.readConnector(user, connector);
	}

	/**
	 * yuwen read kafka-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void readConnectorKafkaWithoutPermission() {
		String user = YUWEN;
		String connector = KAFKA_CONNECTOR;
		AuthorizationEngine.readConnector(user, connector);
	}

	// No.1 readConnector test end

	// No.2 readLink test start
	/**
	 * sqoop read any link success
	 */
	@Test
	public void readLinkAnyWithAllPermissions() {
		String user = SQOOP;
		String link = getRandomLinkName();
		AuthorizationEngine.readLink(user, link);
	}

	/**
	 * zhangqiang read any link success
	 */
	@Test
	public void readLinkAnyAsCreater() {
		String user = ZHANGQIANG;
		String link = getRandomLinkName();
		AuthorizationEngine.readLink(user, link);
	}

	/**
	 * yuwen read oracle-link success
	 */
	@Test
	public void readLinkOracleWithReadPermission() {
		String user = YUWEN;
		String link = ORACLE_LINK;
		AuthorizationEngine.readLink(user, link);
	}

	/**
	 * yuwen read hdfs-link success
	 */
	@Test
	public void readLinkHdfsWithReadPermission() {
		String user = YUWEN;
		String link = HDFS_LINK;
		AuthorizationEngine.readLink(user, link);
	}

	/**
	 * yuwen read any link failed
	 */
	@Test(expected = SqoopException.class)
	public void readLinkAnyWithoutPermission() {
		String user = YUWEN;
		String link = getRandomLinkName();
		AuthorizationEngine.readLink(user, link);
	}

	// No.2 readLink test end

	// No.3 createLink test start
	/**
	 * sqoop create link by all connectors success
	 */
	@Test
	public void createLinkByAllConnectorsWithAllPermissions() {
		String user = SQOOP;
		for (String connector : allConnectors) {
			AuthorizationEngine.createLink(user, connector);
		}
	}

	/**
	 * zhangqiang create link by kafka-connector success
	 */
	@Test
	public void createLinkByKafkaConnectorWithReadPermission() {
		String user = ZHANGQIANG;
		String connector = KAFKA_CONNECTOR;
		AuthorizationEngine.createLink(user, connector);
	}

	/**
	 * zhangqiang create link by hdfs-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void createLinkByHdfsConnectorWithoutPermission() {
		String user = ZHANGQIANG;
		String connector = HDFS_CONNECTOR;
		AuthorizationEngine.createLink(user, connector);
	}

	/**
	 * yuwen create link by oracle-jdbc-connector success
	 */
	@Test
	public void createLinkByOracleJdbcConnectorWithReadPermission() {
		String user = YUWEN;
		String connector = ORACLE_JDBC_CONNECTOR;
		AuthorizationEngine.createLink(user, connector);
	}

	/**
	 * yuwen create link by hdfs-connector success
	 */
	@Test
	public void createLinkByHdfsConnectorWithReadPermission() {
		String user = YUWEN;
		String connector = HDFS_CONNECTOR;
		AuthorizationEngine.createLink(user, connector);
	}

	/**
	 * yuwen create link by kafka-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void createLinkByKafkaConnectorWithoutPermission() {
		String user = YUWEN;
		String connector = KAFKA_CONNECTOR;
		AuthorizationEngine.createLink(user, connector);
	}

	// No.3 createLink test end

	// No.4 updateLink test start
	/**
	 * sqoop update any link created by all connectors success
	 */
	@Test
	public void updateLinkAnyByAllConnectorsWithAllPermissions() {
		String user = SQOOP;
		for (String connector : allConnectors) {
			String link = getRandomLinkName();
			AuthorizationEngine.updateLink(user, connector, link);
		}
	}

	/**
	 * zhangqiang update any link created by kafka-connector success
	 */
	@Test
	public void updateLinkAnyByKafkaConnectorAsCreater() {
		String user = ZHANGQIANG;
		String connector = KAFKA_CONNECTOR;
		String link = getRandomLinkName();
		AuthorizationEngine.updateLink(user, connector, link);
	}

	/**
	 * zhangqiang update any link created by hdfs-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void updateLinkAnyByHdfsConnectorWithoutPermission() {
		String user = ZHANGQIANG;
		String connector = HDFS_CONNECTOR;
		String link = getRandomLinkName();
		AuthorizationEngine.updateLink(user, connector, link);
	}

	/**
	 * yuwen update link created by oracle-jdbc-connector success
	 */
	@Test
	public void updateLinkByOracleJdbcConnectorWithWritePermission() {
		String user = YUWEN;
		String connector = ORACLE_JDBC_CONNECTOR;
		String link = ORACLE_LINK;
		AuthorizationEngine.updateLink(user, connector, link);
	}

	/**
	 * yuwen update link created by hdfs-connector success
	 */
	@Test
	public void updateLinkByHdfsConnectorWithReadPermission() {
		String user = YUWEN;
		String connector = HDFS_CONNECTOR;
		String link = HDFS_LINK;
		AuthorizationEngine.updateLink(user, connector, link);
	}

	/**
	 * yuwen update link created by kafka-connector failed
	 */
	@Test(expected = SqoopException.class)
	public void updateLinkByKafkaConnectorWithoutPermission() {
		String user = YUWEN;
		String connector = KAFKA_CONNECTOR;
		String link = getRandomLinkName();
		AuthorizationEngine.updateLink(user, connector, link);
	}

	// No.4 updateLink test end

	// No.5 deleteLink test start
	/**
	 * sqoop delete any link success
	 */
	@Test
	public void deleteLinkAnyWithAllPermissions() {
		String user = SQOOP;
		String link = getRandomLinkName();
		AuthorizationEngine.deleteLink(user, link);
	}

	/**
	 * zhangqiang delete any link success
	 */
	@Test
	public void deleteLinkAnyAsCreater() {
		String user = ZHANGQIANG;
		String link = getRandomLinkName();
		AuthorizationEngine.deleteLink(user, link);
	}

	/**
	 * yuwen delete oracle-link success
	 */
	@Test
	public void deleteLinkOracleWithWritePermission() {
		String user = YUWEN;
		String link = ORACLE_LINK;
		AuthorizationEngine.deleteLink(user, link);
	}

	/**
	 * yuwen delete hdfs-link success
	 */
	@Test
	public void deleteLinkHdfsWithWritePermission() {
		String user = YUWEN;
		String link = HDFS_LINK;
		AuthorizationEngine.deleteLink(user, link);
	}

	/**
	 * yuwen delete any link failed
	 */
	@Test(expected = SqoopException.class)
	public void deleteLinkAnyWithoutPermission() {
		String user = YUWEN;
		String link = getRandomLinkName();
		AuthorizationEngine.deleteLink(user, link);
	}

	// No.5 deleteLink test end

	// No.6 enableDisableLink test start
	/**
	 * sqoop enable disable any link success
	 */
	@Test
	public void enableDisableLinkAnyWithAllPermissions() {
		String user = SQOOP;
		String link = getRandomLinkName();
		AuthorizationEngine.enableDisableLink(user, link);
	}

	/**
	 * zhangqiang enable disable any link success
	 */
	@Test
	public void enableDisableLinkAnyAsCreater() {
		String user = ZHANGQIANG;
		String link = getRandomLinkName();
		AuthorizationEngine.enableDisableLink(user, link);
	}

	/**
	 * yuwen enable disable oracle-link success
	 */
	@Test
	public void enableDisableLinkOracleWithWritePermission() {
		String user = YUWEN;
		String link = ORACLE_LINK;
		AuthorizationEngine.enableDisableLink(user, link);
	}

	/**
	 * yuwen enable disable hdfs-link success
	 */
	@Test
	public void enableDisableLinkHdfsWithWritePermission() {
		String user = YUWEN;
		String link = HDFS_LINK;
		AuthorizationEngine.enableDisableLink(user, link);
	}

	/**
	 * yuwen enable disable any link failed
	 */
	@Test(expected = SqoopException.class)
	public void enableDisableLinkAnyWithoutPermission() {
		String user = YUWEN;
		String link = getRandomLinkName();
		AuthorizationEngine.enableDisableLink(user, link);
	}

	// No.6 enableDisableLink test end

	// No.7 readJob test start
	/**
	 * sqoop read any job success
	 */
	@Test
	public void readJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.readJob(user, job);
	}

	/**
	 * zhangqiang read any job success
	 */
	@Test
	public void readJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.readJob(user, job);
	}

	/**
	 * yuwen read oracle2hdfs-job success
	 */
	@Test
	public void readJobOracle2HdfsWithReadPermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.readJob(user, job);
	}

	/**
	 * yuwen read any job failed
	 */
	@Test(expected = SqoopException.class)
	public void readJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.readJob(user, job);
	}

	// No.7 readJob test end

	// No.8 createJob test start
	/**
	 * sqoop create job by any two links success
	 */
	@Test
	public void createJobByAnyTwoLinksWithAllPermissions() {
		String user = SQOOP;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		AuthorizationEngine.createJob(user, link1, link2);
	}

	/**
	 * zhangqiang create job by any two links success
	 */
	@Test
	public void createJobByAnyTwoLinksAsCreater() {
		String user = ZHANGQIANG;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		AuthorizationEngine.createJob(user, link1, link2);
	}

	/**
	 * yuwen create job from oracle-link to hdfs-link success
	 */
	@Test
	public void createJobFromOracle2HdfsLinkWithReadPermission() {
		String user = YUWEN;
		String link1 = ORACLE_LINK;
		String link2 = HDFS_LINK;
		AuthorizationEngine.createJob(user, link1, link2);
	}

	/**
	 * yuwen create job from oracle-link to any link failed
	 */
	@Test(expected = SqoopException.class)
	public void createJobFromOracle2AnyLinkWithoutPermission() {
		String user = YUWEN;
		String link1 = ORACLE_LINK;
		String link2 = getRandomLinkName();
		AuthorizationEngine.createJob(user, link1, link2);
	}

	/**
	 * yuwen create job by any two links failed
	 */
	@Test(expected = SqoopException.class)
	public void createJobByAnyTwoLinksWithoutPermission() {
		String user = YUWEN;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		AuthorizationEngine.createJob(user, link1, link2);
	}

	// No.8 createJob test end

	// No.9 updateJob test start
	/**
	 * sqoop update any job created by any two links success
	 */
	@Test
	public void updateJobAnyByAnyTwoLinksWithAllPermissions() {
		String user = SQOOP;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		String job = getRandomJobName();
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	/**
	 * zhangqiang update any job created by any two links success
	 */
	@Test
	public void updateJobAnyByAnyTwoLinksAsCreater() {
		String user = ZHANGQIANG;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		String job = getRandomJobName();
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	/**
	 * yuwen update oracle2hdfs-job created from oracle-link to hdfs-link
	 * success
	 */
	@Test
	public void updateJobOracle2HdfsByTwoLinksWithWritePermission() {
		String user = YUWEN;
		String link1 = ORACLE_LINK;
		String link2 = HDFS_LINK;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	/**
	 * yuwen update oracle2hdfs-job created from new_oracle-link to hdfs-link
	 * failed
	 */
	@Test(expected = SqoopException.class)
	public void updateJobOracle2HdfsByTwoLinksWithoutPermission() {
		String user = YUWEN;
		String link1 = "new_" + ORACLE_LINK;
		String link2 = HDFS_LINK;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	/**
	 * yuwen update any job created from oracle-link to hdfs-link failed
	 */
	@Test(expected = SqoopException.class)
	public void updateJobAnyByTwoLinksWithoutPermission() {
		String user = YUWEN;
		String link1 = ORACLE_LINK;
		String link2 = HDFS_LINK;
		String job = getRandomJobName();
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	/**
	 * yuwen update any job created from oracle-link to hdfs-link failed
	 */
	@Test(expected = SqoopException.class)
	public void updateJobAnyByAnyLinksWithoutPermission() {
		String user = YUWEN;
		String link1 = getRandomLinkName();
		String link2 = getRandomLinkName();
		String job = getRandomJobName();
		AuthorizationEngine.updateJob(user, link1, link2, job);
	}

	// No.9 updateJob test end

	// No.10 deleteJob test start
	/**
	 * sqoop delete any job success
	 */
	@Test
	public void deleteJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.deleteJob(user, job);
	}

	/**
	 * zhangqiang delete any job success
	 */
	@Test
	public void deleteJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.deleteJob(user, job);
	}

	/**
	 * yuwen delete oracle2hdfs-job success
	 */
	@Test
	public void deleteJobOracle2HdfsWithWritePermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.deleteJob(user, job);
	}

	/**
	 * yuwen delete any job failed
	 */
	@Test(expected = SqoopException.class)
	public void deleteJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.deleteJob(user, job);
	}

	// No.10 deleteJob test end

	// No.11 enableDisableJob test start
	/**
	 * sqoop enable disable any job success
	 */
	@Test
	public void enableDisableJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.enableDisableJob(user, job);
	}

	/**
	 * zhangqiang enable disable any job success
	 */
	@Test
	public void enableDisableJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.enableDisableJob(user, job);
	}

	/**
	 * yuwen enable disable oracle2hdfs-job success
	 */
	@Test
	public void enableDisableJobOracle2HdfsWithWritePermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.enableDisableJob(user, job);
	}

	/**
	 * yuwen enable disable any job failed
	 */
	@Test(expected = SqoopException.class)
	public void enableDisableJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.enableDisableJob(user, job);
	}

	// No.11 enableDisableJob test end

	// No.12 startJob test start
	/**
	 * sqoop start any job success
	 */
	@Test
	public void startJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.startJob(user, job);
	}

	/**
	 * zhangqiang start any job success
	 */
	@Test
	public void startJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.startJob(user, job);
	}

	/**
	 * yuwen start oracle2hdfs-job success
	 */
	@Test
	public void startJobOracle2HdfsWithWritePermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.startJob(user, job);
	}

	/**
	 * yuwen start any job failed
	 */
	@Test(expected = SqoopException.class)
	public void startJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.startJob(user, job);
	}

	// No.12 startJob test end

	// No.13 stopJob test start
	/**
	 * sqoop stop any job success
	 */
	@Test
	public void stopJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.stopJob(user, job);
	}

	/**
	 * zhangqiang start any job success
	 */
	@Test
	public void stopJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.stopJob(user, job);
	}

	/**
	 * yuwen stop oracle2hdfs-job success
	 */
	@Test
	public void stopJobOracle2HdfsWithWritePermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.stopJob(user, job);
	}

	/**
	 * yuwen stop any job failed
	 */
	@Test(expected = SqoopException.class)
	public void stopJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.stopJob(user, job);
	}

	// No.13 stopJob test end

	// No.14 statusJob test start
	/**
	 * sqoop status any job success
	 */
	@Test
	public void statusJobAnyWithAllPermissions() {
		String user = SQOOP;
		String job = getRandomJobName();
		AuthorizationEngine.statusJob(user, job);
	}

	/**
	 * zhangqiang status any job success
	 */
	@Test
	public void statusJobAnyAsCreater() {
		String user = ZHANGQIANG;
		String job = getRandomJobName();
		AuthorizationEngine.statusJob(user, job);
	}

	/**
	 * yuwen status oracle2hdfs-job success
	 */
	@Test
	public void statusJobOracle2HdfsWithReadPermission() {
		String user = YUWEN;
		String job = ORACLE2HDFS_JOB;
		AuthorizationEngine.statusJob(user, job);
	}

	/**
	 * yuwen status status job failed
	 */
	@Test(expected = SqoopException.class)
	public void statusJobAnyWithoutPermission() {
		String user = YUWEN;
		String job = getRandomJobName();
		AuthorizationEngine.statusJob(user, job);
	}

	// No.14 statusJob test end

}
