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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * Class captures immediate response to a bootstrap api call.
 * If the api call is ok, the return should return that its ok.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {})
public class BootStrapPostStatus {
  @XmlType(name="status")
  @XmlEnum
  public enum BSPostStat {
    OK,
    ERROR
  }

  @XmlElement
  private BSPostStat postStatus;
  @XmlElement
  private String log;
  @XmlElement
  private long requestId;

  public long getRequestId() {
    return this.requestId;
  }

  public void setRequestId(long requestId) {
    this.requestId = requestId;
  }

  public BSPostStat getStatus() {
    return this.postStatus;
  }

  public void setStatus(BSPostStat status) {
    this.postStatus  = status;
  }

  public String getLog() {
    return this.log;
  }

  public void setLog(String log) {
    this.log = log;
  }
}
