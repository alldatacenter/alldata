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
package org.apache.ambari.server.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Config;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * The {@link ConfigUpgradeChangeDefinition} represents a configuration change. This change can be
 * defined with conditional statements that will only set values if a condition
 * passes:
 * <p/>
 *
 * <pre>
 * {@code
 * <definition>
 *   <condition type="hive-site" key="hive.server2.transport.mode" value="binary">
 *     <type>hive-site</type>
 *     <key>hive.server2.thrift.port</key>
 *     <value>10010</value>
 *   </condition>
 *   <condition type="hive-site" key="hive.server2.transport.mode" value="http">
 *     <type>hive-site</type>
 *     <key>hive.server2.http.port</key>
 *     <value>10011</value>
 *   </condition>
 * </definition>
 * }
 * </pre>
 *
 * It's also possible to simple set values directly without a precondition
 * check.
 *
 * <pre>
 * {@code
 * <definition xsi:type="configure">
 *   <type>hive-site</type>
 *   <set key="hive.server2.thrift.port" value="10010"/>
 *   <set key="foo" value="bar"/>
 *   <set key="foobar" value="baz"/>
 * </definition>
 * }
 * </pre>
 *
 *
 *
 * WARNING! After adding/updating classes below, please don't forget to update 'upgrade-config.xsd' respectively
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ConfigUpgradeChangeDefinition {

  private static final Logger LOG = LoggerFactory.getLogger(ConfigUpgradeChangeDefinition.class);

  /**
   * The key that represents the configuration type to change (ie hdfs-site).
   */
  public static final String PARAMETER_CONFIG_TYPE = "configure-task-config-type";

  /**
   * Setting key/value pairs can be several per task, so they're passed in as a
   * json-ified list of objects.
   */
  public static final String PARAMETER_KEY_VALUE_PAIRS = "configure-task-key-value-pairs";

  /**
   * Transfers can be several per task, so they're passed in as a json-ified
   * list of objects.
   */
  public static final String PARAMETER_TRANSFERS = "configure-task-transfers";

  /**
   * Replacements can be several per task, so they're passed in as a json-ified list of
   * objects.
   */
  public static final String PARAMETER_REPLACEMENTS = "configure-task-replacements";

  public static final String actionVerb = "Configuring";

  public static final Float DEFAULT_PRIORITY = 1.0f;


  /**
   * An optional brief description of config changes.
   */
  @XmlAttribute(name = "summary")
  public String summary;

  @XmlAttribute(name = "id", required = true)
  public String id;

  @XmlElement(name="type")
  private String configType;

  @XmlElement(name = "set")
  private List<ConfigurationKeyValue> keyValuePairs;

  @XmlElement(name = "transfer")
  private List<Transfer> transfers;

  @XmlElement(name="replace")
  private List<Replace> replacements;

  @XmlElement(name="regex-replace")
  private List<RegexReplace> regexReplacements;
  /**
   * Insert new content into an existing value by either prepending or
   * appending. Each {@link Insert} will only run if:
   * <ul>
   * <li>The key specified by {@link Insert#key} exists.
   * <li>The content specified by {@link Insert#value} is not found in the key's
   * existing content.
   * </ul>
   */
  @XmlElement(name = "insert")
  private List<Insert> inserts;

  /**
   * @return the config type
   */
  public String getConfigType() {
    return configType;
  }

  /**
   * @return the list of <set key=foo value=bar/> items
   */
  public List<ConfigurationKeyValue> getKeyValuePairs() {
    return keyValuePairs;
  }

  /**
   * @return the list of transfers, checking for appropriate null fields.
   */
  public List<Transfer> getTransfers() {
    if (null == transfers) {
      return Collections.emptyList();
    }

    List<Transfer> list = new ArrayList<>();
    for (Transfer t : transfers) {
      switch (t.operation) {
        case COPY:
        case MOVE:
          if (null != t.fromKey && null != t.toKey) {
            list.add(t);
          } else {
            LOG.warn(String.format("Transfer %s is invalid", t));
          }
          break;
        case DELETE:
          if (null != t.deleteKey) {
            list.add(t);
          } else {
            LOG.warn(String.format("Transfer %s is invalid", t));
          }

          break;
      }
    }

    return list;
  }

  /**
   * @return the replacement tokens, never {@code null}
   */
  public List<Replace> getReplacements() {
    if (null == replacements) {
      return Collections.emptyList();
    }

    List<Replace> list = new ArrayList<>();
    for (Replace r : replacements) {

      if (StringUtils.isBlank(r.key) || StringUtils.isEmpty(r.find) || null == r.replaceWith) {
        LOG.warn(String.format("Replacement %s is invalid", r));
        continue;
      }
      list.add(r);
    }

    return list;
  }

  /**
   * Evaluates the {@link RegexReplace} instances defined for the upgrade and
   * converts them into distinct {@link Replace} objects. In some cases, if the
   * regex matches more than 1 string in the configuration, it will create
   * multiple {@link Replace} objects, each with their own literal string to
   * find/replace.
   *
   * @return the replacement tokens, never {@code null}
   */
  public List<Replace> getRegexReplacements(Cluster cluster) {
    if (null == regexReplacements) {
      return Collections.emptyList();
    }

    List<Replace> list = new ArrayList<>();
    for (RegexReplace regexReplaceObj : regexReplacements) {
      if (StringUtils.isBlank(regexReplaceObj.key) || StringUtils.isEmpty(regexReplaceObj.find)
          || null == regexReplaceObj.replaceWith) {
        LOG.warn(String.format("Replacement %s is invalid", regexReplaceObj));
        continue;
      }

      try {
        Config config = cluster.getDesiredConfigByType(configType);

        Map<String, String> properties = config.getProperties();
        String content = properties.get(regexReplaceObj.key);

        Pattern REGEX = Pattern.compile(regexReplaceObj.find, Pattern.MULTILINE);
        Matcher patternMatchObj = REGEX.matcher(content);

        if (regexReplaceObj.matchAll) {
          while (patternMatchObj.find()) {
            regexReplaceObj.find = patternMatchObj.group();
            if (StringUtils.isNotBlank(regexReplaceObj.find)) {
              Replace rep = regexReplaceObj.copyToReplaceObject();
              list.add(rep);
            }
          }
        } else {
          // find the first literal match and create a replacement for it
          if (patternMatchObj.find() && patternMatchObj.groupCount() == 1) {
            regexReplaceObj.find = patternMatchObj.group();
            Replace rep = regexReplaceObj.copyToReplaceObject();
            list.add(rep);
          }
        }

      } catch (Exception e) {
        LOG.error(String.format(
            "There was an error while trying to execute a regex replacement for %s/%s. The regular expression was %s",
            configType, regexReplaceObj.key, regexReplaceObj.find), e);
      }
    }

    return list;
  }

  /**
   * Gets the insertion directives.
   *
   * @return the inserts, or an empty list (never {@code null}).
   */
  public List<Insert> getInsertions() {
    if (null == inserts) {
      return Collections.emptyList();
    }

    return inserts;
  }

  @XmlAccessorType(XmlAccessType.FIELD)
  public static class ConditionalField{
    /**
     * The key to read for the if condition.
     */
    @XmlAttribute(name = "if-key")
    public String ifKey;

    /**
     * The config type to read for the if condition.
     */
    @XmlAttribute(name = "if-type")
    public String ifType;

    /**
     * The property value to compare against for the if condition.
     */
    @XmlAttribute(name = "if-value")
    public String ifValue;

    /**
     * Reverse search value result to opposite by allowing
     * operation to be executed when value not found
     */
    @XmlAttribute(name = "if-value-not-matched")
    public boolean ifValueNotMatched = false;

    /**
     * the way how to search the value:
     * {@code IfValueMatchType.EXACT} - full comparison
     * {@code IfValueMatchType.PARTIAL} - search for substring in string
     */
    @XmlAttribute(name = "if-value-match-type")
    public IfValueMatchType ifValueMatchType = IfValueMatchType.EXACT;

    /**
     * The property key state for the if condition
     */
    @XmlAttribute(name = "if-key-state")
    public PropertyKeyState ifKeyState;
  }

  /**
   * Used for configuration updates that should mask their values from being
   * printed in plain text.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  public static class Masked extends ConditionalField{
    @XmlAttribute(name = "mask")
    public boolean mask = false;
  }


  /**
   * A key/value pair to set in the type specified by {@link ConfigUpgradeChangeDefinition#configType}
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "set")
  public static class ConfigurationKeyValue extends Masked {
    @XmlAttribute(name = "key")
    public String key;

    @XmlAttribute(name = "value")
    public String value;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper("Set").add("key", key)
          .add("value", value)
          .add("ifKey", ifKey)
          .add("ifType", ifType)
          .add("ifValue",ifValue)
          .add("ifKeyState", ifKeyState).omitNullValues().toString();
    }
  }

  /**
   * A {@code transfer} element will copy, move, or delete the value of one type/key to another type/key.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "transfer")
  public static class Transfer extends Masked {
    /**
     * The type of operation, such as COPY or DELETE.
     */
    @XmlAttribute(name = "operation")
    public TransferOperation operation;

    /**
     * The configuration type to copy or move from.
     */
    @XmlAttribute(name = "from-type")
    public String fromType;

    /**
     * The key to copy or move the configuration from.
     */
    @XmlAttribute(name = "from-key")
    public String fromKey;

    /**
     * The key to copy the configuration value to.
     */
    @XmlAttribute(name = "to-key")
    public String toKey;

    /**
     * The configuration key to delete, or "*" for all.
     */
    @XmlAttribute(name = "delete-key")
    public String deleteKey;

    /**
     * If {@code true}, this will ensure that any changed properties are not
     * removed during a {@link TransferOperation#DELETE}.
     */
    @XmlAttribute(name = "preserve-edits")
    public boolean preserveEdits = false;

    /**
     * A default value to use when the configurations don't contain the
     * {@link #fromKey}.
     */
    @XmlAttribute(name = "default-value")
    public String defaultValue;

    /**
     * A data type to convert the configuration value to when the action is
     * {@link TransferOperation#COPY}.
     */
    @XmlAttribute(name = "coerce-to")
    public TransferCoercionType coerceTo;

    /**
     * The keys to keep when the action is {@link TransferOperation#DELETE}.
     */
    @XmlElement(name = "keep-key")
    public List<String> keepKeys = new ArrayList<>();


    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("operation", operation)
          .add("fromType", fromType)
          .add("fromKey", fromKey)
          .add("toKey", toKey)
          .add("deleteKey", deleteKey)
          .add("preserveEdits",preserveEdits)
          .add("defaultValue", defaultValue)
          .add("coerceTo", coerceTo)
          .add("ifKey", ifKey)
          .add("ifType", ifType)
          .add("ifValue", ifValue)
          .add("ifKeyState", ifKeyState)
          .add("keepKeys", keepKeys).omitNullValues().toString();
    }
  }

  /**
   * Used to replace strings in a key with other strings.  More complex
   * scenarios will be possible with regex (when needed)
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "replace")
  public static class Replace extends Masked {
    /**
     * The key name
     */
    @XmlAttribute(name="key")
    public String key;

    /**
     * The string to find
     */
    @XmlAttribute(name="find")
    public String find;

    /**
     * The string to replace
     */
    @XmlAttribute(name="replace-with")
    public String replaceWith;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("key", key)
          .add("find", find)
          .add("replaceWith", replaceWith)
          .add("ifKey", ifKey)
          .add("ifType", ifType)
          .add("ifValue", ifValue)
          .add("ifKeyState", ifKeyState).omitNullValues().toString();
    }
  }

  /**
   * Used to replace strings in a key with other strings.  More complex
   * scenarios are possible with regex.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "regex-replace")
  public static class RegexReplace extends Masked{
    /**
     * The key name
     */
    @XmlAttribute(name="key")
    public String key;

    /**
     * The string to find
     */
    @XmlAttribute(name="find")
    public String find;

    /**
     * The string to replace
     */
    @XmlAttribute(name="replace-with")
    public String replaceWith;

    /**
     * Find as many matching groups as possible and create replacements for each
     * one. The default value is {@code false}.
     */
    @XmlAttribute(name = "match-all")
    public boolean matchAll = false;

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("key", key)
          .add("find", find)
          .add("replaceWith",replaceWith)
          .add("ifKey", ifKey)
          .add("ifType", ifType)
          .add("ifValue", ifValue)
          .add("ifKeyState", ifKeyState).omitNullValues().toString();
    }

    /***
     * Copies a RegexReplace type object to Replace object.
     * @return Replace object
     */
    public Replace copyToReplaceObject(){
      Replace rep = new Replace();
      rep.find = find;
      rep.key = key;
      rep.replaceWith = replaceWith;
      rep.ifKey = ifKey;
      rep.ifType = ifType;
      rep.ifValue = ifValue;
      rep.ifKeyState = ifKeyState;

      return rep;
    }
  }

  /**
   * Used to replace strings in a key with other strings. More complex scenarios
   * will be possible with regex (when needed). If the value specified in
   * {@link Insert#value} already exists, then it is not inserted again.
   */
  @XmlAccessorType(XmlAccessType.FIELD)
  @XmlType(name = "insert")
  public static class Insert extends Masked{
    /**
     * The key name
     */
    @XmlAttribute(name = "key", required = true)
    public String key;

    /**
     * The value to insert.
     */
    @XmlAttribute(name = "value", required = true)
    public String value;

    /**
     * The value to insert.
     */
    @XmlAttribute(name = "insert-type", required = true)
    public InsertType insertType = InsertType.APPEND;

    /**
     * {@code true} to insert a new line before inserting the {@link #value}.
     */
    @XmlAttribute(name = "newline-before")
    public boolean newlineBefore = false;

    /**
     * {@code true} to insert a new line after inserting the {@link #value}.
     */
    @XmlAttribute(name = "newline-after")
    public boolean newlineAfter = false;

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("insertType", insertType)
          .add("key", key)
          .add("value",value)
          .add("newlineBefore", newlineBefore)
          .add("newlineAfter", newlineAfter).omitNullValues().toString();
    }
  }

  /**
   * The {@link InsertType} defines how to use the {@link Insert} directive.
   */
  @XmlEnum
  public enum InsertType {
    /**
     * Prepend the content.
     */
    @XmlEnumValue("prepend")
    PREPEND,

    /**
     * Append the content.
     */
    @XmlEnumValue("append")
    APPEND
  }

  /**
   * The {@link IfValueMatchType} defines value search behaviour
   */
  @XmlEnum
  public enum IfValueMatchType {
    /**
     * Exact value match
     */
    @XmlEnumValue("exact")
    EXACT,

    /**
     * partial value match
     */
    @XmlEnumValue("partial")
    PARTIAL
  }

}
