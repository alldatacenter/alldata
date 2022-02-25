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

import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;

import com.google.gson.annotations.Expose;

/**
 * The {@link RegenerateKeytabsTask} is used for injection Kerberos tasks into
 * an upgrade which will regenerate keytabs. The regeneration is always partial,
 * opting to only regenerate missing keytabs.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "regenerate_keytabs")
public class RegenerateKeytabsTask extends Task {

  @Expose
  @XmlTransient
  private Task.Type type = Type.REGENERATE_KEYTABS;

  /**
   * Constructor.
   *
   */
  public RegenerateKeytabsTask() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Task.Type getType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public StageWrapper.Type getStageWrapperType() {
    return StageWrapper.Type.REGENERATE_KEYTABS;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getActionVerb() {
    return "Regenerating Keytabs";
  }

  /**
   * Gets a JSON representation of this task.
   *
   * @return a JSON representation of this task.
   */
  public String toJson() {
    return GSON.toJson(this);
  }
}
