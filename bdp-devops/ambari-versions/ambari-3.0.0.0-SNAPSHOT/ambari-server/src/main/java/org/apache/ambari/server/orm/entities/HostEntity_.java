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

package org.apache.ambari.server.orm.entities;

import javax.persistence.metamodel.SingularAttribute;

/**
 * This class exists so that JPQL can use static singular attributes that are
 * strongly typed as opposed to Java reflection like HostEntity.get("fieldname")
 */
@javax.persistence.metamodel.StaticMetamodel(HostEntity.class)
public class HostEntity_ {
  public static volatile SingularAttribute<HostEntity, Long> hostId;
  public static volatile SingularAttribute<HostEntity, String> hostName;
  public static volatile SingularAttribute<HostEntity, String> ipv4;
  public static volatile SingularAttribute<HostEntity, String> ipv6;
  public static volatile SingularAttribute<HostEntity, String> publicHostName;
  public static volatile SingularAttribute<HostEntity, Long> totalMem;
  public static volatile SingularAttribute<HostEntity, Integer> cpuCount;
  public static volatile SingularAttribute<HostEntity, Integer> phCpuCount;
  public static volatile SingularAttribute<HostEntity, String> cpuInfo;
  public static volatile SingularAttribute<HostEntity, String> osArch;
  public static volatile SingularAttribute<HostEntity, String> osInfo;
  public static volatile SingularAttribute<HostEntity, String> discoveryStatus;
  public static volatile SingularAttribute<HostEntity, Long> lastRegistrationTime;
  public static volatile SingularAttribute<HostEntity, String> rackInfo;
  public static volatile SingularAttribute<HostEntity, String> hostAttributes;
}
