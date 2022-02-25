/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.security;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

public class SecurePasswordHelperTest {

  private SecurePasswordHelper securePasswordHelper;

  @Before
  public void setUp() throws Exception {
    securePasswordHelper = new SecurePasswordHelper();
  }

  @Test
  public void testCreateSecurePassword() throws Exception {

    String password1 = securePasswordHelper.createSecurePassword();
    Assert.assertNotNull(password1);
    Assert.assertEquals(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH, password1.length());

    String password2 = securePasswordHelper.createSecurePassword();
    Assert.assertNotNull(password2);
    Assert.assertEquals(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH, password2.length());

    // Make sure the passwords are different... if they are the same, that indicated the random
    // number generators are generating using the same pattern and that is not secure.
    Assert.assertFalse((password1.equals(password2)));
  }

  @Test
  public void testCreateSecurePasswordWithRules() throws Exception {
    String password;

    //Default rules....
    password = securePasswordHelper.createSecurePassword(null, null, null, null, null, null);
    Assert.assertNotNull(password);
    Assert.assertEquals(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH, password.length());
    assertMinLowercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS, password);
    assertMinUppercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS, password);
    assertMinDigits(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_DIGITS, password);
    assertMinPunctuation(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION, password);
    assertMinWhitespace(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE, password);

    password = securePasswordHelper.createSecurePassword(10, null, null, null, null, null);
    Assert.assertNotNull(password);
    Assert.assertEquals(10, password.length());
    assertMinLowercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS, password);
    assertMinUppercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS, password);
    assertMinDigits(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_DIGITS, password);
    assertMinPunctuation(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION, password);
    assertMinWhitespace(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE, password);

    password = securePasswordHelper.createSecurePassword(0, null, null, null, null, null);
    Assert.assertNotNull(password);
    Assert.assertEquals(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH, password.length());
    assertMinLowercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS, password);
    assertMinUppercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS, password);
    assertMinDigits(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_DIGITS, password);
    assertMinPunctuation(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION, password);
    assertMinWhitespace(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE, password);

    password = securePasswordHelper.createSecurePassword(-20, null, null, null, null, null);
    Assert.assertNotNull(password);
    Assert.assertEquals(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH, password.length());
    assertMinLowercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS, password);
    assertMinUppercaseLetters(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS, password);
    assertMinDigits(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_DIGITS, password);
    assertMinPunctuation(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION, password);
    assertMinWhitespace(SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE, password);

    password = securePasswordHelper.createSecurePassword(100, 30, 20, 10, 5, 2);
    Assert.assertNotNull(password);
    Assert.assertEquals(100, password.length());
    assertMinLowercaseLetters(30, password);
    assertMinUppercaseLetters(20, password);
    assertMinDigits(10, password);
    assertMinPunctuation(5, password);
    assertMinWhitespace(2, password);

    password = securePasswordHelper.createSecurePassword(100, 20, 20, 20, 20, 0);
    Assert.assertNotNull(password);
    Assert.assertEquals(100, password.length());
    assertMinLowercaseLetters(20, password);
    assertMinUppercaseLetters(20, password);
    assertMinDigits(20, password);
    assertMinPunctuation(20, password);
    assertMinWhitespace(0, password);
  }

  private void assertMinLowercaseLetters(int minCount, String password) {
    assertMinCharacterCount(minCount, password, SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_LOWERCASE_LETTERS);
  }

  private void assertMinUppercaseLetters(int minCount, String password) {
    assertMinCharacterCount(minCount, password, SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_UPPERCASE_LETTERS);
  }

  private void assertMinDigits(int minCount, String password) {
    assertMinCharacterCount(minCount, password, SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_DIGITS);
  }

  private void assertMinPunctuation(int minCount, String password) {
    assertMinCharacterCount(minCount, password, SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_PUNCTUATION);
  }

  private void assertMinWhitespace(int minCount, String password) {
    assertMinCharacterCount(minCount, password, SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_WHITESPACE);
  }

  private void assertMinCharacterCount(int minCount, String string, char[] characters) {

    int count = 0;
    Set<Character> set = new HashSet<>();
    for(char c:characters) {
      set.add(c);
    }

    for (char c : string.toCharArray()) {
      if (set.contains(c)) {
        count++;

        if (count == minCount) {
          break;
        }
      }
    }

    Assert.assertEquals(string, minCount, count);
  }
}