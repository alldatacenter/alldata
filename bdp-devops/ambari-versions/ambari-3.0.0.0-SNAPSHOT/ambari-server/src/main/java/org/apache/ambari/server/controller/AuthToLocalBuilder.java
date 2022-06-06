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

package org.apache.ambari.server.controller;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * AuthToLocalBuilder helps to create auth_to_local rules for use in configuration files like
 * core-site.xml.  No duplicate rules will be generated.
 * <p/>
 * Allows previously existing rules to be added verbatim.  Also allows new rules to be generated
 * based on a principal and local username.  For each principal added to the builder, generate
 * a rule conforming to one of the following formats:
 * <p/>
 * Qualified Principal (the principal contains a user and host):
 * RULE:[2:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 * <p/>
 * Unqualified Principal (only user is specified):
 * RULE:[1:$1@$0](PRIMARY@REALM)s/.*\/LOCAL_USERNAME/
 * <p/>
 * Additionally, for each realm included in the rule set, generate a default realm rule
 * in the format: RULE:[1:$1@$0](.*@REALM)s/@.{@literal *}//
 * <p/>
 * Ordering guarantees for the generated rule string are as follows:
 * <ul>
 * <li>Rules with the same expected component count are ordered according to match component count</li>
 * <li>Rules with different expected component count are ordered according to the default string ordering</li>
 * <li>Rules in the form of .*@REALM are ordered after all other rules with the same expected component count</li>
 * </ul>
 */
public class AuthToLocalBuilder implements Cloneable {
  public static final ConcatenationType DEFAULT_CONCATENATION_TYPE = ConcatenationType.NEW_LINES;

  /**
   * Ordered set of rules which have been added to the builder.
   */
  private Set<Rule> setRules = new TreeSet<>();

  /**
   * The default realm.
   */
  private final String defaultRealm;

  /**
   * A set of additional realm names to reference when generating rules.
   */
  private final Set<String> additionalRealms;


  /**
   * A flag indicating whether case insensitive support to the local username has been requested. This will append an //L switch to the generic realm rule
   */
  private boolean caseInsensitiveUser;

  /**
   * Constructs a new AuthToLocalBuilder.
   *
   * @param defaultRealm               a String declaring the default realm
   * @param additionalRealms           a String containing a comma-delimited list of realm names
   *                                   to incorporate into the generated rule set
   * @param caseInsensitiveUserSupport true indicating that case-insensitivity should be enabled;
   *                                   false otherwise
   */
  public AuthToLocalBuilder(String defaultRealm, String additionalRealms, boolean caseInsensitiveUserSupport) {
    this(defaultRealm, splitDelimitedString(additionalRealms), caseInsensitiveUserSupport);
  }

  /**
   * Constructs a new AuthToLocalBuilder.
   *
   * @param defaultRealm               a String declaring the default realm
   * @param additionalRealms           a collection of Strings declaring the set of realm names to
   *                                   incorporate into the generated rule set
   * @param caseInsensitiveUserSupport true indicating that case-insensitivity should be enabled;
   *                                   false otherwise
   */
  public AuthToLocalBuilder(String defaultRealm, Collection<String> additionalRealms, boolean caseInsensitiveUserSupport) {
    this.defaultRealm = defaultRealm;

    this.additionalRealms = (additionalRealms == null)
        ? Collections.emptySet()
        : Collections.unmodifiableSet(new HashSet<>(additionalRealms));

    this.caseInsensitiveUser = caseInsensitiveUserSupport;
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    AuthToLocalBuilder copy = (AuthToLocalBuilder) super.clone();

    /* **** Copy mutable members **** */
    copy.setRules = new TreeSet<>(setRules);

    return copy;
  }

  /**
   * Add existing rules from the given authToLocal configuration property.
   * The rules are added verbatim.
   *
   * @param authToLocalRules config property value containing the existing rules
   */
  public AuthToLocalBuilder addRules(String authToLocalRules) {
    if (!StringUtils.isEmpty(authToLocalRules)) {
      String[] rules = authToLocalRules.split("RULE:|DEFAULT");
      for (String r : rules) {
        r = r.trim();
        if (!r.isEmpty()) {
          Rule rule = createRule(r);
          if (!setRules.contains(rule.caseSensitivityInverted())) {
            setRules.add(rule);
          }
        }
      }
    }
    return this;
  }

  /**
   * Adds a rule for the given principal and local user.
   * The principal must contain a realm component.
   * <p/>
   * The supplied principal is parsed to determine if it is qualified or unqualified and stored
   * accordingly so that when the mapping rules are generated the appropriate rule is generated.
   * <p/>
   * If a principal is added that yields a duplicate primary principal value (relative to the set of
   * qualified or unqualified rules), that later entry will overwrite the older entry, allowing for
   * only one mapping rule.
   * <p/>
   * If the principal does not match one of the two expected patterns, it will be ignored.
   *
   * @param principal     a string containing the full principal
   * @param localUsername a string declaring that local username to map the principal to
   * @throws IllegalArgumentException if the provided principal doesn't contain a realm element
   */
  public AuthToLocalBuilder addRule(String principal, String localUsername) {
    if (!StringUtils.isEmpty(principal) && !StringUtils.isEmpty(localUsername)) {
      Principal p = new Principal(principal);
      if (p.getRealm() == null) {
        throw new IllegalArgumentException(
            "Attempted to add a rule for a principal with no realm: " + principal);
      }

      Rule rule = createHostAgnosticRule(p, localUsername);
      setRules.add(rule);
    }
    return this;
  }

  /**
   * Generates the auth_to_local rules used by configuration settings such as core-site/auth_to_local.
   * <p/>
   * Each rule is concatenated using the default ConcatenationType, like calling
   * {@link #generate(ConcatenationType)} with {@link #DEFAULT_CONCATENATION_TYPE}
   *
   * @return a string containing the generated auth-to-local rule set
   */
  public String generate() {
    return generate(null);
  }

  /**
   * Generates the auth_to_local rules used by configuration settings such as core-site/auth_to_local.
   * <p/>
   * Each rule is concatenated using the specified
   * {@link org.apache.ambari.server.controller.AuthToLocalBuilder.ConcatenationType}.
   * If the concatenation type is <code>null</code>, the default concatenation type is assumed -
   * see {@link #DEFAULT_CONCATENATION_TYPE}.
   *
   * @param concatenationType the concatenation type to use to generate the rule set string
   * @return a string containing the generated auth-to-local rule set
   */
  public String generate(ConcatenationType concatenationType) {
    StringBuilder builder = new StringBuilder();
    // ensure that a default rule is added for this realm
    if (!StringUtils.isEmpty(defaultRealm)) {
      // Remove existing default rule.... this is in the event we are switching case sensitivity...
      setRules.remove(createDefaultRealmRule(defaultRealm, !caseInsensitiveUser));
      // Add (new) default rule....
      setRules.add(createDefaultRealmRule(defaultRealm, caseInsensitiveUser));
    }

    // ensure that a default realm rule is added for the specified additional realms
    for (String additionalRealm : additionalRealms) {
      // Remove existing default rule.... this is in the event we are switching case sensitivity...
      setRules.remove(createDefaultRealmRule(additionalRealm, !caseInsensitiveUser));
      // Add (new) default rule....
      setRules.add(createDefaultRealmRule(additionalRealm, caseInsensitiveUser));
    }

    if (concatenationType == null) {
      concatenationType = DEFAULT_CONCATENATION_TYPE;
    }

    for (Rule rule : setRules) {
      appendRule(builder, rule.toString(), concatenationType);
    }

    appendRule(builder, "DEFAULT", concatenationType);
    return builder.toString();
  }

  /**
   * Append a rule to the given string builder.
   *
   * @param stringBuilder     string builder to which rule is added
   * @param rule              rule to add
   * @param concatenationType the concatenation type to use to generate the rule set string
   */
  private void appendRule(StringBuilder stringBuilder, String rule, ConcatenationType concatenationType) {
    if (stringBuilder.length() > 0) {
      switch (concatenationType) {
        case NEW_LINES:
          stringBuilder.append('\n');
          break;
        case NEW_LINES_ESCAPED:
          stringBuilder.append("\\\n");
          break;
        case SPACES:
          stringBuilder.append(" ");
          break;
        case COMMA:
          stringBuilder.append(",");
          break;
        default:
          throw new UnsupportedOperationException(String.format("The auth-to-local rule concatenation type is not supported: %s",
              concatenationType.name()));
      }
    }

    stringBuilder.append(rule);
  }

  /**
   * Create a rule that expects 2 components in the principal and ignores hostname in the comparison.
   *
   * @param principal principal
   * @param localUser local user
   * @return a new rule that ignores hostname in the comparison
   */
  private Rule createHostAgnosticRule(Principal principal, String localUser) {
    List<String> principalComponents = principal.getComponents();
    int componentCount = principalComponents.size();
    return new Rule(
      MatchingRule.ignoreHostWhenComponentCountIs(componentCount),
      new Principal(principal.getComponent(1) + "@" + principal.getRealm()),
      new Substitution(".*", localUser, "", false));
  }

  /**
   * Create a default rule for a realm which matches all principals with 1 component and the same realm.
   *
   * @param realm realm that the rule is being created for
   * @param caseInsensitive true if the rule should be case-insensitive; otherwise false
   * @return a new default realm rule
   */
  private Rule createDefaultRealmRule(String realm, boolean caseInsensitive) {
    return new Rule(
      MatchingRule.ignoreHostWhenComponentCountIs(1),
      new Principal(".*@" + realm),
      new Substitution("@.*", "", "", caseInsensitive));
  }

  /**
   * Create a rule from an existing string representation.
   *
   * @param rule string representation of a rule
   * @return a new rule which matches the provided string representation
   */
  private Rule createRule(String rule) {
    return Rule.parse(rule.startsWith("RULE:") ? rule : String.format("RULE:%s", rule));
  }

  /**
   * Given a comma or line delimited list of strings, returns a collection of non-empty strings.
   *
   * @param string a string to split
   * @return an array of non-empty strings or null if the source string is empty or null
   */
  private static Collection<String> splitDelimitedString(String string) {
    Collection<String> collection = null;

    if (!StringUtils.isEmpty(string)) {
      collection = new HashSet<>();

      for (String realm : string.split("\\s*(?:\\r?\\n|,)\\s*")) {
        realm = realm.trim();
        if (!realm.isEmpty()) {
          collection.add(realm);
        }
      }
    }

    return collection;
  }

  /**
   * I represent an auth-to-local rule that maps a principal of the form username/hostname@REALM to username.
   */
  private static class Rule implements Comparable<Rule> {
    private static final Pattern PATTERN_RULE_PARSE =
      Pattern.compile("RULE:\\s*\\[\\s*(\\d)\\s*:\\s*(.+?)(?:@(.+?))??\\s*\\]\\s*\\((.+?)\\)\\s*s/(.*?)/(.*?)/([a-zA-Z]*)((/L)?)(?:.|\n)*");
    private final MatchingRule matchingRule;
    private final Principal principal;
    private final Substitution substitution;

    /**
     * @param rule in the following format RULE:[n:string](regexp)s/pattern/replacement/[modifier]/[L]
     */
    public static Rule parse(String rule) {
      Matcher m = PATTERN_RULE_PARSE.matcher(rule);
      if (!m.matches()) {
        throw new IllegalArgumentException("Invalid rule: " + rule);
      }
      int expectedComponentCount = Integer.parseInt(m.group(1));
      String matchPattern = m.group(2);
      String optionalPatternRealm = m.group(3);
      String matchingRegexp = m.group(4);
      String replacementPattern = m.group(5);
      String replacementReplacement = m.group(6);
      String replacementModifier = m.group(7);
      String caseSensitivity = m.group(8);
      return new Rule(
        new MatchingRule(expectedComponentCount, matchPattern, optionalPatternRealm),
        new Principal(matchingRegexp),
        new Substitution(replacementPattern, replacementReplacement, replacementModifier, !caseSensitivity.isEmpty()));
    }

    public Rule(MatchingRule matchingRule, Principal principal, Substitution substitution) {
      this.matchingRule = matchingRule;
      this.principal = principal;
      this.substitution = substitution;
    }

    @Override
    public String toString() {
      return String.format("RULE:%s(%s)%s", matchingRule, principal, substitution);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Rule rule = (Rule) o;
      return new EqualsBuilder()
        .append(matchingRule, rule.matchingRule)
        .append(principal, rule.principal)
        .append(substitution, rule.substitution)
        .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
        .append(matchingRule)
        .append(principal)
        .append(substitution)
        .toHashCode();
    }

    /**
     * Compares rules.
     * <p/>
     * For rules with different expected component counts, the default string comparison is used.
     * For rules with the same expected component count rules are ordered so that rules with a higher
     * match component count occur first.
     * <p/>
     * For rules with the same expected component count, default realm rules in the form of
     * .*@myRealm.com are ordered last.
     *
     * @param other the other rule to compare
     * @return a negative integer, zero, or a positive integer as this object is less than,
     * equal to, or greater than the specified object
     */
    @Override
    public int compareTo(Rule other) {
      int retVal = matchingRule.expectedComponentCount - other.matchingRule.expectedComponentCount;
      if (retVal == 0) {
        retVal = other.matchingRule.matchComponentCount() - matchingRule.matchComponentCount();
        if (retVal == 0) {
          if (this.principal.equals(other.principal)) {
            retVal = toString().compareTo(other.toString());
          } else {
            // check for wildcard realms '.*'
            String realm = this.principal.getRealm();
            String otherRealm = other.principal.getRealm();
            retVal = compareValueWithWildcards(realm, otherRealm);
            if (retVal == 0) {
              for (int i = 1; i <= matchingRule.matchComponentCount(); i++) {
                // check for wildcard component
                String component1 = this.principal.getComponent(1);
                String otherComponent1 = other.principal.getComponent(1);
                retVal = compareValueWithWildcards(component1, otherComponent1);

                if (retVal != 0) {
                  break;
                }
              }
            }
          }
        }
      }
      return retVal;
    }

    /**
     * Compares 2 strings for use in compareTo methods but orders <code>null</code>s first and wildcards last.
     * <p/>
     * Rules:
     * <ul>
     * <li><code>null</code> is ordered before any other string except for <code>null</code>, which is considered be equal</li>
     * <li><code>.*</code> is ordered after any other string except for <code>.*</code>, which is considered equal</li>
     * <li>All other values are order based on the result of {@link String#compareTo(String)}</li>
     * </ul>
     *
     * @param s1 the first string to be compared.
     * @param s2 the second string to be compared.
     * @return a negative integer, zero, or a positive integer as the first argument is less than,
     * equal to, or greater than the second.
     * @see Comparable#compareTo(Object)
     */
    private int compareValueWithWildcards(String s1, String s2) {
      if (s1 == null) {
        if (s2 == null) {
          return 0;
        } else {
          return -1;
        }
      } else if (s2 == null) {
        return 1;
      } else if (s1.equals(s2)) {
        return 0;
      } else if (s1.equals(".*")) {
        return 1;
      } else if (s2.equals(".*")) {
        return -1;
      } else {
        return s1.compareTo(s2);
      }
    }

    public Rule caseSensitivityInverted() {
      return new Rule(matchingRule, principal, substitution.caseSensitivityInverted());
    }
  }

  /**
   * The matching rule part of an auth-to-local rule: [n:string]
   * Indicates a matching rule where n declares the number of expected components in the principal.
   * Components are separated by a /, where a user account has one component (ambari-qa) and a service account has two components (nn/fqdn).
   * The string value declares how to reformat the value to be used in the rest of the expression.
   * The placeholders are as follows:
   *  $0 - realm
   *  $1 - 1st component
   *  $2 - 2nd component
   *  For example: [2:$1@$0] matches on nn/c6501.ambari.apache.org@EXAMPLE.COM and translates to nn@EXAMPLE.COM
   */
  private static class MatchingRule {
    private final int expectedComponentCount;
    private final String matchPattern;
    private final String realmPattern;

    public static MatchingRule ignoreHostWhenComponentCountIs(int expectedComponentCount) {
      return new MatchingRule(expectedComponentCount, "$1", "$0");
    }

    public MatchingRule(int expectedComponentCount, String matchPattern, @Nullable String realmPattern) {
      this.expectedComponentCount = expectedComponentCount;
      this.matchPattern = matchPattern;
      this.realmPattern = realmPattern;
    }

    /**
     * Get the match component count. This is the number of components that are evaluated
     * when attempting to match a principal to the rule.
     */
    public int matchComponentCount() {
      return (matchPattern.startsWith("$")
        ? matchPattern.substring(1)
        : matchPattern).split("\\$").length;
    }

    @Override
    public String toString() {
      return realmPattern != null
        ? String.format("[%d:%s@%s]", expectedComponentCount, matchPattern, realmPattern)
        : String.format("[%d:%s]", expectedComponentCount, matchPattern);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MatchingRule that = (MatchingRule) o;
      return new EqualsBuilder()
        .append(expectedComponentCount, that.expectedComponentCount)
        .append(matchPattern, that.matchPattern)
        .append(realmPattern, that.realmPattern)
        .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
        .append(expectedComponentCount)
        .append(matchPattern)
        .append(realmPattern)
        .toHashCode();
    }
  }

  /**
   * I'm the substitution part of an auth-to-local rule.
   * I have 4 parts:
   *  s/pattern/replacement/g/L where the last 2 parts are optional.
   * The pattern part of this expression is a regular expression used to find the portion of the string to replace.
   * The replacement part of this expression is the value to use for replacing the matched section.
   * If g is specified after the last /, the replacements will occur for every match in the value, else only the first match is processed.
   */
  private static class Substitution {
    private final String pattern;
    private final String replacement;
    private final String modifier;
    private final boolean caseInsensitiveUser;

    public Substitution(String pattern, String replacement, String modifier, boolean caseInsensitiveUser) {
      this.pattern = pattern;
      this.replacement = replacement;
      this.modifier = modifier;
      this.caseInsensitiveUser = caseInsensitiveUser;
    }

    @Override
    public String toString() {
      return String.format(
        "s/%s/%s/%s%s",
        pattern,
        replacement,
        modifier,
        caseInsensitiveUser ? "/L" : "");
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Substitution that = (Substitution) o;
      return new EqualsBuilder()
        .append(caseInsensitiveUser, that.caseInsensitiveUser)
        .append(pattern, that.pattern)
        .append(replacement, that.replacement)
        .append(modifier, that.modifier)
        .isEquals();
    }

    @Override
    public int hashCode() {
      return new HashCodeBuilder(17, 37)
        .append(pattern)
        .append(replacement)
        .append(modifier)
        .append(caseInsensitiveUser)
        .toHashCode();
    }

    public Substitution caseSensitivityInverted() {
      return new Substitution(pattern, replacement, modifier, !caseInsensitiveUser);
    }
  }

  /**
   * Principal implementation.
   */
  private static class Principal {

    /**
     * principal pattern which allows for null realm
     */
    private static final Pattern p = Pattern.compile("([^@]+)(?:@(.*))?");

    /**
     * string representation
     */
    private String principal;

    /**
     * associated realm
     */
    private String realm;

    /**
     * list of components in the principal not including the realm
     */
    private List<String> components;

    /**
     * Constructor.
     *
     * @param principal string representation of the principal
     */
    public Principal(String principal) {
      this.principal = principal;

      Matcher m = p.matcher(principal);

      if (m.matches()) {
        String allComponents = m.group(1);
        if (allComponents == null) {
          components = Collections.emptyList();
        } else {
          allComponents = allComponents.startsWith("/") ? allComponents.substring(1) : allComponents;
          components = Arrays.asList(allComponents.split("/"));
        }
        realm = m.group(2);
      } else {
        throw new IllegalArgumentException("Invalid Principal: " + principal);
      }
    }

    /**
     * Get all of the components which make up the principal.
     *
     * @return list of principal components
     */
    public List<String> getComponents() {
      return components;
    }

    /**
     * Get the component at the specified location.
     * Uses the range 1-n to match the notation used in the rule.
     *
     * @param position position of the component in the range 1-n
     * @return the component at the specified location or null
     */
    public String getComponent(int position) {
      if (position > components.size()) {
        return null;
      } else {
        return components.get(position - 1);
      }
    }

    /**
     * Get the associated realm.
     *
     * @return the associated realm
     */
    public String getRealm() {
      return realm;
    }

    @Override
    public String toString() {
      return principal;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Principal principal1 = (Principal) o;

      return components.equals(principal1.components) &&
          principal.equals(principal1.principal) &&
          !(realm != null ?
              !realm.equals(principal1.realm) :
              principal1.realm != null);

    }

    @Override
    public int hashCode() {
      int result = principal.hashCode();
      result = 31 * result + (realm != null ? realm.hashCode() : 0);
      result = 31 * result + components.hashCode();
      return result;
    }
  }

  /**
   * ConcatenationType is an enumeration of auth-to-local rule concatenation types.
   */
  public enum ConcatenationType {
    /**
     * Each rule is appended to the set of rules on a new line (<code>\n</code>)
     */
    NEW_LINES,
    /**
     * Each rule is appended to the set of rules on a new line, escaped using a \ (<code>\\n</code>)
     */
    NEW_LINES_ESCAPED,
    /**
     * Each rule is appended to the set of rules using a space - the ruleset exists on a single line
     */
    SPACES,
    /**
     * Each rule is appended to the set of rules using comma - the ruleset exists on a single line.
     */
    COMMA;
    /**
     * Translate a string declaring a concatenation type to the enumerated value.
     * <p/>
     * If the string value is <code>null</code> or empty, return the default type - {@link #NEW_LINES}.
     *
     * @param value a value to translate
     * @return a ConcatenationType
     */
    public static ConcatenationType translate(String value) {
      if (value != null) {
        value = value.trim();

        if (!value.isEmpty()) {
          return valueOf(value.toUpperCase());
        }
      }

      return DEFAULT_CONCATENATION_TYPE;
    }
  }
}
