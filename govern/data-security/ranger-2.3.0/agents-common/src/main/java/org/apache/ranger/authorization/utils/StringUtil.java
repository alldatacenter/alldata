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

 package org.apache.ranger.authorization.utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

public class StringUtil {

    private static final TimeZone gmtTimeZone = TimeZone.getTimeZone("GMT+0");

    public static boolean equals(String str1, String str2) {
		boolean ret = false;

		if(str1 == null) {
			ret = str2 == null;
		} else if(str2 == null) {
			ret = false;
		} else {
			ret = str1.equals(str2);
		}

		return ret;
	}

	public static boolean equalsIgnoreCase(String str1, String str2) {
		boolean ret = false;

		if(str1 == null) {
			ret = str2 == null;
		} else if(str2 == null) {
			ret = false;
		} else {
			ret = str1.equalsIgnoreCase(str2);
		}

		return ret;
	}

	public static boolean equals(Collection<String> set1, Collection<String> set2) {
		boolean ret = false;

		if(set1 == null) {
			ret = set2 == null;
		} else if(set2 == null) {
			ret = false;
		} else if(set1.size() == set2.size()) {
			ret = set1.containsAll(set2);
		}

		return ret;
	}

	public static boolean equalsIgnoreCase(Collection<String> set1, Collection<String> set2) {
		boolean ret = false;

		if(set1 == null) {
			ret = set2 == null;
		} else if(set2 == null) {
			ret = false;
		} else if(set1.size() == set2.size()) {
			int numFound = 0;

			for(String str1 : set1) {
				boolean str1Found = false;

				for(String str2 : set2) {
					if(equalsIgnoreCase(str1, str2)) {
						str1Found = true;

						break;
					}
				}
				
				if(str1Found) {
					numFound++;
				} else {
					break;
				}
			}

			ret = numFound == set1.size();
		}

		return ret;
	}

	public static boolean matches(String pattern, String str) {
		boolean ret = false;

		if(pattern == null || str == null || pattern.isEmpty() || str.isEmpty()) {
			ret = true;
		} else {
			ret = str.matches(pattern);
		}

		return ret;
	}

	/*
	public static boolean matches(Collection<String> patternSet, Collection<String> strSet) {
		boolean ret = false;

		if(patternSet == null || strSet == null || patternSet.isEmpty() || strSet.isEmpty()) {
			ret = true;
		} else {
			boolean foundUnmatched = false;

			for(String str : strSet) {
				boolean isMatched = false;
				for(String pattern : patternSet) {
					isMatched = str.matches(pattern);
					
					if(isMatched) {
						break;
					}
				}
				
				foundUnmatched = ! isMatched;
				
				if(foundUnmatched) {
					break;
				}
			}
			
			ret = !foundUnmatched;
		}

		return ret;
	}
	*/

	public static boolean contains(String str, String strToFind) {
		return str != null && strToFind != null && str.contains(strToFind);
	}

	public static boolean containsIgnoreCase(String str, String strToFind) {
		return str != null && strToFind != null && str.toLowerCase().contains(strToFind.toLowerCase());
	}

	public static boolean contains(String[] strArr, String str) {
		boolean ret = false;

		if(strArr != null && strArr.length > 0 && str != null) {
			for(String s : strArr) {
				ret = equals(s, str);

				if(ret) {
					break;
				}
			}
		}

		return ret;
	}

	public static boolean containsIgnoreCase(String[] strArr, String str) {
		boolean ret = false;

		if(strArr != null && strArr.length > 0 && str != null) {
			for(String s : strArr) {
				ret = equalsIgnoreCase(s, str);
				
				if(ret) {
					break;
				}
			}
		}

		return ret;
	}

	public static String toString(Iterable<String> iterable) {
		String ret = "";

		if(iterable != null) {
			int count = 0;
			for(String str : iterable) {
				if(count == 0)
					ret = str;
				else
					ret += (", " + str);
				count++;
			}
		}
		
		return ret;
	}

	public static String toString(String[] arr) {
		String ret = "";

		if(arr != null && arr.length > 0) {
			ret = arr[0];
			for(int i = 1; i < arr.length; i++) {
				ret += (", " + arr[i]);
			}
		}
		
		return ret;
	}

	public static String toString(List<String> arr) {
		String ret = "";

		if(arr != null && !arr.isEmpty()) {
			ret = arr.get(0);
			for(int i = 1; i < arr.size(); i++) {
				ret += (", " + arr.get(i));
			}
		}
		
		return ret;
	}

	public static boolean isEmpty(String str) {
		return str == null || str.trim().isEmpty();
	}

	public static <T> boolean isEmpty(Collection<T> set) {
		return set == null || set.isEmpty();
	}
	
	public static String toLower(String str) {
		return str == null ? null : str.toLowerCase();
	}

	public static byte[] getBytes(String str) {
		return str == null ? null : str.getBytes();
	}

	public static Date getUTCDate() {
	    Calendar local  = Calendar.getInstance();
	    int      offset = local.getTimeZone().getOffset(local.getTimeInMillis());

	    GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);

	    utc.setTimeInMillis(local.getTimeInMillis());
	    utc.add(Calendar.MILLISECOND, -offset);

	    return utc.getTime();
	}

	public static Date getUTCDateForLocalDate(Date date) {
		Calendar local  = Calendar.getInstance();
		int      offset = local.getTimeZone().getOffset(local.getTimeInMillis());

		GregorianCalendar utc = new GregorianCalendar(gmtTimeZone);

		utc.setTimeInMillis(date.getTime());
		utc.add(Calendar.MILLISECOND, -offset);

		return utc.getTime();
	}

	public static Map<String, Object> toStringObjectMap(Map<String, String> map) {
		Map<String, Object> ret = null;

		if (map != null) {
			ret = new HashMap<>(map.size());

			for (Map.Entry<String, String> e : map.entrySet()) {
				ret.put(e.getKey(), e.getValue());
			}
		}

		return ret;
	}

	public static Set<String> toSet(String str) {
		Set<String> values = new HashSet<String>();
		if (StringUtils.isNotBlank(str)) {
			for (String item : str.split(",")) {
				if (StringUtils.isNotBlank(item)) {
					values.add(StringUtils.trim(item));
				}
			}
		}
		return values;
	}

	public static List<String> toList(String str) {
		List<String> values;
		if (StringUtils.isNotBlank(str)) {
			values = new ArrayList<>();
			for (String item : str.split(",")) {
				if (StringUtils.isNotBlank(item)) {
					values.add(StringUtils.trim(item));
				}
			}
		} else {
			values = Collections.emptyList();
		}
		return values;
	}

	public static List<String> getURLs(String configURLs) {
		List<String> configuredURLs = new ArrayList<>();
		if(configURLs!=null) {
			String[] urls = configURLs.split(",");
			for (String strUrl : urls) {
				if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(strUrl))) {
					if (strUrl.endsWith("/")) {
						strUrl = strUrl.substring(0, strUrl.length() - 1);
					}
					configuredURLs.add(strUrl);
				}
			}
		}
		return configuredURLs;
	}
}
