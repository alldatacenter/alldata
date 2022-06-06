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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

import org.apache.ambari.server.serveraction.upgrades.ManualStageAction;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;

/**
 * Identifies that an upgrade step that requires confirmation before continuing.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name="manual")
public class ManualTask extends ServerSideActionTask {

  public ManualTask() {
    implClass = ManualStageAction.class.getName();
  }

  @XmlTransient
  private Task.Type type = Task.Type.MANUAL;

  @XmlTransient
  public String structuredOut = null;

  @Override
  public Task.Type getType() {
    return type;
  }

  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.SERVER_SIDE_ACTION;
  }
}
