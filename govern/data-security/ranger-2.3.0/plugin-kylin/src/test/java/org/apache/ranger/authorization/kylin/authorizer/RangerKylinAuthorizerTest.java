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

package org.apache.ranger.authorization.kylin.authorizer;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.kylin.common.KylinConfig;
import org.apache.kylin.metadata.project.ProjectInstance;
import org.apache.kylin.metadata.project.ProjectManager;
import org.apache.kylin.metadata.project.RealizationEntry;
import org.apache.kylin.rest.util.AclEvaluate;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
*
* Here we plug the Ranger RangerKylinAuthorizer into Kylin.
*
* A custom RangerAdminClient is plugged into Ranger in turn, which loads security policies from a local file.
* These policies were generated in the Ranger Admin UI for a kylin service called "kylinTest":
*
* a) A user "kylin" can do all permissions(contains "ADMIN", "MANAGEMENT", "OPERATION", "QUERY")
* on all kylin projects;
* b) A user "zhangqiang" can do a "ADMIN" on the project "test_project",
* and do a "OPERATION" on the project "kylin_project";
* c) A user "yuwen" can do a "ADMIN" on the project "test_project",
* and do a "OPERATION" on the project "kylin_project";
* d) A user "admin" has role "ROLE_ADMIN",
* and the others have role "ROLE_USER" by mock for test.
*
*/
@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:applicationContext.xml", "classpath*:kylinSecurity.xml" })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RangerKylinAuthorizerTest {
	private static final Map<String, ProjectInstance> uuid2Projects = new HashMap<>();

	private static final Map<String, ProjectInstance> name2Projects = new HashMap<>();

	private static final String ADMIN = "admin";

	private static final String KYLIN = "kylin";

	private static final String ZHANGQIANG = "zhangqiang";

	private static final String YUWEN = "yuwen";

	private static final String LEARN_PROJECT = "learn_project";

	private static final String TEST_PROJECT = "test_project";

	private static final String KYLIN_PROJECT = "kylin_project";

	private static final String[] PROJECTNAMES = new String[] { LEARN_PROJECT, TEST_PROJECT, KYLIN_PROJECT };

	private static final String ROLE_ADMIN = "ADMIN";

	private static final String ROLE_USER = "USER";

	@Autowired
	private AclEvaluate aclEvaluate;

	@BeforeClass
	public static void setup() throws Exception {
		// set kylin conf path
		System.setProperty(KylinConfig.KYLIN_CONF, "src/test/resources");

		// init kylin projects
		initKylinProjects();

		// mock kylin projects, to match projectUuid and projectName for kylin
		mockKylinProjects();
	}

	@AfterClass
	public static void cleanup() throws Exception {
		// do nothing
	}

	/**
	 * Help function: init kylin projects
	 */
	private static void initKylinProjects() {
		for (String projectName : PROJECTNAMES) {
			ProjectInstance projectInstance = getProjectInstance(projectName);
			name2Projects.put(projectName, projectInstance);

			uuid2Projects.put(projectInstance.getUuid(), projectInstance);
		}
	}

	/**
	 * Help function: mock kylin projects, to match projectUuid and projectName
	 */
	private static void mockKylinProjects() {
		KylinConfig kylinConfig = KylinConfig.getInstanceFromEnv();
		ProjectManager projectManager = mock(ProjectManager.class);

		@SuppressWarnings({ "rawtypes", "unchecked" })
		Map<Class, Object> managersCache = (Map<Class, Object>) ReflectionTestUtils.getField(kylinConfig,
				"managersCache");
		managersCache.put(ProjectManager.class, projectManager);

		Answer<ProjectInstance> answer = new Answer<ProjectInstance>() {
			@Override
			public ProjectInstance answer(InvocationOnMock invocation) throws Throwable {
				Object[] args = invocation.getArguments();
				if (args == null || args.length == 0) {
					return null;
				}
				String uuid = (String) args[0];
				return uuid2Projects.get(uuid);
			}
		};
		when(projectManager.getPrjByUuid(anyString())).thenAnswer(answer);
	}

	/**
	 * Help function: get random project instance for test
	 */
	private static ProjectInstance getRandomProjectInstance() {
		String name = RandomStringUtils.randomAlphanumeric(10) + "-project";
		return getProjectInstance(name);
	}

	/**
	 * Help function: get specific project instance for test
	 */
	private static ProjectInstance getProjectInstance(String name) {
		String owner = null;
		String description = null;
		LinkedHashMap<String, String> overrideProps = null;
		List<RealizationEntry> realizationEntries = null;
		List<String> models = null;

		return ProjectInstance.create(name, owner, description, overrideProps, realizationEntries, models);
	}

	// No.1 hasProjectReadPermission test start
	/**
	 * no credentials read any project failed
	 */
	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void readProjectAnyWithoutCredentials() {
		ProjectInstance project = getRandomProjectInstance();
		aclEvaluate.hasProjectReadPermission(project);
	}

	/**
	 * admin read all projects sueecss
	 */
	@Test
	@WithMockUser(username = ADMIN, roles = { ROLE_ADMIN })
	public void readProjectAllAsRoleAdmin() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectReadPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * kylin read all projects success
	 */
	@Test
	@WithMockUser(username = KYLIN, roles = { ROLE_USER })
	public void readProjectAllWithAdminPermission() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectReadPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * zhangqiang read test_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void readProjectTestWithAdminPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectReadPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * zhangqiang read kylin_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void readProjectKylinWithOperationPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectReadPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen read test_project success
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void readProjectTestWithManagementPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectReadPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen read kylin_project success
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void readProjectKylinWithQueryPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectReadPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen read learn_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void readProjectLearnWithoutPermission() {
		ProjectInstance project = name2Projects.get(LEARN_PROJECT);
		boolean result = aclEvaluate.hasProjectReadPermission(project);
		Assert.assertFalse(result);
	}

	// No.1 hasProjectReadPermission test end

	// No.2 hasProjectOperationPermission test start
	/**
	 * no credentials operation any project failed
	 */
	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void operationProjectAnyWithoutCredentials() {
		ProjectInstance project = getRandomProjectInstance();
		aclEvaluate.hasProjectOperationPermission(project);
	}

	/**
	 * admin operation all projects sueecss
	 */
	@Test
	@WithMockUser(username = ADMIN, roles = { ROLE_ADMIN })
	public void operationProjectAllAsRoleAdmin() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectOperationPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * kylin operation all projects success
	 */
	@Test
	@WithMockUser(username = KYLIN, roles = { ROLE_USER })
	public void operationProjectAllWithAdminPermission() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectOperationPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * zhangqiang operation test_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void operationProjectTestWithAdminPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectOperationPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * zhangqiang operation kylin_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void operationProjectKylinWithOperationPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectOperationPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen operation test_project success
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void operationProjectTestWithManagementPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectOperationPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen operation kylin_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void operationProjectKylinWithoutPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectOperationPermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen operation learn_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void operationProjectLearnWithoutPermission() {
		ProjectInstance project = name2Projects.get(LEARN_PROJECT);
		boolean result = aclEvaluate.hasProjectOperationPermission(project);
		Assert.assertFalse(result);
	}

	// No.2 hasProjectOperationPermission test end

	// No.3 hasProjectWritePermission test start
	/**
	 * no credentials write any project failed
	 */
	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void writeProjectAnyWithoutCredentials() {
		ProjectInstance project = getRandomProjectInstance();
		aclEvaluate.hasProjectWritePermission(project);
	}

	/**
	 * admin write all projects sueecss
	 */
	@Test
	@WithMockUser(username = ADMIN, roles = { ROLE_ADMIN })
	public void writeProjectAllAsRoleAdmin() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectWritePermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * kylin write all projects success
	 */
	@Test
	@WithMockUser(username = KYLIN, roles = { ROLE_USER })
	public void writeProjectAllWithAdminPermission() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectWritePermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * zhangqiang write test_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void writeProjectTestWithAdminPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectWritePermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * zhangqiang write kylin_project failed
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void writeProjectKylinWithoutPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectWritePermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen write test_project success
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void writeProjectTestWithManagementPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectWritePermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * yuwen write kylin_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void writeProjectKylinWithoutPermission2() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectWritePermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen write learn_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void writeProjectLearnWithoutPermission() {
		ProjectInstance project = name2Projects.get(LEARN_PROJECT);
		boolean result = aclEvaluate.hasProjectWritePermission(project);
		Assert.assertFalse(result);
	}

	// No.3 hasProjectWritePermission test end

	// No.4 hasProjectAdminPermission test start
	/**
	 * no credentials admin any project failed
	 */
	@Test(expected = AuthenticationCredentialsNotFoundException.class)
	public void adminProjectAnyWithoutCredentials() {
		ProjectInstance project = getRandomProjectInstance();
		aclEvaluate.hasProjectAdminPermission(project);
	}

	/**
	 * admin admin all projects sueecss
	 */
	@Test
	@WithMockUser(username = ADMIN, roles = { ROLE_ADMIN })
	public void adminProjectAllAsRoleAdmin() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectAdminPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * kylin admin all projects success
	 */
	@Test
	@WithMockUser(username = KYLIN, roles = { ROLE_USER })
	public void adminProjectAllWithAdminPermission() {
		for (ProjectInstance project : uuid2Projects.values()) {
			boolean result = aclEvaluate.hasProjectAdminPermission(project);
			Assert.assertTrue(result);
		}
	}

	/**
	 * zhangqiang admin test_project success
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void adminProjectTestWithAdminPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectAdminPermission(project);
		Assert.assertTrue(result);
	}

	/**
	 * zhangqiang admin kylin_project failed
	 */
	@Test
	@WithMockUser(username = ZHANGQIANG, roles = { ROLE_USER })
	public void adminProjectKylinWithoutPermission() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectAdminPermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen admin test_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void adminProjectTestWithoutPermission() {
		ProjectInstance project = name2Projects.get(TEST_PROJECT);
		boolean result = aclEvaluate.hasProjectAdminPermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen admin kylin_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void adminProjectKylinWithoutPermission2() {
		ProjectInstance project = name2Projects.get(KYLIN_PROJECT);
		boolean result = aclEvaluate.hasProjectAdminPermission(project);
		Assert.assertFalse(result);
	}

	/**
	 * yuwen admin learn_project failed
	 */
	@Test
	@WithMockUser(username = YUWEN, roles = { ROLE_USER })
	public void adminProjectLearnWithoutPermission() {
		ProjectInstance project = name2Projects.get(LEARN_PROJECT);
		boolean result = aclEvaluate.hasProjectAdminPermission(project);
		Assert.assertFalse(result);
	}
	// No.4 hasProjectAdminPermission test end
}
