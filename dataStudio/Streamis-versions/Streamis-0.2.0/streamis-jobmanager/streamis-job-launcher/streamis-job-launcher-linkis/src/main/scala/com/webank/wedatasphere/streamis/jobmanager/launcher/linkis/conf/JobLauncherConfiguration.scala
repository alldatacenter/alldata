package com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.conf

import org.apache.linkis.common.conf.CommonVars

/**
 * Job Launcher configuration
 */
object JobLauncherConfiguration {


  val FLINK_FETCH_APPLICATION_INFO_MAX_TIMES: CommonVars[Int] = CommonVars("wds.streamis.application.info.fetch.max", 6)

  val FLINK_STATE_DEFAULT_SCHEME: CommonVars[String] = CommonVars("wds.streamis.launch.flink.state.default.scheme", "hdfs")
  /**
   * Support schema protocols to store flink job states
   */
  val FLINK_STATE_SUPPORT_SCHEMES: CommonVars[String] = CommonVars("wds.streamis.launch.flink.state.support.schemas", "hdfs,file,viewfs,s3")

  /**
   * Authority(host) value to store flink job states
   */
  val FLINK_STATE_DEFAULT_AUTHORITY: CommonVars[String] = CommonVars("wds.streamis.launch.flink.state.authority", "")
  /**
   * Savepoint mode
   */
  val FLINK_TRIGGER_SAVEPOINT_MODE: CommonVars[String] = CommonVars("wds.streamis.launch.flink.savepoint.mode", "trigger")

  /**
   * Savepoint dir
   */
  val FLINK_SAVEPOINT_PATH: CommonVars[String] = CommonVars("wds.streamis.launch.flink.savepoint.dir", "/flink/flink-savepoints")

  /**
   * Checkpoint dir
   */
  val FLINK_CHECKPOINT_PATH: CommonVars[String] = CommonVars("wds.streamis.launch.flink.checkpoint.dir", "/flink/flink-checkpoints")

  /**
   * Linkis release version
   */
  val FLINK_LINKIS_RELEASE_VERSION: CommonVars[String] = CommonVars("wds.streamis.launch.flink.linkis.release.version", "")
  /**
   * Variable: savepoint path
   */
  val VAR_FLINK_SAVEPOINT_PATH: CommonVars[String] = CommonVars("wds.streamis.launch.variable.flink.savepoint.path", "flink.app.savePointPath")

  /**
   * Variable: flink app
   */
  val VAR_FLINK_APP_NAME: CommonVars[String] = CommonVars("wds.streamis.launch.variable.flink.app.name", "flink.app.name")

}
