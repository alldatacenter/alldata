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

/**
 * The {@link ExecuteHostType} enum is used to represent where an
 * {@link ExecuteTask} will run.
 */
@XmlEnum
public enum ExecuteHostType {
  /**
   * The term "master" can mean something unique to each service. In terms of
   * where to run, this means calculate which is the "master" component and run
   * it on that.
   */
  @XmlEnumValue("master")
  MASTER,

  /**
   * Run on a single host that satifies the condition of the {@link ExecuteTask}
   * .
   */
  @XmlEnumValue("any")
  ANY,

  /**
   * Run on a single host that is picked by alphabetically sorting all hosts that satisfy the condition of the {@link ExecuteTask}
   * .
   */
  @XmlEnumValue("first")
  FIRST,

  /**
   * Run on all of the hosts.
   */
  @XmlEnumValue("all")
  ALL;
}
