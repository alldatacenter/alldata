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

package org.apache.ambari.server.controller.logging;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * This class represents a single entry from a LogSearch query.
 *
 */
// annotate this class, so that Jackson will
// ignore any extra properties in the response that
// the integration code does not need at this time
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogLineResult {

  private final Map<String, String> mapOfLogLineProperties =
    new HashMap<>();

  private String clusterName;

  private String logMethod;

  private String logLevel;

  private String eventCount;

  private String ipAddress;

  private String componentType;

  private String sequenceNumber;

  private String logFilePath;

  private String sourceFile;

  private String sourceFileLineNumber;

  private String hostName;

  private String logMessage;

  private String loggerName;

  private String id;

  private String messageMD5;

  private String logTime;

  private String eventMD5;

  private String logFileLineNumber;

  private String ttl;

  private String expirationTime;

  private String version;

  private String thread_name;

  public LogLineResult() {

  }

  public LogLineResult(Map<String, String> propertiesMap) {
    mapOfLogLineProperties.putAll(propertiesMap);
  }

  public String getProperty(String propertyName) {
    return mapOfLogLineProperties.get(propertyName);
  }

  public boolean doesPropertyExist(String propertyName) {
    return mapOfLogLineProperties.containsKey(propertyName);
  }


  @JsonProperty("cluster")
  public String getClusterName() {
    return clusterName;
  }

  @JsonProperty("cluster")
  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  @JsonProperty("method")
  public String getLogMethod() {
    return logMethod;
  }

  @JsonProperty("method")
  public void setLogMethod(String logMethod) {
    this.logMethod = logMethod;
  }

  @JsonProperty("level")
  public String getLogLevel() {
    return logLevel;
  }

  @JsonProperty("level")
  public void setLogLevel(String logLevel) {
    this.logLevel = logLevel;
  }

  @JsonProperty("event_count")
  public String getEventCount() {
    return eventCount;
  }

  @JsonProperty("event_count")
  public void setEventCount(String eventCount) {
    this.eventCount = eventCount;
  }

  @JsonProperty("ip")
  public String getIpAddress() {
    return ipAddress;
  }

  @JsonProperty("ip")
  public void setIpAddress(String ipAddress) {
    this.ipAddress = ipAddress;
  }

  @JsonProperty("type")
  public String getComponentType() {
    return componentType;
  }

  @JsonProperty("type")
  public void setComponentType(String componentType) {
    this.componentType = componentType;
  }

  @JsonProperty("seq_num")
  public String getSequenceNumber() {
    return sequenceNumber;
  }

  @JsonProperty("seq_num")
  public void setSequenceNumber(String sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  @JsonProperty("path")
  public String getLogFilePath() {
    return logFilePath;
  }

  @JsonProperty("path")
  public void setLogFilePath(String logFilePath) {
    this.logFilePath = logFilePath;
  }

  @JsonProperty("file")
  public String getSourceFile() {
    return sourceFile;
  }

  @JsonProperty("file")
  public void setSourceFile(String sourceFile) {
    this.sourceFile = sourceFile;
  }

  @JsonProperty("line_number")
  public String getSourceFileLineNumber() {
    return sourceFileLineNumber;
  }

  @JsonProperty("line_number")
  public void setSourceFileLineNumber(String sourceFileLineNumber) {
    this.sourceFileLineNumber = sourceFileLineNumber;
  }

  @JsonProperty("host")
  public String getHostName() {
    return hostName;
  }

  @JsonProperty("host")
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  @JsonProperty("log_message")
  public String getLogMessage() {
    return logMessage;
  }

  @JsonProperty("log_message")
  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  @JsonProperty("logger_name")
  public String getLoggerName() {
    return loggerName;
  }

  @JsonProperty("logger_name")
  public void setLoggerName(String loggerName) {
    this.loggerName = loggerName;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("message_md5")
  public String getMessageMD5() {
    return messageMD5;
  }

  @JsonProperty("message_md5")
  public void setMessageMD5(String messageMD5) {
    this.messageMD5 = messageMD5;
  }

  @JsonProperty("logtime")
  public String getLogTime() {
    return logTime;
  }

  @JsonProperty("logtime")
  public void setLogTime(String logTime) {
    this.logTime = logTime;
  }

  @JsonProperty("event_md5")
  public String getEventMD5() {
    return eventMD5;
  }

  @JsonProperty("event_md5")
  public void setEventMD5(String eventMD5) {
    this.eventMD5 = eventMD5;
  }

  @JsonProperty("logfile_line_number")
  public String getLogFileLineNumber() {
    return logFileLineNumber;
  }

  @JsonProperty("logfile_line_number")
  public void setLogFileLineNumber(String logFileLineNumber) {
    this.logFileLineNumber = logFileLineNumber;
  }

  @JsonProperty("_ttl_")
  public String getTtl() {
    return ttl;
  }

  @JsonProperty("_ttl_")
  public void setTtl(String ttl) {
    this.ttl = ttl;
  }

  @JsonProperty("_expire_at_")
  public String getExpirationTime() {
    return expirationTime;
  }

  @JsonProperty("_expire_at_")
  public void setExpirationTime(String expirationTime) {
    this.expirationTime = expirationTime;
  }

  @JsonProperty("_version_")
  public String getVersion() {
    return version;
  }

  @JsonProperty("_version_")
  public void setVersion(String version) {
    this.version = version;
  }

  @JsonProperty("thread_name")
  public String getThreadName() {
    return thread_name;
  }

  @JsonProperty("thread_name")
  public void setThreadName(String thread_name) {
    this.thread_name = thread_name;
  }
}
