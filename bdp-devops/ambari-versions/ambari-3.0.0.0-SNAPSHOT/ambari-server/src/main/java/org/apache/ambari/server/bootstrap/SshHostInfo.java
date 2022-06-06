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

package org.apache.ambari.server.bootstrap;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Information that the API needs to provide to run bootstrap on hosts.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class SshHostInfo {

  @XmlElement
  private String sshKey;

  @XmlElement
  private List<String>  hosts = new ArrayList<>();

  @XmlElement
  private boolean verbose = false;

  @XmlElement
  private String user;

  @XmlElement
  private String sshPort;

  @XmlElement
  private String password;
  
  @XmlElement
  private String userRunAs;

  public String getSshKey() {
    return sshKey;
  }

  public void setSshKey(String sshKey) {
    this.sshKey = sshKey;
  }

  public void setHosts(List<String> hosts) {
    this.hosts = hosts;
  }

  public List<String> getHosts() {
    return this.hosts;
  }
  
  public boolean isVerbose() {
    return verbose;
  }
  
  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getSshPort(){ return sshPort; }

  public void setSshPort(String sshPort){ this.sshPort = sshPort; }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }
  
  public String getUserRunAs() {
    return userRunAs;
  }

  public void setUserRunAs(String userRunAs) {
    this.userRunAs = userRunAs;
  }

  public String hostListAsString() {
    StringBuilder ret = new StringBuilder();
    if (this.hosts == null) {
      return "";
    }
    for (String host : this.hosts) {
      ret.append(host).append(":");
    }
    return ret.toString();
  }
}
