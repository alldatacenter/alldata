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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Status of a bootstrap operation. Operation is successful or error
 * and explains all the info regarding the operation on each host.
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class BootStrapStatus {
  @XmlType(name="status")
  @XmlEnum
  public enum BSStat {
    RUNNING,
    SUCCESS,
    ERROR
  }

  @XmlElement
  private BSStat status;

  @XmlElement
  private List<BSHostStatus> hostsStatus;

  @XmlElement
  private String log;

  public synchronized void setStatus(BSStat status) {
    this.status = status;
  }

  public synchronized BSStat getStatus() {
    return this.status;
  }

  public synchronized void setHostsStatus(List<BSHostStatus> hostsStatus) {
    this.hostsStatus = hostsStatus;
  }

  public synchronized List<BSHostStatus> getHostsStatus() {
    return this.hostsStatus;
  }

  public synchronized void setLog(String log) {
    this.log = log;
  }

  public synchronized String getLog() {
    return this.log;
  }
}
