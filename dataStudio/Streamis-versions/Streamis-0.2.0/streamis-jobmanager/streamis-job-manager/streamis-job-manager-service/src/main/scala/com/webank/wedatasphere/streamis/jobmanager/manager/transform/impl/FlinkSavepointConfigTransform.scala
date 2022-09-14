package com.webank.wedatasphere.streamis.jobmanager.manager.transform.impl
import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.LaunchJob

import java.util
import scala.collection.JavaConverters._

/**
 * Flink savepoint config
 */
class FlinkSavepointConfigTransform extends FlinkConfigTransform {


  /**
   * Config group name
   *
   * @return
   */
  override protected def configGroup(): String = JobConfKeyConstants.GROUP_PRODUCE.getValue

  override protected def transform(valueSet: util.Map[String, Any], job: LaunchJob): LaunchJob = {
    transformConfig(valueSet.asScala.filter(_._1.startsWith(JobConfKeyConstants.SAVEPOINT.getValue))
      .map{
        case (key, value) =>
          (FlinkConfigTransform.FLINK_CONFIG_PREFIX + key.replace(JobConfKeyConstants.SAVEPOINT.getValue, "execution.savepoint."), value)
      }.asJava, job)
  }
}
