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

package org.apache.ranger.security.handler;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;

public class RangerCustomPasswordEncoder implements PasswordEncoder {

	private final String salt;
	private final String algorithm;

	public RangerCustomPasswordEncoder(String salt, String algorithm) {
		this.salt = salt;
		this.algorithm = algorithm;
	}

	@Override
	public String encode(CharSequence rawPassword) {
		try {
			String saltedPassword = mergeTextAndSalt(rawPassword, this.salt, false);
			MessageDigest digest = MessageDigest.getInstance(this.algorithm);
			return new String(Hex.encode(digest.digest(saltedPassword.getBytes("UTF-8"))));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("UTF-8 not supported");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Algorithm " + algorithm + " not supported");
		}
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		return this.encode(rawPassword).equals(encodedPassword);
	}

	private String mergeTextAndSalt(CharSequence text, Object salt, boolean strict) {
		if (text == null) {
			text = "";
		}

		if ((strict) && (salt != null) && ((salt.toString().lastIndexOf("{") != -1) || (salt.toString().lastIndexOf("}") != -1))) {
			throw new IllegalArgumentException("Cannot use { or } in salt.toString()");
		}

		if ((salt == null) || ("".equals(salt))) {
			return text.toString();
		}
		return text + "{" + salt.toString() + "}";
	}

}