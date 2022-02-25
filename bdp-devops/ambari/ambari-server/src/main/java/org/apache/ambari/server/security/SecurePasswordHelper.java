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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Singleton;

@Singleton
public class SecurePasswordHelper {

  /**
   * The default number of characters to generate for a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_LENGTH = 18;
  /**
   * The default minimum number of lowercase letters to include when generating a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS = 1;
  /**
   * The default minimum number of uppercase letters to include when generating a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS = 1;
  /**
   * The default minimum number of digits to include when generating a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_MIN_DIGITS = 1;
  /**
   * The default minimum number of punctuation characters to include when generating a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION = 1;
  /**
   * The default minimum number of whitespace characters to include when generating a secure password
   */
  public final static int DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE = 1;

  /**
   * The set of available lowercase letters to use when generating a secure password
   */
  final static char[] SECURE_PASSWORD_CHARACTER_CLASS_LOWERCASE_LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
  /**
   * The set of available uppercase letters to use when generating a secure password
   */
  final static char[] SECURE_PASSWORD_CHARACTER_CLASS_UPPERCASE_LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
  /**
   * The set of available digits to use when generating a secure password
   */
  final static char[] SECURE_PASSWORD_CHARACTER_CLASS_DIGITS = "0123456789".toCharArray();
  /**
   * The set of available punctuation characters to use when generating a secure password
   */
  final static char[] SECURE_PASSWORD_CHARACTER_CLASS_PUNCTUATION = "?.!$%^*()-_+=~".toCharArray();
  /**
   * The set of available whitespace characters to use when generating a secure password
   */
  final static char[] SECURE_PASSWORD_CHARACTER_CLASS_WHITESPACE = " ".toCharArray();
  /**
   * The collection of available character classes
   */
  private final static char[][] SECURE_PASSWORD_CHARACTER_CLASSES = {
      SECURE_PASSWORD_CHARACTER_CLASS_LOWERCASE_LETTERS,
      SECURE_PASSWORD_CHARACTER_CLASS_UPPERCASE_LETTERS,
      SECURE_PASSWORD_CHARACTER_CLASS_DIGITS,
      SECURE_PASSWORD_CHARACTER_CLASS_PUNCTUATION,
      SECURE_PASSWORD_CHARACTER_CLASS_WHITESPACE
  };
  
  private final SecureRandom secureRandom = new SecureRandom();

  /**
   * Create a secure (random) password using a secure random number generator and a set of (reasonable)
   * characters.
   * <p/>
   * The default rules are used to generate the password. See {@link #createSecurePassword(Integer, Integer, Integer, Integer, Integer, Integer)}
   *
   * @return a String containing the new password
   * @see #createSecurePassword(Integer, Integer, Integer, Integer, Integer, Integer)
   */
  public String createSecurePassword() {
    return createSecurePassword(null, null, null, null, null, null);
  }

  /**
   * Create a secure (random) password using a secure random number generator, a set of (reasonable)
   * characters, and meeting the specified rules.
   * <p/>
   * If any rule is <code>null</code>, it's default value will be used:
   * <ul>
   * <li>length: 18</li>
   * <li>minimum lowercase letters (a-z): 1</li>
   * <li>minimum uppercase letters (A-Z): 1</li>
   * <li>minimum digits (0-9): 1</li>
   * <li>minimum punctuation (?.!$%^*()-_+=~): 1</li>
   * <li>minimum whitespace ( ): 0</li>
   * </ul>
   *
   * @param length              the required length of the generated password
   * @param minLowercaseLetters the required minimum number of lowercase letters
   * @param minUppercaseLetters the required minimum number of uppercase letters
   * @param minDigits           the required minimum number of digits
   * @param minPunctuation      the required minimum number of punctuation characters
   * @param minWhitespace       the required minimum number of space characters
   * @return a String containing the new password
   */
  public String createSecurePassword(Integer length, Integer minLowercaseLetters, Integer minUppercaseLetters, Integer minDigits, Integer minPunctuation, Integer minWhitespace) {
    if ((length == null) || (length < 1)) {
      length = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_LENGTH;
    }

    if (minLowercaseLetters == null) {
      minLowercaseLetters = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_LOWERCASE_LETTERS;
    }

    if (minUppercaseLetters == null) {
      minUppercaseLetters = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_UPPERCASE_LETTERS;
    }

    if (minDigits == null) {
      minDigits = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_DIGITS;
    }

    if (minPunctuation == null) {
      minPunctuation = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_PUNCTUATION;
    }

    if (minWhitespace == null) {
      minWhitespace = SecurePasswordHelper.DEFAULT_SECURE_PASSWORD_MIN_WHITESPACE;
    }

    // Gather the set of characters that meet the specified requirements
    List<Character> characters = new ArrayList<>(length);

    for (int i = 0; i < minLowercaseLetters; i++) {
      characters.add(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_LOWERCASE_LETTERS[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_LOWERCASE_LETTERS.length)]);
    }

    for (int i = 0; i < minUppercaseLetters; i++) {
      characters.add(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_UPPERCASE_LETTERS[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_UPPERCASE_LETTERS.length)]);
    }

    for (int i = 0; i < minDigits; i++) {
      characters.add(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_DIGITS[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_DIGITS.length)]);
    }

    for (int i = 0; i < minPunctuation; i++) {
      characters.add(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_PUNCTUATION[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_PUNCTUATION.length)]);
    }

    for (int i = 0; i < minWhitespace; i++) {
      characters.add(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_WHITESPACE[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASS_WHITESPACE.length)]);
    }

    // If we need to gather more characters, select randomly from the set of character classes
    if (characters.size() < length) {
      int difference = length - characters.size();
      for (int i = 0; i < difference; i++) {
        char[] characterClass = SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASSES[secureRandom.nextInt(SecurePasswordHelper.SECURE_PASSWORD_CHARACTER_CLASSES.length - 1)];
        characters.add(characterClass[secureRandom.nextInt(characterClass.length)]);
      }
    }

    // Generate the password string by randomly selecting from the list of available characters
    StringBuilder passwordBuilder = new StringBuilder(characters.size());

    while (!characters.isEmpty()) {
      passwordBuilder.append(characters.remove(secureRandom.nextInt(characters.size())));
    }

    return passwordBuilder.toString();
  }

}
