/**
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package com.qlangtech.tis.manage.servlet;

import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 百岁（baisui@qlangtech.com）
 * @date 2021-03-11 13:16
 */
public class ServerJoinGroup {
  private static final Pattern IP_PATTERN = Pattern.compile("//([\\d|\\.]+)");
  private boolean leader;
  private String ipAddress;
  private String replicBaseUrl;
  private short groupIndex;
  private boolean checked;

  public void setLeader(boolean leader) {
    this.leader = leader;
  }

  public void setIpAddress(String coreUrl) {
    this.ipAddress = coreUrl;
  }

  public void setReplicBaseUrl(String baseUrl) {
    this.replicBaseUrl = baseUrl;
  }

  public String getReplicBaseUrl() {
    return this.replicBaseUrl;
  }

  public void setGroupIndex(short i) {
    this.groupIndex = i;
  }

  public void setChecked(boolean b) {
    this.checked = b;
  }

  public short getGroupIndex() {
    return this.groupIndex;
  }

  public String getIpAddress() {
    return this.ipAddress;
  }

  public String getIp() {
    Matcher matcher = IP_PATTERN.matcher(StringUtils.trimToEmpty(this.getIpAddress()));
    if (matcher.find()) {
      return matcher.group(1);
    }
    return StringUtils.EMPTY;
  }
}
