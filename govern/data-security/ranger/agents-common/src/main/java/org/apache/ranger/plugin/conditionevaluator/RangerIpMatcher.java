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


package org.apache.ranger.plugin.conditionevaluator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Credits: Large parts of this file have been lifted as is from org.apache.ranger.pdp.knox.URLBasedAuthDB.  Credits for those are due to Dilli Arumugam.
 * @author alal
 */
public class RangerIpMatcher extends RangerAbstractConditionEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerIpMatcher.class);
	private List<String> _exactIps = new ArrayList<>();
	private List<String> _wildCardIps = new ArrayList<>();
	private boolean _allowAny;
	
	@Override
	public void init() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.init(" + condition + ")");
		}

		super.init();

		// NOTE: this evaluator does not use conditionDef!
		if (condition == null) {
			LOG.debug("init: null policy condition! Will match always!");
			_allowAny = true;
		} else if (CollectionUtils.isEmpty(condition.getValues())) {
			LOG.debug("init: empty conditions collection on policy condition!  Will match always!");
			_allowAny = true;
		} else if (condition.getValues().contains("*")) {
			_allowAny = true;
			LOG.debug("init: wildcard value found.  Will match always.");
		} else {
			for (String ip : condition.getValues()) {
				String digestedIp = digestPolicyIp(ip);
				if (digestedIp.isEmpty()) {
					LOG.debug("init: digested ip was empty! Will match always");
					_allowAny = true;
				} else if (digestedIp.equals(ip)) {
					_exactIps.add(ip);
				} else {
					_wildCardIps.add(digestedIp);
				}
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.init(" + condition + "): exact-ips[" + _exactIps + "], wildcard-ips[" + _wildCardIps + "]");
		}
	}

	@Override
	public boolean isMatched(final RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.isMatched(" + request + ")");
		}

		boolean ipMatched = true;
		if (_allowAny) {
			LOG.debug("isMatched: allowAny flag is true.  Matched!");
		} else {
			String requestIp = extractIp(request);
			if (requestIp == null) {
				LOG.debug("isMatched: couldn't get ip address from request.  Ok.  Implicitly matched!");
			} else {
				ipMatched = isWildcardMatched(_wildCardIps, requestIp) || isExactlyMatched(_exactIps, requestIp);
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.isMatched(" + request+ "): " + ipMatched);
		}

		return ipMatched;
	}
	
	/**
	 * Pre-digests the policy ip address to drop any trailing wildcard specifiers such that a simple beginsWith match can be done to check for match during authorization calls
	 * @param ip
	 * @return
	 */
	static final Pattern allWildcards = Pattern.compile("^((\\*(\\.\\*)*)|(\\*(:\\*)*))$"); // "*", "*.*", "*.*.*", "*:*", "*:*:*", etc.
	static final Pattern trailingWildcardsIp4 = Pattern.compile("(\\.\\*)+$"); // "blah.*", "blah.*.*", etc.
	static final Pattern trailingWildcardsIp6 = Pattern.compile("(:\\*)+$");   // "blah:*", "blah:*:*", etc.
	
	String digestPolicyIp(final String policyIp) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.digestPolicyIp(" + policyIp + ")");
		}

		String result;
		Matcher matcher = allWildcards.matcher(policyIp);
		if (matcher.matches()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("digestPolicyIp: policyIP[" + policyIp +"] all wildcards.");
			}
			result = "";
		} else if (policyIp.contains(".")) {
			matcher = trailingWildcardsIp4.matcher(policyIp);
			result = matcher.replaceFirst(".");
		} else {
			matcher = trailingWildcardsIp6.matcher(policyIp);
			// also lower cases the ipv6 items
			result = matcher.replaceFirst(":").toLowerCase();
		}
		
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.digestPolicyIp(" + policyIp + "): " + policyIp);
		}
		return result;
	}
	
	boolean isWildcardMatched(final List<String> ips, final String requestIp) {

		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.isWildcardMatched(" + ips+ ", " + requestIp + ")");
		}

		boolean matchFound = false;
		Iterator<String> iterator = ips.iterator();
		while (iterator.hasNext() && !matchFound) {
			String ip = iterator.next();
			if (requestIp.contains(".") && requestIp.startsWith(ip)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Wildcard Policy IP[" + ip + "] matches request IPv4[" + requestIp + "].");
				}
				matchFound = true;
			} else if (requestIp.toLowerCase().startsWith(ip)) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Wildcard Policy IP[" + ip + "] matches request IPv6[" + requestIp + "].");
					}
					matchFound = true;
			} else {
				LOG.debug("Wildcard policy IP[" + ip + "] did not match request IP[" + requestIp + "].");
			}
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.isWildcardMatched(" + ips+ ", " + requestIp + "): " + matchFound);
		}
		return matchFound;
	}
	
	boolean isExactlyMatched(final List<String> ips, final String requestIp) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.isExactlyMatched(" + ips+ ", " + requestIp + ")");
		}

		boolean matchFound = false;
		if (requestIp.contains(".")) {
			matchFound = ips.contains(requestIp);
		} else {
			matchFound = ips.contains(requestIp.toLowerCase());
		}
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.isExactlyMatched(" + ips+ ", " + requestIp + "): " + matchFound);
		}
		return matchFound;
	}
	
	/**
	 * Extracts and returns the ip address from the request.  Returns null if one can't be obtained out of the request.
	 * @param request
	 * @return
	 */
	String extractIp(final RangerAccessRequest request) {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerIpMatcher.extractIp(" + request+ ")");
		}

		String ip = null;
		if (request == null) {
			LOG.debug("isMatched: Unexpected: null request object!");
		} else {
			ip = request.getClientIPAddress();
			if (ip == null) {
				LOG.debug("isMatched: Unexpected: Client ip in request object is null!");
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerIpMatcher.extractIp(" + request+ "): " + ip);
		}
		return ip;
	}
}
