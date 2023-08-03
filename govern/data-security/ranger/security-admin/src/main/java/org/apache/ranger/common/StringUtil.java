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

 package org.apache.ranger.common;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StringUtil implements Serializable {
	private static final Logger logger = LoggerFactory.getLogger(StringUtil.class);

	static final public String VALIDATION_CRED = "(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}";

	static final public String VALIDATION_NAME = "^([A-Za-z0-9_]|[\u00C0-\u017F])([a-zA-Z0-9\\s_. -@]|[\u00C0-\u017F])+$";
	static final public String VALIDATION_TEXT = "[a-zA-Z0-9\\ \"!@#$%^&amp;*()-_=+;:'&quot;|~`&lt;&gt;?/{}\\.\\,\\-\\?<>\\x00-\\x7F\\p{L}-]*";
	static final public String VALIDATION_LOGINID = "^([A-Za-z0-9_]|[\u00C0-\u017F])([a-z0-9,._\\-+/@= ]|[\u00C0-\u017F])+$";

	static final public String VALIDATION_ALPHA = "[a-z,A-Z]*";
	static final public String VALIDATION_IP_ADDRESS = "[\\d\\.\\%\\:]*";
	static final public String WILDCARD_ASTERISK = "*";

	static HashMap<String, Pattern> compiledRegEx = new HashMap<String, Pattern>();

	String[] invalidNames = null;

	/**
     *
     */
	private static final long serialVersionUID = -2102399594424760213L;

	public StringUtil() {
		// Default constructor
		invalidNames = PropertiesUtil.getPropertyStringList("xa.names.invalid");
	}

	/**
	 * Checks if the string is null or empty string.
	 *
	 * @param str
	 * @return true if it is empty string or null
	 */
	public boolean isEmpty(String str) {
		if (str == null || str.trim().length() == 0) {
			return true;
		}
		return false;
	}

	public boolean isEmptyOrWildcardAsterisk(String str) {
		return isEmpty(str) || WILDCARD_ASTERISK.equals(str);
	}

	public boolean equals(String str1, String str2) {
		if (str1 == str2) {
			return true;
		}

		if (str1 == null || str2 == null) {
			return false;
		}

		return str1.equals(str2);
	}

	public String toCamelCaseAllWords(String str) {
		if (str == null) {
			return null;
		}
		str = str.trim().toLowerCase();
		StringBuilder result = new StringBuilder(str.length());
		boolean makeUpper = true;
		boolean lastCharSpace = true;
		for (int c = 0; c < str.length(); c++) {
			char ch = str.charAt(c);
			if (lastCharSpace && ch == ' ') {
				continue;
			}

			if (makeUpper) {
				result.append(str.substring(c, c + 1).toUpperCase());
				makeUpper = false;
			} else {
				result.append(ch);
			}
			if (ch == ' ') {
				lastCharSpace = true;
				makeUpper = true;
			} else {
				lastCharSpace = false;
			}

		}
		return result.toString();
	}

	public boolean validatePassword(String password, String[] invalidValues) {
		// For now let's make sure we have minimum 8 characters
		if (password == null) {
			return false;
		}
		password = password.trim();
		boolean checkPassword = password.matches(VALIDATION_CRED);
		if (!checkPassword) {
			return false;
		}

		for (int i = 0; invalidValues != null && i < invalidValues.length; i++) {
			if (password.equalsIgnoreCase(invalidValues[i])) {
				return false;
			}
		}
		return true;
	}

	public boolean validateEmail(String emailAddress) {
		if (emailAddress == null || emailAddress.trim().length() > 128) {
			return false;
		}
		emailAddress = emailAddress.trim();
		String expression = "^[\\w]([\\-\\.\\w])+[\\w]+@[\\w]+[\\w\\-]+[\\w]*\\.([\\w]+[\\w\\-]+[\\w]*(\\.[a-z][a-z|0-9]*)?)$";
		return regExPatternMatch(expression, emailAddress);

	}

	public boolean regExPatternMatch(String expression, String inputStr) {
		boolean ret = false;

		if (expression != null && inputStr != null) {
			Pattern pattern = compiledRegEx.get(expression);

			if (pattern == null) {
				pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
				compiledRegEx.put(expression, pattern);
			}

			Matcher matcher = pattern.matcher(inputStr);
			ret = matcher.matches();
		}

		return ret;
	}

	public boolean validateString(String regExStr, String str) {
		try {
			return regExPatternMatch(regExStr, str);
		} catch (Throwable t) {
			logger.info("Error validating string. str=" + str, t);
			return false;
		}
	}

	public String normalizeEmail(String email) {
		// Make email address as lower case
		if (email != null) {
			return email.trim().toLowerCase();
		}
		return null;
	}

	public String[] split(String value) {
		return split(value, ",");
	}

	public String[] split(String value, String delimiter) {
		if (value != null) {
			value = value.startsWith(delimiter) ? value.substring(1) : value;
			String[] splitValues = value.split(delimiter);
			String[] returnValues = new String[splitValues.length];
			int c = -1;
			for (String splitValue : splitValues) {
				String str = splitValue.trim();
				if (str.length() > 0) {
					c++;
					returnValues[c] = str;
				}
			}
			return returnValues;
		} else {
			return new String[0];
		}
	}

	public static String trim(String str) {
		return str != null ? str.trim() : null;
	}

	/**
	 * @param name
	 * @return
	 */
	public boolean isValidName(String name) {
		if (name == null || name.trim().length() < 1) {
			return false;
		}
		for (String invalidName : invalidNames) {
			if (name.toUpperCase().trim()
					.startsWith(invalidName.toUpperCase().trim())) {
				return false;
			}
		}
		return validateString(VALIDATION_NAME, name);
	}

	/**
	 * Checks if the list is null or empty list.
	 *
	 * @param list
	 * @return true if it is empty list or null
	 */
	public boolean isEmpty(List<?> list) {
		if (list == null || list.isEmpty()) {
			return true;
		}
		return false;
	}
	
	/**
	 * Returns a valid user name from the passed string
	 * @param str
	 * @return
	 */
	public String getValidUserName(String str) {
		return str.indexOf("/") >= 0 ?
				 str.substring(0,str.indexOf("/"))
				:	str.indexOf("@") >= 0 ?
						str.substring(0,str.indexOf("@"))
						: str;
	}

	public static String getUTFEncodedString(String username) throws UnsupportedEncodingException {
		return URLEncoder.encode(username, StandardCharsets.UTF_8.toString());
	}

}
