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

package org.apache.ranger.plugin.resourcematcher;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.util.ServiceDefUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


public class RangerPathResourceMatcher extends RangerDefaultResourceMatcher {
	private static final Logger LOG = LoggerFactory.getLogger(RangerPathResourceMatcher.class);

	public static final String OPTION_PATH_SEPARATOR       = "pathSeparatorChar";
	public static final char   DEFAULT_PATH_SEPARATOR_CHAR = org.apache.hadoop.fs.Path.SEPARATOR_CHAR;

	private boolean   policyIsRecursive;
	private Character pathSeparatorChar = '/';

	@Override
	public void init() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerPathResourceMatcher.init()");
		}

		Map<String, String> options = resourceDef == null ? null : resourceDef.getMatcherOptions();

		policyIsRecursive = policyResource != null && policyResource.getIsRecursive();
		pathSeparatorChar = ServiceDefUtil.getCharOption(options, OPTION_PATH_SEPARATOR, DEFAULT_PATH_SEPARATOR_CHAR);

		super.init();

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerPathResourceMatcher.init()");
		}
	}

	@Override

	protected ResourceMatcherWrapper buildResourceMatchers() {
		List<ResourceMatcher> resourceMatchers = new ArrayList<>();
		boolean needsDynamicEval = false;

		for (String policyValue : policyValues) {
			if (optWildCard && policyIsRecursive) {
				if (policyValue.charAt(policyValue.length() - 1) == pathSeparatorChar) {
					policyValue += WILDCARD_ASTERISK;
				}
			}

			ResourceMatcher matcher = getMatcher(policyValue);

			if (matcher != null) {
				if (matcher.isMatchAny()) {
					resourceMatchers.clear();
					break;
				}
				if (!needsDynamicEval && matcher.getNeedsDynamicEval()) {
					needsDynamicEval = true;
				}
				resourceMatchers.add(matcher);
			}
		}

		Collections.sort(resourceMatchers, new ResourceMatcher.PriorityComparator());

		return CollectionUtils.isNotEmpty(resourceMatchers) ?
				new ResourceMatcherWrapper(needsDynamicEval, resourceMatchers) : null;
	}

	@Override
	ResourceMatcher getMatcher(String policyValue) {
		if (!policyIsRecursive) {
			return getPathMatcher(policyValue);
		}

		final int len = policyValue != null ? policyValue.length() : 0;

		if (len == 0) {
			return null;
		}

		// To ensure that when policyValue is single '*', ResourceMatcher created here returns true for isMatchAny()
		if (optWildCard && WILDCARD_ASTERISK.equals(policyValue)) {
			return new CaseInsensitiveStringMatcher("", getOptions());
		}

		boolean isWildcardPresent = false;

		if (optWildCard) {
			for (int i = 0; i < len; i++) {
				final char c = policyValue.charAt(i);

				if (c == '?' || c == '*') {
					isWildcardPresent = true;
					break;
				}
			}
		}

		final ResourceMatcher ret;

		if (isWildcardPresent) {
			ret = new RecursiveWildcardResourceMatcher(policyValue, getOptions(), pathSeparatorChar, optIgnoreCase, RangerPathResourceMatcher::isRecursiveWildCardMatch, optIgnoreCase ? 8 : 7);
		} else {
			ret = new RecursivePathResourceMatcher(policyValue, getOptions(), pathSeparatorChar, optIgnoreCase ? StringUtils::equalsIgnoreCase : StringUtils::equals, optIgnoreCase ? StringUtils::startsWithIgnoreCase : StringUtils::startsWith, optIgnoreCase ? 8 : 7);
		}

		if (optReplaceTokens) {
			ret.setDelimiters(startDelimiterChar, endDelimiterChar, escapeChar, tokenPrefix);
		}

		return ret;
	}

	static boolean isRecursiveWildCardMatch(String pathToCheck, String wildcardPath, Character pathSeparatorChar, IOCase caseSensitivity) {

		boolean ret = false;

		if (! StringUtils.isEmpty(pathToCheck)) {
			String[] pathElements = StringUtils.split(pathToCheck, pathSeparatorChar);

			if(! ArrayUtils.isEmpty(pathElements)) {
				StringBuilder sb = new StringBuilder();

				if(pathToCheck.charAt(0) == pathSeparatorChar) {
					sb.append(pathSeparatorChar); // preserve the initial pathSeparatorChar
				}

				for(String p : pathElements) {
					sb.append(p);

					ret = FilenameUtils.wildcardMatch(sb.toString(), wildcardPath, caseSensitivity);

					if (ret) {
						break;
					}

					sb.append(pathSeparatorChar);
				}

				sb = null;
			} else { // pathToCheck consists of only pathSeparatorChar
				ret = FilenameUtils.wildcardMatch(pathToCheck, wildcardPath, caseSensitivity);
			}
		}
		return ret;
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerPathResourceMatcher={");

		super.toString(sb);

		sb.append("policyIsRecursive={").append(policyIsRecursive).append("} ");

		sb.append("}");

		return sb;
	}

	private ResourceMatcher getPathMatcher(String policyValue) {
		final int len = policyValue != null ? policyValue.length() : 0;

		if (len == 0) {
			return null;
		}

		final ResourceMatcher ret;

		int wildcardStartIdx = -1;
		int wildcardEndIdx = -1;
		boolean needWildcardMatch = false;

		// If optWildcard is true
		//   If ('?' found or non-contiguous '*'s found in policyValue)
		//	   needWildcardMatch = true
		// 	 End
		//
		// 	 wildcardStartIdx is set to index of first '*' in policyValue or -1 if '*' is not found in policyValue, and
		// 	 wildcardEndIdx is set to index of last '*' in policyValue or -1 if '*' is not found in policyValue
		// Else
		// 	 needWildcardMatch is set to false
		// End
		if (optWildCard) {
			for (int i = 0; i < len; i++) {
				final char c = policyValue.charAt(i);

				if (c == '?') {
					needWildcardMatch = true;
					break;
				} else if (c == '*') {
					if (wildcardEndIdx == -1 || wildcardEndIdx == (i - 1)) {
						wildcardEndIdx = i;
						if (wildcardStartIdx == -1) {
							wildcardStartIdx = i;
						}
					} else {
						needWildcardMatch = true;
						break;
					}
				}
			}
		}

		if (needWildcardMatch) { // test?, test*a*, test*a*b, *test*a
			ret = new WildcardResourceMatcher(policyValue, getOptions(), pathSeparatorChar, optIgnoreCase, FilenameUtils::wildcardMatch, 6);
		} else if (wildcardStartIdx == -1) { // test, testa, testab
			ret = new StringResourceMatcher(policyValue, getOptions(), pathSeparatorChar, optIgnoreCase ? StringUtils::equalsIgnoreCase : StringUtils::equals, optIgnoreCase ? 2 : 1);
		} else if (wildcardStartIdx == 0) { // *test, **test, *testa, *testab
			String matchStr = policyValue.substring(wildcardEndIdx + 1);
			ret = new StringResourceMatcher(matchStr, getOptions(), pathSeparatorChar, optIgnoreCase ? StringUtils::endsWithIgnoreCase : StringUtils::endsWith, optIgnoreCase ? 4 : 3);
		} else if (wildcardEndIdx != (len - 1)) { // test*a, test*ab
			ret = new WildcardResourceMatcher(policyValue, getOptions(), pathSeparatorChar, optIgnoreCase, FilenameUtils::wildcardMatch, 6);
		} else { // test*, test**, testa*, testab*
			String matchStr = policyValue.substring(0, wildcardStartIdx);
			ret = new StringResourceMatcher(matchStr, getOptions(), pathSeparatorChar, optIgnoreCase ? StringUtils::startsWithIgnoreCase : StringUtils::startsWith, optIgnoreCase ? 4 : 3);
		}

		if (optReplaceTokens) {
			ret.setDelimiters(startDelimiterChar, endDelimiterChar, escapeChar, tokenPrefix);
		}

		return ret;
	}

	interface TriFunction<T, U, V, R> {
		R apply(T t, U u, V v);
	}

	interface QuadFunction<T, U, V, W, R> {
		R apply(T t, U u, V v, W w);
	}

	static abstract class PathResourceMatcher extends ResourceMatcher {
		final char    pathSeparatorChar;
		final int     priority;

		PathResourceMatcher(String value, Map<String, String> options, char pathSeparatorChar, int priority) {
			super(value, options);
			this.pathSeparatorChar    = pathSeparatorChar;
			this.priority             = priority;
		}
		int getPriority() {
			return priority + (getNeedsDynamicEval() ? DYNAMIC_EVALUATION_PENALTY : 0);
		}
	}

	static class StringResourceMatcher extends PathResourceMatcher {
		final BiFunction<String, String, Boolean> function;
		StringResourceMatcher(String value, Map<String, String> options, char pathSeparatorChar, BiFunction<String, String, Boolean> function, int priority) {
			super(value, options, pathSeparatorChar, priority);
			this.function = function;
		}
		@Override
		boolean isMatch(String resourceValue, Map<String, Object> evalContext) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> StringResourceMatcher.isMatch(resourceValue=" + resourceValue + ", evalContext=" + evalContext + ")");
			}
			String expandedValue = getExpandedValue(evalContext);
			boolean ret = function.apply(resourceValue, expandedValue);
			if (!ret) {
				RangerAccessRequest.ResourceMatchingScope scope = MapUtils.isNotEmpty(evalContext) ? (RangerAccessRequest.ResourceMatchingScope) evalContext.get(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING) : null;
				if (scope == RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD) {
					int lastLevelSeparatorIndex = expandedValue.lastIndexOf(pathSeparatorChar);
					if (lastLevelSeparatorIndex != -1) {
						String shorterExpandedValue = expandedValue.substring(0, lastLevelSeparatorIndex);
						if (resourceValue.charAt(resourceValue.length()-1) == pathSeparatorChar) {
							resourceValue = resourceValue.substring(0, resourceValue.length()-1);
						}
						ret = function.apply(resourceValue, shorterExpandedValue);
					}
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== StringResourceMatcher.isMatch(resourceValue=" + resourceValue + ", expandedValue=" + expandedValue + ") : result:[" + ret + "]");
			}
			return ret;
		}

	}

	static class WildcardResourceMatcher extends PathResourceMatcher {
		final TriFunction<String, String, IOCase, Boolean> function;
		final IOCase ioCase;

		WildcardResourceMatcher(String value, Map<String, String> options, char pathSeparatorChar, boolean optIgnoreCase, TriFunction<String, String, IOCase, Boolean> function, int priority) {
			super(value, options, pathSeparatorChar, priority);
			this.function = function;
			this.ioCase   = optIgnoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE;
		}
		@Override
		boolean isMatch(String resourceValue, Map<String, Object> evalContext) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> WildcardResourceMatcher.isMatch(resourceValue=" + resourceValue + ", evalContext=" + evalContext + ")");
			}
			String expandedValue = getExpandedValue(evalContext);
			boolean ret = function.apply(resourceValue, expandedValue, ioCase);
			if (!ret) {
				RangerAccessRequest.ResourceMatchingScope scope = MapUtils.isNotEmpty(evalContext) ? (RangerAccessRequest.ResourceMatchingScope) evalContext.get(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING) : null;
				if (scope == RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD) {
					int lastLevelSeparatorIndex = expandedValue.lastIndexOf(pathSeparatorChar);
					if (lastLevelSeparatorIndex != -1) {
						String shorterExpandedValue = expandedValue.substring(0, lastLevelSeparatorIndex);
						if (resourceValue.charAt(resourceValue.length()-1) == pathSeparatorChar) {
							resourceValue = resourceValue.substring(0, resourceValue.length()-1);
						}
						ret = function.apply(resourceValue, shorterExpandedValue, ioCase);
					}
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== WildcardResourceMatcher.isMatch(resourceValue=" + resourceValue + ", expandedValue=" + expandedValue + ") : result:[" + ret + "]");
			}
			return ret;
		}
	}

	static class RecursiveWildcardResourceMatcher extends PathResourceMatcher {
		final QuadFunction<String, String, Character, IOCase, Boolean> function;
		final IOCase ioCase;

		RecursiveWildcardResourceMatcher(String value, Map<String, String> options, char pathSeparatorChar, boolean optIgnoreCase, QuadFunction<String, String, Character, IOCase, Boolean> function, int priority) {
			super(value, options, pathSeparatorChar, priority);
			this.function = function;
			this.ioCase   = optIgnoreCase ? IOCase.INSENSITIVE : IOCase.SENSITIVE;
		}
		@Override
		boolean isMatch(String resourceValue, Map<String, Object> evalContext) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RecursiveWildcardResourceMatcher.isMatch(resourceValue=" + resourceValue + ", evalContext=" + evalContext + ")");
			}
			String expandedValue = getExpandedValue(evalContext);
			boolean ret = function.apply(resourceValue, expandedValue, pathSeparatorChar, ioCase);
			if (!ret) {
				RangerAccessRequest.ResourceMatchingScope scope = MapUtils.isNotEmpty(evalContext) ? (RangerAccessRequest.ResourceMatchingScope) evalContext.get(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING) : null;
				if (scope == RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD) {
					int lastLevelSeparatorIndex = expandedValue.lastIndexOf(pathSeparatorChar);
					if (lastLevelSeparatorIndex != -1) {
						String shorterExpandedValue = expandedValue.substring(0, lastLevelSeparatorIndex);
						if (resourceValue.charAt(resourceValue.length()-1) == pathSeparatorChar) {
							resourceValue = resourceValue.substring(0, resourceValue.length()-1);
						}
						ret = function.apply(resourceValue, shorterExpandedValue, pathSeparatorChar, ioCase);
					}
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RecursiveWildcardResourceMatcher.isMatch(resourceValue=" + resourceValue + ", expandedValue=" + expandedValue + ") : result:[" + ret + "]");
			}
			return ret;
		}
	}

	static class RecursivePathResourceMatcher extends PathResourceMatcher {
		String valueWithoutSeparator;
		String valueWithSeparator;

		final BiFunction<String, String, Boolean> primaryFunction;
		final BiFunction<String, String, Boolean> fallbackFunction;

		RecursivePathResourceMatcher(String value, Map<String, String> options, char pathSeparatorChar, BiFunction<String, String, Boolean> primaryFunction, BiFunction<String, String, Boolean> fallbackFunction, int priority) {
			super(value, options, pathSeparatorChar, priority);
			this.primaryFunction    = primaryFunction;
			this.fallbackFunction   = fallbackFunction;
		}

		String getStringToCompare(String policyValue) {
			if (StringUtils.isEmpty(policyValue)) {
				return policyValue;
			}
			return (policyValue.lastIndexOf(pathSeparatorChar) == policyValue.length() - 1) ?
					policyValue.substring(0, policyValue.length() - 1) : policyValue;
		}

		@Override
		boolean isMatch(String resourceValue, Map<String, Object> evalContext) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RecursivePathResourceMatcher.isMatch(resourceValue=" + resourceValue + ", evalContext=" + evalContext + ")");
			}
			final String noSeparator;
			if (getNeedsDynamicEval()) {
				String expandedPolicyValue = getExpandedValue(evalContext);
				noSeparator = expandedPolicyValue != null ? getStringToCompare(expandedPolicyValue) : null;
			} else {
				if (valueWithoutSeparator == null && value != null) {
					valueWithoutSeparator = getStringToCompare(value);
					valueWithSeparator = valueWithoutSeparator + pathSeparatorChar;
				}
				noSeparator = valueWithoutSeparator;
			}

			boolean ret = primaryFunction.apply(resourceValue, noSeparator);

			if (!ret && noSeparator != null) {
				final String withSeparator = getNeedsDynamicEval() ? noSeparator + pathSeparatorChar : valueWithSeparator;
				ret = fallbackFunction.apply(resourceValue, withSeparator);

				if (!ret) {
					RangerAccessRequest.ResourceMatchingScope scope = MapUtils.isNotEmpty(evalContext) ? (RangerAccessRequest.ResourceMatchingScope) evalContext.get(RangerAccessRequest.RANGER_ACCESS_REQUEST_SCOPE_STRING) : null;
					if (scope == RangerAccessRequest.ResourceMatchingScope.SELF_OR_CHILD) {
						final int lastLevelSeparatorIndex = noSeparator.lastIndexOf(pathSeparatorChar);
						if (lastLevelSeparatorIndex != -1) {
							final String shorterExpandedValue = noSeparator.substring(0, lastLevelSeparatorIndex);
							if (resourceValue.charAt(resourceValue.length() - 1) == pathSeparatorChar) {
								resourceValue = resourceValue.substring(0, resourceValue.length() - 1);
							}
							ret = primaryFunction.apply(resourceValue, shorterExpandedValue);
						}
					}
				}
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RecursivePathResourceMatcher.isMatch(resourceValue=" + resourceValue + ", expandedValueWithoutTrailingSeparatorChar=" + noSeparator + ") : result:[" + ret + "]");
			}

			return ret;
		}
	}

}
