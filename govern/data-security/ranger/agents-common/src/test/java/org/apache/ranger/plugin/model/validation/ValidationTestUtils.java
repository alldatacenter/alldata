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

package org.apache.ranger.plugin.model.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumElementDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.junit.Assert;

public class ValidationTestUtils {
	
	Map<String, String> createMap(String[] keys) {
		Map<String, String> result = new HashMap<>();
		for (String key : keys) {
			result.put(key, "valueof-" + key);
		}
		return result;
	}

	// helper methods for tests
	List<RangerServiceConfigDef> createServiceConditionDefs(Object[][] input) {
		List<RangerServiceConfigDef> result = new ArrayList<>();
		
		for (Object data[] : input) {
			RangerServiceConfigDef aConfigDef = mock(RangerServiceConfigDef.class);
			when(aConfigDef.getName()).thenReturn((String)data[0]);
			when(aConfigDef.getMandatory()).thenReturn((boolean)data[1]);
			result.add(aConfigDef);
		}
		
		return result;
	}
	
	void checkFailureForSemanticError(List<ValidationFailureDetails> failures, String fieldName) {
		checkFailure(failures, null, null, true, fieldName, null);
	}

	void checkFailureForSemanticError(List<ValidationFailureDetails> failures, String fieldName, String subField) {
		checkFailure(failures, null, null, true, fieldName, subField);
	}

	void checkFailureForMissingValue(List<ValidationFailureDetails> failures, String field) {
		checkFailure(failures, null, true, null, field, null);
	}

	void checkFailureForMissingValue(List<ValidationFailureDetails> failures, String field, String subField) {
		checkFailure(failures, null, true, null, field, subField);
	}

	// check if any one of the sub-fields is present
	void checkFailureForMissingValue(List<ValidationFailureDetails> failures, String field, String[] subFields) {
		if (CollectionUtils.isEmpty(failures)) {
			Assert.fail("List of failures is null/empty!");
		} else {
			boolean found = false;
			int i = 0;
			while (!found && i < subFields.length) {
				String subField = subFields[i];
				if (hasFailure(failures, null, true, null, field, subField)) {
					found = true;
				}
				i++;
			}
			Assert.assertTrue(failures.toString(), found);
		}
	}

	void checkFailureForInternalError(List<ValidationFailureDetails> failures, String fieldName) {
		checkFailure(failures, true, null, null, fieldName, null);
	}

	void checkFailureForInternalError(List<ValidationFailureDetails> failures) {
		checkFailure(failures, true, null, null, null, null);
	}

	void checkFailure(List<ValidationFailureDetails> failures, Boolean internalError, Boolean missing, Boolean semanticError, String field, String subField) {
		if (CollectionUtils.isEmpty(failures)) {
			Assert.fail("List of failures is null/empty!");
		} else {
			boolean found = hasFailure(failures, internalError, missing, semanticError, field, subField);
			Assert.assertTrue(failures.toString(), found);
		}
	}
	
	boolean hasFailure(List<ValidationFailureDetails> failures, Boolean internalError, Boolean missing, Boolean semanticError, String field, String subField) {
		boolean found = false;
		for (ValidationFailureDetails f : failures) {
			if ((internalError == null || internalError == f._internalError) &&
					(missing == null || missing == f._missing) &&
					(semanticError == null || semanticError == f._semanticError) &&
					(field == null || field.equals(f._fieldName)) &&
					(subField == null || subField.equals(f._subFieldName))) {
				found = true;
			}
		}
		return found;
	}

	List<RangerAccessTypeDef> createAccessTypeDefs(String[] names) {
		Assert.assertFalse(names == null); // fail if null is passed in!
		List<RangerAccessTypeDef> defs = new ArrayList<RangerServiceDef.RangerAccessTypeDef>();
		for (String name : names) {
			RangerAccessTypeDef def = mock(RangerAccessTypeDef.class);
			when(def.getName()).thenReturn(name);
			defs.add(def);
		}
		return defs;
	}

	List<RangerAccessTypeDef> createAccessTypeDefs(Object[][] data) {
		if (data == null) {
			return null;
		}
		List<RangerAccessTypeDef> result = new ArrayList<>();
		if (data.length == 0) {
			return result;
		}
		for (Object[] entry : data) {
			Long itemId = (Long)entry[0];
			String accessType = (String)entry[1];
			String[] impliedAccessArray = (String[])entry[2];
			List<String> impliedAccesses = null;
			if (impliedAccessArray != null) {
				impliedAccesses = Arrays.asList(impliedAccessArray);
			}
			RangerAccessTypeDef aTypeDef = mock(RangerAccessTypeDef.class);
			when(aTypeDef.getName()).thenReturn(accessType);
			when(aTypeDef.getImpliedGrants()).thenReturn(impliedAccesses);
			when(aTypeDef.getItemId()).thenReturn(itemId);
			result.add(aTypeDef);
		}
		return result;
	}

	RangerServiceDef createServiceDefWithAccessTypes(String[] accesses, String serviceName) {
		RangerServiceDef serviceDef = createServiceDefWithAccessTypes(accesses);
		when(serviceDef.getName()).thenReturn(serviceName);
		return serviceDef;
	}

	RangerServiceDef createServiceDefWithAccessTypes(String[] accesses) {
		RangerServiceDef serviceDef = mock(RangerServiceDef.class);
		List<RangerAccessTypeDef> accessTypeDefs = new ArrayList<>();
		for (String access : accesses) {
			RangerAccessTypeDef accessTypeDef = mock(RangerAccessTypeDef.class);
			when(accessTypeDef.getName()).thenReturn(access);
			accessTypeDefs.add(accessTypeDef);
		}
		when(serviceDef.getAccessTypes()).thenReturn(accessTypeDefs);
		return serviceDef;
	}

	List<RangerPolicyItemAccess> createItemAccess(Object[][] data) {
		List<RangerPolicyItemAccess> accesses = new ArrayList<>();
		for (Object[] row : data) {
			RangerPolicyItemAccess access = mock(RangerPolicyItemAccess.class);
			when(access.getType()).thenReturn((String)row[0]);
			when(access.getIsAllowed()).thenReturn((Boolean)row[1]);
			accesses.add(access);
		}
		return accesses;
	}

	List<RangerPolicyItem> createPolicyItems(Object[] data) {
		List<RangerPolicyItem> policyItems = new ArrayList<>();
		for (Object object : data) {
			@SuppressWarnings("unchecked")
			Map<String, Object[]> map = (Map<String, Object[]>) object;
			RangerPolicyItem policyItem = mock(RangerPolicyItem.class);
			
			List<String> usersList = null;
			if (map.containsKey("users")) {
				usersList = Arrays.asList((String[])map.get("users"));
			}
			when(policyItem.getUsers()).thenReturn(usersList);
			
			List<String> groupsList = null;
			if (map.containsKey("groups")) {
				groupsList = Arrays.asList((String[])map.get("groups"));
			}
			when(policyItem.getGroups()).thenReturn(groupsList);
			
			String[] accesses = (String[])map.get("accesses");
			Boolean[] isAllowedFlags = (Boolean[])map.get("isAllowed");
			List<RangerPolicyItemAccess> accessesList = null;
			if (accesses != null && isAllowedFlags != null) {
				accessesList = new ArrayList<>();
				for (int i = 0; i < accesses.length; i++) {
					String access = accesses[i];
					Boolean isAllowed = isAllowedFlags[i];
					RangerPolicyItemAccess itemAccess = mock(RangerPolicyItemAccess.class);
					when(itemAccess.getType()).thenReturn(access);
					when(itemAccess.getIsAllowed()).thenReturn(isAllowed);
					accessesList.add(itemAccess);
				}
			}
			when(policyItem.getAccesses()).thenReturn(accessesList);
			
			policyItems.add(policyItem);
		}
		return policyItems;
	}

	List<RangerResourceDef> createResourceDefs(Object[][] data) {
		// if data itself is null then return null back
		if (data == null) {
			return null;
		}
		List<RangerResourceDef> defs = new ArrayList<>();
		for (Object[] row : data) {
			RangerResourceDef aDef = null;
			if (row != null) {
				String name = null;
				Boolean mandatory = null;
				String regExPattern = null;
				Boolean isExcludesSupported = null;
				Boolean isRecursiveSupported = null;
				String parent = null;
				Integer level = null;
				switch(row.length) {
				case 7:
					level = (Integer)row[6];
				case 6:
					parent = (String) row[5];
				case 5:
					regExPattern = (String)row[4];
				case 4:
					mandatory = (Boolean)row[3];
				case 3:
					isRecursiveSupported = (Boolean)row[2];
				case 2:
					isExcludesSupported = (Boolean)row[1];
				case 1:
					name = (String)row[0];
				}
				aDef = mock(RangerResourceDef.class);
				when(aDef.getName()).thenReturn(name);
				when(aDef.getMandatory()).thenReturn(mandatory);
				when(aDef.getValidationRegEx()).thenReturn(regExPattern);
				when(aDef.getExcludesSupported()).thenReturn(isExcludesSupported);
				when(aDef.getRecursiveSupported()).thenReturn(isRecursiveSupported);
				when(aDef.getParent()).thenReturn(parent);
				when(aDef.getLevel()).thenReturn(level);
				when(aDef.getIsValidLeaf()).thenReturn(null);
			}
			defs.add(aDef);
		}
		return defs;
	}

	List<RangerResourceDef> createResourceDefsWithRegEx(String[][] data) {
		// if data itself is null then return null back
		if (data == null) {
			return null;
		}
		List<RangerResourceDef> defs = new ArrayList<>();
		for (String[] row : data) {
			RangerResourceDef aDef = null;
			if (row != null) {
				String name = row[0];
				String regEx = row[1];
				aDef = mock(RangerResourceDef.class);
				when(aDef.getName()).thenReturn(name);
				when(aDef.getValidationRegEx()).thenReturn(regEx);
			}
			defs.add(aDef);
		}
		return defs;
	}

	List<RangerResourceDef> createResourceDefsWithIds(Object[][] data) {
		// if data itself is null then return null back
		if (data == null) {
			return null;
		}
		List<RangerResourceDef> defs = new ArrayList<>();
		for (Object[] row : data) {
			RangerResourceDef aDef = null;
			if (row != null) {
				Long itemId = (Long)row[0];
				Integer level = (Integer)row[1];
				String name = (String)row[2];
				aDef = mock(RangerResourceDef.class);
				when(aDef.getName()).thenReturn(name);
				when(aDef.getItemId()).thenReturn(itemId);
				when(aDef.getLevel()).thenReturn(level);
			}
			defs.add(aDef);
		}
		return defs;
	}

	List<RangerEnumElementDef> createEnumElementDefs(String[] input) {
		if (input == null) {
			return null;
		}
		Object[][] data = new Object[input.length][];
		for (int i = 0; i < input.length; i++) {
			data[i] = new Object[] { (long)i, input[i] };
		}
		return createEnumElementDefs(data);
	}

	List<RangerEnumElementDef> createEnumElementDefs(Object[][] input) {
		if (input == null) {
			return null;
		}
		List<RangerEnumElementDef> output = new ArrayList<>();
		for (Object[] row : input) {
			RangerEnumElementDef aDef = mock(RangerEnumElementDef.class);
			switch(row.length) {
			case 2:
				when(aDef.getName()).thenReturn((String)row[1]);
			case 1:
				when(aDef.getItemId()).thenReturn((Long) row[0]);
			}
			output.add(aDef);
		}
		return output;
	}

	List<RangerEnumDef> createEnumDefs(Object[][] input) {
		if (input == null) {
			return null;
		}
		List<RangerEnumDef> defs = new ArrayList<>();
		for (Object[] row : input) {
			RangerEnumDef enumDef = mock(RangerEnumDef.class);
			switch (row.length) {
			case 3:
				List<RangerEnumElementDef> elements = createEnumElementDefs((String[])row[2]);
				when(enumDef.getElements()).thenReturn(elements);
				// by default set default index to last element
				when(enumDef.getDefaultIndex()).thenReturn(elements.size() - 1);
			case 2:
				String enumName = (String) row[1];
				when(enumDef.getName()).thenReturn(enumName);
			case 1:
				when(enumDef.getItemId()).thenReturn((Long)row[0]);
			}
			defs.add(enumDef);
		}
		return defs;
	}

	public Map<String, RangerPolicyResource> createPolicyResourceMap(Object[][] input) {
		if (input == null) {
			return null;
		}
		Map<String, RangerPolicyResource> result = new HashMap<String, RangerPolicyResource>(input.length);
		for (Object[] row : input) {
			String resourceName = (String)row[0];
			String[] valuesArray = (String[])row[1];
			Boolean isExcludes = (Boolean)row[2];
			Boolean isRecursive = (Boolean)row[3];
			RangerPolicyResource aResource = mock(RangerPolicyResource.class);
			if (valuesArray == null) {
				when(aResource.getValues()).thenReturn(null);
			} else {
				when(aResource.getValues()).thenReturn(Arrays.asList(valuesArray));
			}
			when(aResource.getIsExcludes()).thenReturn(isExcludes);
			when(aResource.getIsRecursive()).thenReturn(isRecursive);
			result.put(resourceName, aResource);
		}
		return result;
	}

	List<RangerServiceConfigDef> createServiceDefConfigs(Object[][] input) {
		if (input == null) {
			return null;
		}
		List<RangerServiceConfigDef> result = new ArrayList<>();
		for (Object[] row : input) {
			RangerServiceConfigDef configDef = mock(RangerServiceConfigDef.class);
			switch(row.length) {
			case 5: // default value
				when(configDef.getDefaultValue()).thenReturn((String)row[4]);
			case 4: // subtype
				when(configDef.getSubType()).thenReturn((String) row[3]);
			case 3: // type
				String type = (String)row[2];
				when(configDef.getType()).thenReturn(type);
			case 2: // name
				String name = (String)row[1];
				when(configDef.getName()).thenReturn(name);
			case 1: // id
				Long itemId = (Long)row[0];
				when(configDef.getItemId()).thenReturn(itemId);
			}
			result.add(configDef);
		}
		return result;
	}

	List<RangerPolicyConditionDef> createPolicyConditionDefs(Object[][] input) {
		List<RangerPolicyConditionDef> result = new ArrayList<>();
		if (input != null) {
			for (Object[] row : input) {
				RangerPolicyConditionDef conditionDef = mock(RangerPolicyConditionDef.class);
				when(conditionDef.getItemId()).thenReturn((Long)row[0]);
				when(conditionDef.getName()).thenReturn((String)row[1]);
				when(conditionDef.getEvaluator()).thenReturn((String)row[2]);
				result.add(conditionDef);
			}
		}
		return result;
	}
}
