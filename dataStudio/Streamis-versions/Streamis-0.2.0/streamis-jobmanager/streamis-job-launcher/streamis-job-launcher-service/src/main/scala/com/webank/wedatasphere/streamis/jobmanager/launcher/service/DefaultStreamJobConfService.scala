package com.webank.wedatasphere.streamis.jobmanager.launcher.service
import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants
import com.webank.wedatasphere.streamis.jobmanager.launcher.dao.StreamJobConfMapper
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo.JobConfValueVo.ValueList
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.vo.{JobConfValueSet, JobConfValueVo}
import com.webank.wedatasphere.streamis.jobmanager.launcher.entity.{JobConfDefinition, JobConfValue}
import com.webank.wedatasphere.streamis.jobmanager.launcher.exception.ConfigurationException
import com.webank.wedatasphere.streamis.jobmanager.launcher.service.tools.JobConfValueUtils
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.StreamJobMapper
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamJob
import org.apache.commons.lang3.StringUtils
import org.apache.linkis.common.utils.Logging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.util
import javax.annotation.Resource
import scala.collection.JavaConverters._

@Service
class DefaultStreamJobConfService extends StreamJobConfService with Logging{

  @Resource
  private var streamJobConfMapper: StreamJobConfMapper = _

  @Resource
  private var streamJobMapper: StreamJobMapper = _
  /**
   * Get all config definitions
   *
   * @return list
   */
  override def loadAllDefinitions(): util.List[JobConfDefinition] = {
    streamJobConfMapper.loadAllDefinitions()
  }

  /**
   * Save job configuration
   *
   * @param jobId    job id
   * @param valueMap value map
   */
  @Transactional(rollbackFor = Array(classOf[Exception]))
  override def saveJobConfig(jobId: Long, valueMap: util.Map[String, Any]): Unit = {
    val definitions = Option(this.streamJobConfMapper.loadAllDefinitions())
      .getOrElse(new util.ArrayList[JobConfDefinition]())
    // Can deserialize the value map at first
    val configValues = JobConfValueUtils.deserialize(valueMap, definitions)
    suppleDefaultConfValue(configValues, definitions)
    saveJobConfig(jobId, configValues)
  }

  /**
   * Query the job configuration
   *
   * @param jobId job id
   * @return
   */
  override def getJobConfig(jobId: Long): util.Map[String, Any] = {
    getJobConfig(jobId, this.streamJobConfMapper.loadAllDefinitions())
  }

  /**
   * Query the job value
   *
   * @param jobId     job id
   * @param configKey config key
   * @return
   */
  override def getJobConfValue(jobId: Long, configKey: String): String = {
    this.streamJobConfMapper.getRawConfValue(jobId, configKey)
  }

  /**
   * Get job configuration value set
   *
   * @param jobId job id
   * @return
   */
  override def getJobConfValueSet(jobId: Long): JobConfValueSet = {
    val valueSet = new JobConfValueSet
    val definitions: util.List[JobConfDefinition] = this.streamJobConfMapper.loadAllDefinitions()
    val jobConfig: util.Map[String, Any] = getJobConfig(jobId, definitions)
    val definitionMap: util.Map[String, JobConfDefinition] = definitions.asScala.map(definition => (definition.getKey, definition)).toMap.asJava
    valueSet.setResourceConfig(resolveConfigValueVo(JobConfKeyConstants.GROUP_RESOURCE.getValue, jobConfig, definitionMap))
    valueSet.setParameterConfig(resolveConfigValueVo(JobConfKeyConstants.GROUP_FLINK_EXTRA.getValue, jobConfig, definitionMap))
    valueSet.setProduceConfig(resolveConfigValueVo(JobConfKeyConstants.GROUP_PRODUCE.getValue, jobConfig, definitionMap))
    valueSet.setPermissionConfig(resolveConfigValueVo(JobConfKeyConstants.GROUP_PERMISSION.getValue, jobConfig, definitionMap))
    valueSet.setAlarmConfig(resolveConfigValueVo(JobConfKeyConstants.GROUP_ALERT.getValue, jobConfig, definitionMap))
    valueSet.setJobId(jobId)
    valueSet
  }

  /**
   * Save job configuration value set
   *
   * @param valueSet value set
   */
  override def saveJobConfValueSet(valueSet: JobConfValueSet): Unit = {
     val configValues: util.List[JobConfValue] = new util.ArrayList[JobConfValue]()
     val definitions = this.streamJobConfMapper.loadAllDefinitions()
     val definitionMap: util.Map[String, JobConfDefinition] = definitions
        .asScala.map(definition => (definition.getKey, definition)).toMap.asJava
     configValues.addAll(convertToConfigValue(
       valueSet.getResourceConfig, definitionMap, Option(definitionMap.get(JobConfKeyConstants.GROUP_RESOURCE.getValue)) match {
         case Some(definition) => definition.getId
         case _ => 0
       }))
     configValues.addAll(convertToConfigValue(
       valueSet.getParameterConfig, definitionMap, Option(definitionMap.get(JobConfKeyConstants.GROUP_FLINK_EXTRA.getValue)) match {
         case Some(definition) => definition.getId
         case _ => 0
       }))
    configValues.addAll(convertToConfigValue(
      valueSet.getProduceConfig, definitionMap, Option(definitionMap.get(JobConfKeyConstants.GROUP_PRODUCE.getValue)) match {
        case Some(definition) => definition.getId
        case _ => 0
      }))
    configValues.addAll(convertToConfigValue(
      valueSet.getPermissionConfig, definitionMap, Option(definitionMap.get(JobConfKeyConstants.GROUP_PERMISSION.getValue)) match {
        case Some(definition) => definition.getId
        case _ => 0
      }))
    configValues.addAll(convertToConfigValue(
      valueSet.getAlarmConfig, definitionMap, Option(definitionMap.get(JobConfKeyConstants.GROUP_ALERT.getValue)) match {
        case Some(definition) => definition.getId
        case _ => 0
      }))
    suppleDefaultConfValue(configValues, definitions)
    saveJobConfig(valueSet.getJobId, configValues)
  }
  /**
   * Get job configuration map
   * @param jobId job id
   * @param definitions definitions
   * @return
   */
  private def getJobConfig(jobId: Long, definitions: util.List[JobConfDefinition]): util.Map[String, Any] = {
    Option(this.streamJobConfMapper.getConfValuesByJobId(jobId)) match {
      case None => new util.HashMap[String, Any]()
      case Some(list: util.List[JobConfValue]) =>
        JobConfValueUtils.serialize(list,
          Option(definitions)
            .getOrElse(new util.ArrayList[JobConfDefinition]()))
    }
  }

  private def saveJobConfig(jobId: Long, configValues: util.List[JobConfValue]): Unit = {
    trace(s"Query and lock the StreamJob in [$jobId] before saving/update configuration")
    Option(streamJobMapper.queryAndLockJobById(jobId)) match {
      case None => throw new ConfigurationException(s"Unable to saving/update configuration, the StreamJob [$jobId] is not exists.")
      case Some(job: StreamJob) =>
        // Delete all configuration
        this.streamJobConfMapper.deleteConfValuesByJobId(job.getId)
        configValues.asScala.foreach(configValue => {{
          configValue.setJobId(job.getId)
          configValue.setJobName(job.getName)
        }})
        info(s"Save the job configuration size: ${configValues.size()}, jobName: ${job.getName}")
        if (!configValues.isEmpty) {
          // Send to save the configuration new
          this.streamJobConfMapper.batchInsertValues(configValues)
        }
    }
  }

  /**
   * Supple the default value into the configuration
   * @param configValues config value list
   * @param definitions definitions
   */
  private def suppleDefaultConfValue(configValues: util.List[JobConfValue], definitions: util.List[JobConfDefinition]): Unit = {
    val configMark = configValues.asScala.filter(configValue => configValue.getReferDefId != null)
      .map(configValue => (configValue.getReferDefId, 1)).toMap
    definitions.asScala.filter(definition => definition.getLevel > 0 && StringUtils.isNotBlank(definition.getDefaultValue))
      .foreach(definition => configMark.get(definition.getId) match {
        case Some(mark) =>
        case None =>
          val configValue = new JobConfValue(definition.getKey, definition.getDefaultValue, definition.getId)
          configValues.add(configValue)
      }
    )
  }
  /**
   * Resolve to config value view object
   * @param group group
   * @param jobConfig job config
   * @param definitionMap (key => definition)
   */
  private def resolveConfigValueVo(group: String, jobConfig: util.Map[String, Any],
                                 definitionMap: util.Map[String, JobConfDefinition]): util.List[JobConfValueVo] = {
     Option(jobConfig.get(group)) match {
       case Some(configMap: util.Map[String, Any]) =>
         configMap.asScala.map{
           case (key, value) =>
             val configValue = new JobConfValueVo(key, String.valueOf(value))
             Option(definitionMap.get(key)) match {
               case Some(definition) =>
                 configValue.setConfigkeyId(definition.getId)
                 configValue.setName(definition.getName)
                 val refValues = definition.getRefValues
                 if (StringUtils.isNotBlank(refValues)){
                    val valueList = new util.ArrayList[ValueList]()
                    refValues.split(",").foreach(refValue =>{
                      valueList.add(new ValueList(refValue, refValue.equals(value)))
                    })
                   configValue.setValueLists(valueList)
                 }
               case _ =>
             }
             configValue
         }.toList.asJava
       case None => new util.ArrayList[JobConfValueVo]()
     }
  }

  /**
   * Convert to config value entities
   * @param configValueVos view object
   * @param definitionMap definition map
   * @param parentRef parent ref id
   * @return
   */
  private def convertToConfigValue(configValueVos: util.List[JobConfValueVo],
                                   definitionMap: util.Map[String, JobConfDefinition], parentRef: Long): util.List[JobConfValue] = {
    Option(configValueVos) match {
      case Some(voList) =>
        voList.asScala.map(vo => {
          val definition = definitionMap.get(vo.getKey)
          val confValue = new JobConfValue(vo.getKey, vo.getValue, if (null == definition) parentRef else definition.getId)
          confValue
        }).asJava
      case _ => new util.ArrayList[JobConfValue]()
    }
  }

}
