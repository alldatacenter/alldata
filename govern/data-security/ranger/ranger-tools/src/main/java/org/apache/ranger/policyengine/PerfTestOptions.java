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

package org.apache.ranger.policyengine;


import org.apache.ranger.authorization.hadoop.config.RangerConfiguration;

import java.net.URL;

public class PerfTestOptions {
	private final URL servicePoliciesFileURL;
	private final URL[] requestFileURLs;
	private final URL statCollectionFileURL;
	private final boolean isTrieLookupPrefixDisabled;
	private final boolean isOnDemandTriePostSetupDisabled;

	private final int concurrentClientCount;
	private final int iterationsCount;
	private final URL perfConfigurationFileURL;
	private final boolean isPolicyTrieOptimizedForSpace;
	private final boolean isTagTrieOptimizedForSpace;

	PerfTestOptions(URL servicePoliciesFileURL, URL[] requestFileURLs, URL statCollectionFileURL, int concurrentClientCount, int iterationsCount, boolean isTrieLookupPrefixDisabled, boolean isOnDemandTriePostSetupDisabled, URL perfConfigurationFileURL) {
		this.servicePoliciesFileURL = servicePoliciesFileURL;
		this.requestFileURLs = requestFileURLs;
		this.statCollectionFileURL = statCollectionFileURL;
		this.iterationsCount = iterationsCount;
		this.concurrentClientCount = concurrentClientCount;
		this.isTrieLookupPrefixDisabled = isTrieLookupPrefixDisabled;
		this.isOnDemandTriePostSetupDisabled = isOnDemandTriePostSetupDisabled;
		this.perfConfigurationFileURL = perfConfigurationFileURL;

		RangerConfiguration configuration = new PerfTestConfiguration(perfConfigurationFileURL);
		this.isPolicyTrieOptimizedForSpace = configuration.getBoolean("ranger.policyengine.option.optimize.policy.trie.for.space", false);
		this.isTagTrieOptimizedForSpace = configuration.getBoolean("ranger.policyengine.option.optimize.tag.trie.for.space", false);
	}

	public URL getServicePoliciesFileURL() {
		return  this.servicePoliciesFileURL;
	}

	public URL[] getRequestFileURLs() {
		return this.requestFileURLs;
	}

	public URL getStatCollectionFileURL() {
		return  this.statCollectionFileURL;
	}

	public int getConcurrentClientCount() {
		return concurrentClientCount;
	}

	public int getIterationsCount() {
		return iterationsCount;
	}

	public boolean getIsTrieLookupPrefixDisabled() { return isTrieLookupPrefixDisabled; }

	public boolean getIsOnDemandTriePostSetupDisabled() { return isOnDemandTriePostSetupDisabled; }

	public URL getPerfConfigurationFileURL() {
		return  this.perfConfigurationFileURL;
	}

	public boolean getIsPolicyTrieOptimizedForSpace() { return isPolicyTrieOptimizedForSpace; }
	public boolean getIsTagTrieOptimizedForSpace() { return isTagTrieOptimizedForSpace; }

}
