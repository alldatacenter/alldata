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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

import com.google.gson.annotations.SerializedName;

/**
 * Indicates the scope of a group or task
 */
@XmlEnum
public enum UpgradeScope {

  /**
   * Used only when completely upgrading the cluster.
   */
  @XmlEnumValue("COMPLETE")
  @SerializedName("rolling_upgrade")
  COMPLETE,

  /**
   * Used only when partially upgrading the cluster.
   */
  @XmlEnumValue("PARTIAL")
  @SerializedName("partial")
  PARTIAL,

  /**
   * Used for any scoped upgrade.
   */
  @XmlEnumValue("ANY")
  @SerializedName("any")
  ANY;
}
