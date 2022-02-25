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
package org.apache.ambari.spi.upgrade;

import javax.xml.bind.annotation.XmlEnumValue;

import com.google.gson.annotations.SerializedName;

/**
 * Indicates the type of Upgrade performed.
 */
public enum UpgradeType {
  /**
   * Services are up the entire time
   */
  @XmlEnumValue("ROLLING")
  @SerializedName("rolling_upgrade")
  ROLLING,

  /**
   * All services are stopped, then started
   */
  @XmlEnumValue("NON_ROLLING")
  @SerializedName("nonrolling_upgrade")
  NON_ROLLING,

  /**
   * Host-ordered upgrade.
   */
  @XmlEnumValue("HOST_ORDERED")
  @SerializedName("host_ordered_upgrade")
  HOST_ORDERED;
}
