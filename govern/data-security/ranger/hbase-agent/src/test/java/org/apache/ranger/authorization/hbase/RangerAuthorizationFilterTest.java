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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.filter.Filter.ReturnCode;
import org.junit.Test;

public class RangerAuthorizationFilterTest {

	@Test
	public void testFilterKeyValueCell_happyPath() throws IOException {

		// null/empty column collection in cache for a family implies family level access
		String[] allowedFamilies = new String[] { "family1", "family2" };
		String[] deniedFamilies = new String[] { "family3", "family4" };
		String[] indeterminateFamilies = new String[] { "family5", "family6" };
		Set<String> familiesAccessAllowed = ImmutableSet.copyOf(allowedFamilies);
		Set<String> familiesAccessDenied = ImmutableSet.copyOf(deniedFamilies);
		Set<String> familiesAccessIndeterminate = ImmutableSet.copyOf(indeterminateFamilies);

		Map<String, Set<String>> columnsAccessAllowed = new HashMap<String, Set<String>>();
		String[] family7KnowGoodColumns = new String[] {"family7-column1", "family7-column2"};
		columnsAccessAllowed.put("family7", ImmutableSet.copyOf(family7KnowGoodColumns));
		String[] family8KnowGoodColumns = new String[] {"family8-column1", "family8-column2"};
		columnsAccessAllowed.put("family8", ImmutableSet.copyOf(family8KnowGoodColumns));

		// auth session
		AuthorizationSession session = createSessionMock();
		RangerAuthorizationFilter filter = new RangerAuthorizationFilter(session, familiesAccessAllowed, familiesAccessDenied, familiesAccessIndeterminate, columnsAccessAllowed);

		// evaluate access for various types of cases
		Cell aCell = mock(Cell.class);
		// families with know denied acess
		for (String family : deniedFamilies) {
			setFamilyArray(aCell, family.getBytes());
			setQualifierArray(aCell, new byte[0]);
			assertEquals(ReturnCode.NEXT_COL, filter.filterKeyValue(aCell));
		}
		// family that isn't in allowed and if cell does not have column then it should be denied
		setFamilyArray(aCell, "family7".getBytes());
		setQualifierArray(aCell, new byte[0]);
		assertEquals(ReturnCode.NEXT_COL, filter.filterKeyValue(aCell));
		// families with known partial access
		for (String column : family7KnowGoodColumns ) {
			setQualifierArray(aCell, column.getBytes());
			assertEquals(ReturnCode.INCLUDE, filter.filterKeyValue(aCell));
		}
		setFamilyArray(aCell, "family8".getBytes());
		for (String column : family8KnowGoodColumns ) {
			setQualifierArray(aCell, column.getBytes());
			assertEquals(ReturnCode.INCLUDE, filter.filterKeyValue(aCell));
		}
		// try some columns that are not in the cache
		for (String column : new String[] { "family8-column3", "family8-column4"}) {
			setQualifierArray(aCell, column.getBytes());
			assertEquals(ReturnCode.NEXT_COL, filter.filterKeyValue(aCell));
		}
		// families with known allowed access - for these we need to doctor up the session
		when(session.isAuthorized()).thenReturn(true);
		for (String family : allowedFamilies) {
			setFamilyArray(aCell, family.getBytes());
			setQualifierArray(aCell, "some-column".getBytes());
			assertEquals(ReturnCode.INCLUDE, filter.filterKeyValue(aCell));
		}
		when(session.isAuthorized()).thenReturn(false);
		for (String family : indeterminateFamilies) {
			setFamilyArray(aCell, family.getBytes());
			setQualifierArray(aCell, "some-column".getBytes());
			assertEquals(ReturnCode.NEXT_COL, filter.filterKeyValue(aCell));
		}
	}

	private void setFamilyArray(Cell aCell, byte[] familyArray) {
		when(aCell.getFamilyArray()).thenReturn(familyArray);
		when(aCell.getFamilyLength()).thenReturn((byte) familyArray.length);
		when(aCell.getFamilyOffset()).thenReturn(0);
	}

	private void setQualifierArray(Cell aCell, byte[] qualifierArray) {
		when(aCell.getQualifierArray()).thenReturn(qualifierArray);
		when(aCell.getQualifierLength()).thenReturn(qualifierArray.length);
		when(aCell.getQualifierOffset()).thenReturn(0);
	}

	AuthorizationSession createSessionMock() {
		AuthorizationSession session = mock(AuthorizationSession.class);
		when(session.column(anyString())).thenReturn(session);
		when(session.columnFamily(anyString())).thenReturn(session);
		when(session.table(anyString())).thenReturn(session);
		when(session.buildRequest()).thenReturn(session);
		when(session.authorize()).thenReturn(session);
		when(session.isAuthorized()).thenReturn(false); // by default the mock fails all auth requests

		HbaseAuditHandler auditHandler = mock(HbaseAuditHandler.class);
		session._auditHandler = auditHandler;

		return session;
	}
}
