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

package org.apache.ranger.plugin.policyengine;

import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.policyresourcematcher.RangerResourceEvaluator;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerPathResourceMatcher;
import org.apache.ranger.plugin.resourcematcher.RangerResourceMatcher;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class TestPathResourceTrie {
	private static final RangerResourceDef       PATH_RESOURCE_DEF = getPathResourceDef();
	private static final RangerResourceEvaluator EVAL_             = getEvaluator("/");
	private static final RangerResourceEvaluator EVAL_nr           = getEvaluator("/", false, false);
	private static final RangerResourceEvaluator EVAL_HOME         = getEvaluator("/home");
	private static final RangerResourceEvaluator EVAL_HOME_        = getEvaluator("/home/");
	private static final RangerResourceEvaluator EVAL_TMPnr        = getEvaluator("/tmp", false, false);
	private static final RangerResourceEvaluator EVAL_TMP_nr       = getEvaluator("/tmp/", false, false);
	private static final RangerResourceEvaluator EVAL_TMP_AB       = getEvaluator("/tmp/ab");
	private static final RangerResourceEvaluator EVAL_TMP_A_B      = getEvaluator("/tmp/a/b");
	private static final RangerResourceEvaluator EVAL_TMP_AC_D_E_F = getEvaluator("/tmp/ac/d/e/f");
	private static final RangerResourceEvaluator EVAL_TMPFILE      = getEvaluator("/tmpfile");
	private static final RangerResourceEvaluator EVAL_TMPdTXT      = getEvaluator("/tmp.txt");
	private static final RangerResourceEvaluator EVAL_TMPA_B       = getEvaluator("/tmpa/b");

	private static final List<RangerResourceEvaluator> EVALUATORS = Arrays.asList(EVAL_,
																						EVAL_nr,
																						EVAL_HOME,
																						EVAL_HOME_,
																						EVAL_TMPnr,
																						EVAL_TMP_nr,
																						EVAL_TMP_AB,
																						EVAL_TMP_A_B,
																						EVAL_TMP_AC_D_E_F,
																						EVAL_TMPFILE,
																						EVAL_TMPdTXT,
																						EVAL_TMPA_B
																						);

	private final RangerResourceTrie<RangerResourceEvaluator> trie = new RangerResourceTrie<>(PATH_RESOURCE_DEF, EVALUATORS);

	@Test
	public void testChildrenScope() {
		final RangerAccessRequest.ResourceMatchingScope scope = RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD;

		verifyEvaluators("/", scope, EVAL_, EVAL_nr, EVAL_HOME, EVAL_HOME_, EVAL_TMPnr, EVAL_TMP_nr, EVAL_TMPFILE, EVAL_TMPdTXT);
		verifyEvaluators("/tmp", scope, EVAL_, EVAL_TMPnr, EVAL_TMP_nr, EVAL_TMP_AB);
		verifyEvaluators("/tmp/", scope, EVAL_, EVAL_TMP_nr, EVAL_TMP_AB);
		verifyEvaluators("/tmp/a", scope, EVAL_, EVAL_TMP_A_B);
		verifyEvaluators("/tmp/ac", scope, EVAL_);
		verifyEvaluators("/tmp/ac/d", scope, EVAL_);
		verifyEvaluators("/tmp/ac/d/e", scope, EVAL_, EVAL_TMP_AC_D_E_F);
		verifyEvaluators("/unmatched", scope, EVAL_);
		verifyEvaluators("invalid: does-not-begin-with-sep", scope);
	}

	@Test
	public void testSelfScope() {
		final RangerAccessRequest.ResourceMatchingScope scope = RangerAccessRequest.ResourceMatchingScope.SELF;

		verifyEvaluators("/", scope, EVAL_, EVAL_nr);
		verifyEvaluators("/tmp", scope, EVAL_, EVAL_TMPnr);
		verifyEvaluators("/tmp/", scope, EVAL_, EVAL_TMP_nr);
		verifyEvaluators("/tmp/a", scope, EVAL_);
		verifyEvaluators("/tmp/ac", scope, EVAL_);
		verifyEvaluators("/tmp/ac/d", scope, EVAL_);
		verifyEvaluators("/tmp/ac/d/e", scope, EVAL_);
		verifyEvaluators("/unmatched", scope, EVAL_);
		verifyEvaluators("invalid: does-not-begin-with-sep", scope);
	}

	private void verifyEvaluators(String resource, RangerAccessRequest.ResourceMatchingScope scope, RangerResourceEvaluator... evaluators) {
		Set<RangerResourceEvaluator> expected = evaluators.length == 0 ? null : new HashSet<>(Arrays.asList(evaluators));
		Set<RangerResourceEvaluator> result   = trie.getEvaluatorsForResource(resource, scope);

		assertEquals("incorrect evaluators for resource "  + resource, expected, result);
	}

	private static RangerResourceDef getPathResourceDef() {
		RangerResourceDef ret = new RangerResourceDef();

		ret.setItemId(1L);
		ret.setName("path");
		ret.setType("path");
		ret.setLevel(10);
		ret.setParent("");
		ret.setMatcher("org.apache.ranger.plugin.resourcematcher.RangerPathResourceMatcher");
		ret.setMatcherOptions(new HashMap<String, String>() {{
			put("wildCard", "true");
			put("ignoreCase", "true");
			put("pathSeparatorChar", "/");
		}});

		return ret;
	}

	private static RangerResourceEvaluator getEvaluator(String resource) {
		return  new TestPolicyResourceEvaluator(new RangerPolicyResource(resource, false, true));
	}

	private static RangerResourceEvaluator getEvaluator(String resource, boolean  isExcludes, boolean isRecursive) {
		return  new TestPolicyResourceEvaluator(new RangerPolicyResource(resource, isExcludes, isRecursive));
	}

	private static class TestPolicyResourceEvaluator implements RangerResourceEvaluator {
		private static long nextId = 1;

		private final long                  id;
		private final RangerPolicyResource  policyResource;
		private final RangerResourceMatcher resourceMatcher;

		TestPolicyResourceEvaluator(RangerPolicyResource policyResource) {
			this.id = nextId++;
			this.policyResource = policyResource;
			this.resourceMatcher = new RangerPathResourceMatcher();

			resourceMatcher.setResourceDef(PATH_RESOURCE_DEF);
			resourceMatcher.setPolicyResource(policyResource);

			resourceMatcher.init();
		}

		@Override
		public long getId() {
			return id;
		}

		@Override
		public RangerPolicyResourceMatcher getPolicyResourceMatcher() {
			return null;
		}

		@Override
		public Map<String, RangerPolicyResource> getPolicyResource() {
			return Collections.singletonMap(PATH_RESOURCE_DEF.getName(), policyResource);
		}

		@Override
		public RangerResourceMatcher getResourceMatcher(String resourceName) {
			return resourceMatcher;
		}

		@Override
		public boolean isAncestorOf(RangerResourceDef resourceDef) {
			return false;
		}

		@Override
		public String toString() {
			return "id=" + id + ", resource=" + policyResource;
		}
	}
}
