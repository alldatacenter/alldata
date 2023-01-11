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
package org.apache.ranger.authorization.hbase;



import static org.mockito.Mockito.*;

import org.apache.hadoop.hbase.security.User;
import org.apache.ranger.plugin.service.RangerBasePlugin;
import org.junit.Assert;
import org.junit.Test;

public class AuthorizationSessionTest {

	@Test
	public void testAuthorizationSession() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testOperation() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testOtherInformation() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testAccess() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testUser() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testTable() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testColumnFamily() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testColumn() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testIsBuildable() {
		RangerBasePlugin plugin = new RangerBasePlugin("hbase", "hbase");
		AuthorizationSession session = new AuthorizationSession(plugin);
		try {
			session.verifyBuildable();
			Assert.fail("Should have thrown exception");
		} catch (IllegalStateException e) { }
		// user and access are the only required ones.
		User user = mock(User.class);
		when(user.getGroupNames()).thenReturn(new String[] { "groups", "group2" });
		session.access(" ");
		session.user(user);
		try {
			session.verifyBuildable();
		} catch (IllegalStateException e) {
			Assert.fail("Shouldn't have thrown an exception!");
		}
		// setting column-family without table is a problem
		session.columnFamily("family");
		try {
			session.verifyBuildable();
			Assert.fail("Should have thrown an exception");
		} catch (IllegalStateException e) { }
		
		session.table("table");
		try {
			session.verifyBuildable();
		} catch (IllegalStateException e) {
			Assert.fail("Shouldn't have thrown an exception!");
		}
		// setting column without column-family is a problem
		session.columnFamily(null);
		session.column("col");
		try {
			session.verifyBuildable();
			Assert.fail("Should have thrown an exception");
		} catch (IllegalStateException e) { }
		session.columnFamily("family");
		try {
			session.verifyBuildable();
		} catch (IllegalStateException e) {
			Assert.fail("Should have thrown an exception");
		}
	}

	@Test
	public void testZapAuthorizationState() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testIsProvided() {
		AuthorizationSession session = new AuthorizationSession(null);
		Assert.assertFalse(session.isProvided(null));
		Assert.assertFalse(session.isProvided(""));
		Assert.assertTrue(session.isProvided(" "));
		Assert.assertTrue(session.isProvided("xtq"));
	}

	@Test
	public void testBuildRequest() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testAuthorize() {
		RangerBasePlugin plugin = new RangerBasePlugin("hbase", "hbase");
		
		User user = mock(User.class);
		when(user.getShortName()).thenReturn("user1");
		when(user.getGroupNames()).thenReturn(new String[] { "users" } );
		AuthorizationSession session = new AuthorizationSession(plugin);
		session.access("read")
			.user(user)
			.table(":meta:")
			.buildRequest()
			.authorize();
	}

	@Test
	public void testPublishResults() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testIsAuthorized() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testGetDenialReason() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testGetResourceType() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testRequestToString() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testAudit() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testGetPrintableValue() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testBuildAccessDeniedMessage() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testBuildAccessDeniedMessageString() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testKnownPatternAllowedNotAudited() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testKnownPatternDisallowedNotAudited() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testAuditHandler() {
//		Assert.fail("Not yet implemented");
	}

	@Test
	public void testBuildResult() {
//		Assert.fail("Not yet implemented");
	}
}
