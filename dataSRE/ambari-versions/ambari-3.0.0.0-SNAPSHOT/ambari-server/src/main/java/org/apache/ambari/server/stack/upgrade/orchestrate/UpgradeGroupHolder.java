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
package org.apache.ambari.server.stack.upgrade.orchestrate;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.UpgradePack;

/**
 * Short-lived objects that hold information about upgrade groups
 */
public class UpgradeGroupHolder {
  /**
   *
   */
  boolean processingGroup;

  /**
   * The name
   */
  public String name;
  /**
   * The title
   */
  public String title;


  public Class<? extends Grouping> groupClass;

  /**
   * Indicate whether retry is allowed for the stages in this group.
   */
  public boolean allowRetry = true;

  /**
   * Indicates whether the stages in this group are skippable on failure.  If a
   * stage is skippable, a failed result can be skipped without failing the entire upgrade.
   */
  public boolean skippable = false;

  /**
   * {@code true} if the upgrade group's tasks can be automatically skipped if
   * they fail. This is used in conjunction with
   * {@link UpgradePack#isComponentFailureAutoSkipped()}. If the upgrade pack
   * (or the upgrade request) does support auto skipping failures, then this
   * setting has no effect. It's used mainly as a way to ensure that some
   * groupings never have their failed tasks automatically skipped.
   */
  public boolean supportsAutoSkipOnFailure = true;

  /**
   * List of stages for the group
   */
  public List<StageWrapper> items = new ArrayList<>();

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder("UpgradeGroupHolder{");
    buffer.append("name=").append(name);
    buffer.append(", title=").append(title);
    buffer.append(", allowRetry=").append(allowRetry);
    buffer.append(", skippable=").append(skippable);
    buffer.append("}");
    return buffer.toString();
  }
}