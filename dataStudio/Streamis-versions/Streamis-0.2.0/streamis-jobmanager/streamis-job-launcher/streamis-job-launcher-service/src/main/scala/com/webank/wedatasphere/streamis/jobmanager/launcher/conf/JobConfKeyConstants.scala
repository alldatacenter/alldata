/*
 * Copyright 2021 WeBank
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.webank.wedatasphere.streamis.jobmanager.launcher.conf

import org.apache.linkis.common.conf.CommonVars


/**
 * Config key constants
 */
object JobConfKeyConstants {

  /**
   * Group: Flink extra
   */
  val GROUP_FLINK_EXTRA: CommonVars[String] = CommonVars("wds.streamis.job.config.key.group.flink-extra", "wds.linkis.flink.custom")

  /**
   * Group: produce
   */
  val GROUP_PRODUCE: CommonVars[String] = CommonVars("wds.streamis.job.config.key.group.produce", "wds.linkis.flink.produce")

  /**
   * Group: resource
   */
  val GROUP_RESOURCE: CommonVars[String] = CommonVars("wds.streamis.job.config.key.group.resource", "wds.linkis.flink.resource")

  /**
   * Group: permission
   */
  val GROUP_PERMISSION: CommonVars[String] = CommonVars("wds.streamis.job.config.key.group.permission", "wds.linkis.flink.authority")

  /**
   * Group: alert
   */
  val GROUP_ALERT: CommonVars[String] = CommonVars("wds.streamis.job.config.key.group.alert", "wds.linkis.flink.alert")
  /**
   * Checkpoint prefix
   */
  val CHECKPOINT: CommonVars[String] = CommonVars("wds.streamis.job.config.key.checkpoint", "wds.linkis.flink.checkpoint.")

  /**
   * Checkpoint switch
   */
  val CHECKPOINT_SWITCH: CommonVars[String] = CommonVars("wds.streamis.job.config.key.checkpoint.switch", "wds.linkis.flink.checkpoint.switch")
  /**
   * Savepoint prefix
   */
  val SAVEPOINT: CommonVars[String] = CommonVars("wds.streamis.job.config.key.savepoint", "wds.linkis.flink.savepoint.")

  /**
   * Switch to restart job automatically when fail
   */
  val FAIL_RESTART_SWITCH: CommonVars[String] = CommonVars("wds.streamis.job.config.key.fail-restart.switch", "wds.linkis.flink.app.fail-restart.switch")

  /**
   * Switch to restore job automatically when starting
   */
  val START_AUTO_RESTORE_SWITCH: CommonVars[String] = CommonVars("wds.streamis.job.config.key.start-auto-restore.switch", "wds.linkis.flink.app.start-auto-restore.switch")

  /**
   * Authority author
   */
  val AUTHORITY_AUTHOR_VISIBLE: CommonVars[String] = CommonVars("wds.streamis.job.config.key.authority.visible", "wds.linkis.flink.authority.visible")

  /**
   * Alert user
   */
  val ALERT_USER: CommonVars[String] = CommonVars("wds.streamis.job.config.key.alert.user", "wds.linkis.flink.alert.failure.user")

  /**
   * Alert level
   */
  val ALERT_LEVEL: CommonVars[String] = CommonVars("wds.streamis.job.config.key.alert.level", "wds.linkis.flink.alert.level")
}
