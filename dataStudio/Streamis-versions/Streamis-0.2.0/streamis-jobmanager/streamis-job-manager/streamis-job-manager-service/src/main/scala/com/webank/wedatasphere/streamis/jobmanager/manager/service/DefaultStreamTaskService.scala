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

package com.webank.wedatasphere.streamis.jobmanager.manager.service

import com.webank.wedatasphere.streamis.jobmanager.launcher.conf.JobConfKeyConstants
import com.webank.wedatasphere.streamis.jobmanager.launcher.dao.StreamJobConfMapper
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.manager.JobLaunchManager
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.state.JobState
import com.webank.wedatasphere.streamis.jobmanager.launcher.job.{JobInfo, LaunchJob}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.entity.LogRequestPayload
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.state.{Checkpoint, Savepoint}
import com.webank.wedatasphere.streamis.jobmanager.launcher.linkis.job.{FlinkJobClient, FlinkJobInfo}
import com.webank.wedatasphere.streamis.jobmanager.manager.SpringContextHolder
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf
import com.webank.wedatasphere.streamis.jobmanager.manager.conf.JobConf.FLINK_JOB_STATUS_FAILED
import com.webank.wedatasphere.streamis.jobmanager.manager.dao.{StreamJobMapper, StreamTaskMapper}
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.StreamTask
import com.webank.wedatasphere.streamis.jobmanager.manager.entity.vo.{ExecResultVo, JobProgressVo, JobStatusVo, PauseResultVo, ScheduleResultVo, StreamTaskListVo}
import com.webank.wedatasphere.streamis.jobmanager.manager.exception.{JobErrorException, JobExecuteErrorException, JobFetchErrorException, JobPauseErrorException, JobTaskErrorException}
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.FutureScheduler
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.events.AbstractStreamisSchedulerEvent.StreamisEventInfo
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.events.{AbstractStreamisSchedulerEvent, StreamisPhaseInSchedulerEvent}
import com.webank.wedatasphere.streamis.jobmanager.manager.scheduler.events.StreamisPhaseInSchedulerEvent.ScheduleCommand
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.exception.TransformFailedErrorException
import com.webank.wedatasphere.streamis.jobmanager.manager.transform.{StreamisTransformJobBuilder, Transform}
import com.webank.wedatasphere.streamis.jobmanager.manager.util.DateUtils
import com.webank.wedatasphere.streamis.jobmanager.manager.utils.StreamTaskUtils
import org.apache.commons.lang.StringUtils
import org.apache.linkis.common.utils.{Logging, Utils}
import org.apache.linkis.httpclient.dws.DWSHttpClient
import org.apache.linkis.scheduler.queue
import org.apache.linkis.scheduler.queue.{Job, SchedulerEvent}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

import java.util
import java.util.{Calendar, Date, function}
import java.util.concurrent.Future
import javax.annotation.Resource
import scala.collection.JavaConverters._


@Service
class DefaultStreamTaskService extends StreamTaskService with Logging{

  @Autowired private var streamTaskMapper:StreamTaskMapper=_
  @Autowired private var streamJobMapper:StreamJobMapper=_
  @Autowired private var streamisTransformJobBuilders: Array[StreamisTransformJobBuilder] = _

  @Resource
  private var jobLaunchManager: JobLaunchManager[_ <: JobInfo] = _

  @Resource
  private var streamJobConfMapper: StreamJobConfMapper = _
  /**
   * Scheduler
   */
  @Resource
  private var scheduler: FutureScheduler = _


  /**
   * Sync to execute job(task)
   * 1) create a new task
   * 2) launch the new task
   *
   * @param jobId    job id
   * @param taskId   task id
   * @param execUser user name
   * @param restore  restore from job state
   */
  override def execute(jobId: Long, taskId: Long, execUser: String, restore: Boolean): Unit = {
    val result: Future[String] = asyncExecute(jobId, taskId, execUser, restore)
    val errorMessage = result.get()
    if (StringUtils.isNotBlank(errorMessage)){
      throw new JobExecuteErrorException(-1, s"Fail to execute StreamJob(Task), message output: $errorMessage");
    }
  }

  override def execute(jobId: Long, taskId: Long, execUser: String): Unit = {
    val actualJobId = if(jobId <= 0) getTaskInfo(taskId)._1 else jobId
    val restore = this.streamJobConfMapper.getRawConfValue(actualJobId, JobConfKeyConstants.START_AUTO_RESTORE_SWITCH.getValue) match {
      case "ON" => true
      case _ =>  false
    }
    execute(actualJobId, 0, execUser, restore)
  }

  override def asyncExecute(jobId: Long, taskId: Long, execUser: String, restore: Boolean): Future[String] = {
    execute(jobId, taskId, execUser, restore, new function.Function[SchedulerEvent, String] {
      override def apply(event: SchedulerEvent): String = {
        event match {
          case job: Job =>
            job.getJobInfo.getOutput
          case _ => null
        }
      }
    })._2
  }

  override def asyncExecute(jobId: Long, taskId: Long, execUser: String): Future[String] = {
    val actualJobId = if(jobId <= 0) getTaskInfo(taskId)._1 else jobId
    val restore = this.streamJobConfMapper.getRawConfValue(actualJobId, JobConfKeyConstants.START_AUTO_RESTORE_SWITCH.getValue) match {
      case "ON" => true
      case _ =>  false
    }
    asyncExecute(actualJobId, 0, execUser, restore)
  }

  override def bulkExecute(jobIds: util.List[Long], taskIds: util.List[Long], execUser: String): util.List[ExecResultVo] = {
    bulkExecute(jobIds, taskIds, execUser, (jobId, taskId) => {
      val actualJobId = if(jobId <= 0) getTaskInfo(taskId)._1 else jobId
      this.streamJobConfMapper.getRawConfValue(actualJobId, JobConfKeyConstants.START_AUTO_RESTORE_SWITCH.getValue) match {
        case "ON" => true
        case _ =>  false
      }
    })
  }
  /**
   * Bulk executing
   *
   * @param jobIds   jobIds
   * @param taskIds  taskIds
   * @param execUser execUser
   * @param restore  restore from job state
   */
  override def bulkExecute(jobIds: util.List[Long], taskIds: util.List[Long], execUser: String, restore: Boolean): util.List[ExecResultVo] = {
    bulkExecute(jobIds, taskIds, execUser, (_, _) => restore)
  }

  def bulkExecute(jobIds: util.List[Long], taskIds: util.List[Long], execUser: String, isRestore: (Long, Long) => Boolean): util.List[ExecResultVo] = {
    val result: util.List[ExecResultVo] = new util.ArrayList[ExecResultVo]()
    val counter = (jobIds.size(), taskIds.size())
    val iterateNum: Int = math.max(counter._1, counter._2)
    for (i <- 0 until iterateNum){
      val jobId = if (i < counter._1) jobIds.get(i) else 0L
      val taskId = if (i < counter._2) taskIds.get(i) else 0L
      val event = execute(jobId, taskId, execUser, isRestore(jobId, taskId),
        new function.Function[SchedulerEvent, String]{
          override def apply(event: SchedulerEvent): String = {
            event match {
              case job: Job =>
                job.getJobInfo.getOutput
              case _ => null
            }
          }
        })._1
      // Convert scheduler event to execution result
      val resultVo: ExecResultVo = new ExecResultVo(jobId, taskId)
      event match {
        case job: queue.Job =>
          queueJobInfoIntoResult(job.getJobInfo, resultVo)
      }
      result.add(resultVo)
    }
    result
  }
  def execute[T](jobId: Long, taskId: Long, execUser: String, restore: Boolean, returnMapping: function.Function[SchedulerEvent, T]): (SchedulerEvent, Future[T]) = {
    val self = SpringContextHolder.getBean(classOf[StreamTaskService])
    var finalJobId = jobId
    val event = new StreamisPhaseInSchedulerEvent(if (jobId > 0) "executeJob-" + jobId else "executeTask-" + taskId, new ScheduleCommand {

      override def onPrepare(context: StreamisPhaseInSchedulerEvent.StateContext, scheduleJob: queue.JobInfo): Unit =  {
        if (finalJobId <= 0 ){
          finalJobId = getTaskInfo(taskId)._1
        }
        // Assign the status STARTING default
        val streamTask = self.createTask(finalJobId, JobConf.FLINK_JOB_STATUS_STARTING.getValue, execUser)
        context.addVar("newTaskId", streamTask.getId);
      }

      override def schedule(context: StreamisPhaseInSchedulerEvent.StateContext, jobInfo: queue.JobInfo): util.Map[String, AnyRef] = {
         val newTaskId = context.getVar("newTaskId")
         if (null != newTaskId){
           var jobState: JobState = null
           // Means to fetch the job state from task to restore
           if (restore){
             val restoreTaskId = taskId
              // TODO fetch the job stage strategy
              jobState = if (restoreTaskId <= 0){
//                val earlierTasks = streamTaskMapper.getEarlierByJobId(finalJobId, 2)
//                if (earlierTasks.isEmpty){
//                  throw new JobExecuteErrorException(-1, "Cannot find the candidate task to search state")
//                } else if (earlierTasks.size() < 2){
//                  warn("First time to launch the StreamJob, ignore to restore JobState")
//                  null
//                } else {
//                  getStateInfo(earlierTasks.get(1))
//                }
                  getStateInfo(streamTaskMapper.getLatestLaunchedById(jobId))
              } else getStateInfo(restoreTaskId)
           }
           // Launch entrance
           launch(newTaskId.asInstanceOf[Long], execUser, jobState)
         } else {
           // TODO cannot find the new task id
         }
         null
      }

      override def onErrorHandle(context: StreamisPhaseInSchedulerEvent.StateContext, scheduleJob: queue.JobInfo, t: Throwable): Unit = {
        // Change the task status
        val newTaskId = context.getVar("newTaskId")
        if (null != newTaskId) {
            info(s"Error to launch StreamTask [$newTaskId], now try to persist the status and message output", t)
            val finalTask = new StreamTask()
            finalTask.setId(newTaskId.asInstanceOf[Long])
            finalTask.setStatus(JobConf.FLINK_JOB_STATUS_FAILED.getValue)
            // Output message equals error message, you can use t.getMessage()
            finalTask.setErrDesc(scheduleJob.getOutput)
            if (streamTaskMapper.updateTaskInStatus(finalTask, JobConf.FLINK_JOB_STATUS_STARTING.getValue) > 0) {
              info(s"Transient the StreamTask [$newTaskId]'status from STARTING to FAILED and flush the output message.")
            }
        }
      }
    })
    (event, scheduler.submit(event, returnMapping))
  }


  /**
   * Sync to pause job(task)
   *
   * @param jobId    job id
   * @param taskId   task id
   * @param operator user name
   */
  override def pause(jobId: Long, taskId: Long, operator: String, snapshot: Boolean): PauseResultVo = {
    val result: Future[PauseResultVo] = asyncPause(jobId, taskId, operator, snapshot)
    val pauseResult = result.get()
    if (StringUtils.isNotBlank(pauseResult.getMessage)){
      throw new JobExecuteErrorException(-1, s"Fail to pause StreamJob(Task), message output: ${pauseResult.getMessage}");
    }
    pauseResult
  }


  override def asyncPause(jobId: Long, taskId: Long, operator: String, snapshot: Boolean): Future[PauseResultVo] = {
    pause(jobId, taskId, operator, snapshot, new function.Function[SchedulerEvent, PauseResultVo] {
      override def apply(event: SchedulerEvent): PauseResultVo = {
        val resultVo: PauseResultVo = new PauseResultVo(jobId, taskId)
        event match {
          case job: queue.Job =>
            val jobInfo = job.getJobInfo
            queueJobInfoIntoResult(jobInfo, resultVo)
            jobInfo match {
              case eventInfo: StreamisEventInfo =>
                resultVo.setSnapshotPath(String.valueOf(eventInfo
                  .getResultSet.asScala.getOrElse("snapshotPath", "")))
            }
          case _ =>
        }
        resultVo
      }
    })._2
  }

  /**
   * Bulk pausing
   *
   * @param jobIds   jobIds
   * @param taskIds  taskIds
   * @param operator operator
   * @param snapshot snapshot
   * @return
   */
  override def bulkPause(jobIds: util.List[Long], taskIds: util.List[Long], operator: String, snapshot: Boolean): util.List[PauseResultVo] = {
    val result: util.List[Future[PauseResultVo]] = new util.ArrayList[Future[PauseResultVo]]()
    val counter = (jobIds.size(), taskIds.size())
    val iterateNum: Int = math.max(counter._1, counter._2)
    for (i <- 0 until iterateNum) {
      val jobId = if (i < counter._1) jobIds.get(i) else 0L
      val taskId = if (i < counter._2) taskIds.get(i) else 0L
      result.add(asyncPause(jobId, taskId, operator, snapshot))
    }
    result.asScala.map(_.get()).asJava
  }

  def pause[T](jobId: Long, taskId: Long, operator: String, snapshot: Boolean, returnMapping: function.Function[SchedulerEvent, T]): (SchedulerEvent, Future[T]) = {
    val self = SpringContextHolder.getBean(classOf[StreamTaskService])
    var finalJobId = jobId
    val event = new StreamisPhaseInSchedulerEvent(if (jobId > 0) "pauseJob-" + jobId else "pauseTask-" + taskId, new ScheduleCommand {

      override def onPrepare(context: StreamisPhaseInSchedulerEvent.StateContext, scheduleJob: queue.JobInfo): Unit = {
          if (finalJobId < 0){
            finalJobId = getTaskInfo(taskId)._1
          }
          // Assign the status STOPPING default
          val pauseTaskId = self.transitionTaskStatus(jobId, taskId, JobConf.FLINK_JOB_STATUS_STOPPING.getValue)
          if (pauseTaskId > 0) context.addVar("pauseTaskId", pauseTaskId)
      }

      override def onErrorHandle(context: StreamisPhaseInSchedulerEvent.StateContext, scheduleJob: queue.JobInfo, t: Throwable): Unit = {
        val pauseTaskId = context.getVar("pauseTaskId")
        if (null != pauseTaskId) {
          info(s"Error to pause StreamTask [$pauseTaskId], now try to restore the status", t)
          val finalTask = new StreamTask()
          finalTask.setId(pauseTaskId.asInstanceOf[Long])
          finalTask.setStatus(JobConf.FLINK_JOB_STATUS_RUNNING.getValue)
          // Not need to store the output message
          if (streamTaskMapper.updateTaskInStatus(finalTask, JobConf.FLINK_JOB_STATUS_STOPPING.getValue) > 0) {
            info(s"Restore the StreamTask [$pauseTaskId]'status from STOPPING return to RUNNING.")
          }
        }
      }

      override def schedule(context: StreamisPhaseInSchedulerEvent.StateContext, jobInfo: queue.JobInfo): util.Map[String, AnyRef] = {
          val pauseTaskId = context.getVar("pauseTaskId")
          val resultSet = new util.HashMap[String, AnyRef]()
          if (null != pauseTaskId){
            val streamTask = streamTaskMapper.getTaskById(pauseTaskId.asInstanceOf[Long])
            if (null == streamTask){
              throw new JobPauseErrorException(-1, s"Not found the StreamTask [$pauseTaskId] to pause, please examined the system runtime status!")
            }
            if (StringUtils.isBlank(streamTask.getLinkisJobId)){
              throw new JobPauseErrorException(-1, s"Unable to pause the StreamTask [$pauseTaskId}], the linkis job id is null")
            }
            val streamJob = streamJobMapper.getJobById(finalJobId)
            info(s"Try to stop StreamJob [${streamJob.getName} with task(taskId: ${streamTask.getId}, linkisJobId: ${streamTask.getLinkisJobId}).")
            val jobClient = jobLaunchManager.connect(streamTask.getLinkisJobId, streamTask.getLinkisJobInfo)
            val jobStateInfo = Utils.tryCatch(jobClient.stop(snapshot)){
              case e: Exception =>
                val pauseError =  new JobPauseErrorException(-1, s"Fail to stop the StreamJob [${streamJob.getName}] " +
                  s"with task(taskId: ${streamTask.getId}, linkisJobId: ${streamTask.getLinkisJobId}), reason: ${e.getMessage}.")
                pauseError.initCause(e)
                throw pauseError
              case pauseE: JobPauseErrorException =>
                throw pauseE
            }
            Option(jobStateInfo).foreach(stateInfo => resultSet.put("snapshotPath", stateInfo.getLocation))
            streamTask.setLastUpdateTime(Calendar.getInstance.getTime)
            streamTask.setStatus(JobConf.FLINK_JOB_STATUS_STOPPED.getValue)
            streamTaskMapper.updateTask(streamTask)
          }
          resultSet
      }
    })
    (event, this.scheduler.submit(event, returnMapping))
  }

  /**
   * Query execute history(查询运行历史)
   * @param jobId
   * @param version
   * @return
   */
  def queryHistory(jobId: Long, version: String): util.List[StreamTaskListVo] ={
    if(StringUtils.isEmpty(version)) throw new JobFetchErrorException(30355, "version cannot be empty.")
    val job = streamJobMapper.getJobById(jobId)
    if(job == null) throw new JobFetchErrorException(30355, s"Unknown job $jobId.")
    val jobVersion = streamJobMapper.getJobVersionById(jobId, version)
    if(jobVersion == null) return new util.ArrayList[StreamTaskListVo]
    val tasks = streamTaskMapper.getByJobVersionId(jobVersion.getId, version)
    if(tasks == null || tasks.isEmpty) return new util.ArrayList[StreamTaskListVo]
    val list = new util.ArrayList[StreamTaskListVo]
    tasks.asScala.foreach{ f =>
      val svo = new StreamTaskListVo()
      svo.setTaskId(f.getId)
      svo.setStatus(JobConf.getStatusString(f.getStatus))
      svo.setCreator(f.getSubmitUser)
      svo.setVersion(version)
      svo.setJobName(job.getName)
      svo.setStartTime(DateUtils.formatDate(f.getStartTime))
      svo.setEndTime(DateUtils.formatDate(f.getLastUpdateTime))
      svo.setJobVersionId(f.getJobVersionId)
      //获取最新版本的代码信息
      svo.setVersionContent(jobVersion.getJobContent)
      svo.setRunTime(DateUtils.intervals(f.getStartTime, f.getLastUpdateTime))
      svo.setStopCause(sub(f.getErrDesc))
      list.add(svo)
    }
    list
  }

  def getRealtimeLog(jobId: Long, taskId: Long, operator: String, requestPayload: LogRequestPayload): util.Map[String, Any] = {
    val returnMap = new util.HashMap[String, Any]
    returnMap.put("logPath", "undefined")
    returnMap.put("logs", util.Arrays.asList("No log content is available. Perhaps the task has not been scheduled"))
    returnMap.put("endLine", 1);
    val streamTask = if(taskId > 0) streamTaskMapper.getTaskById(taskId)
      else streamTaskMapper.getLatestByJobId(jobId)
    if (null != streamTask && StringUtils.isNotBlank(streamTask.getLinkisJobId)) {
      Utils.tryCatch {
        val jobClient = jobLaunchManager.connect(streamTask.getLinkisJobId, streamTask.getLinkisJobInfo)
        jobClient match {
          case client: FlinkJobClient =>
            val logIterator = client.fetchLogs(requestPayload)
            returnMap.put("logPath", logIterator.getLogPath)
            returnMap.put("logs", logIterator.getLogs)
            returnMap.put("endLine", logIterator.getEndLine)
            logIterator.close()
        }
      }{ case e: Exception =>
        // Just warn the exception
        warn(s"Unable to fetch runtime log for StreamTask " +
          s"[id: ${streamTask.getId}, jobId: ${streamTask.getJobId}, linkis_id: ${streamTask.getLinkisJobId}]", e)
      }
    }
    returnMap
  }

  /**
   * Do snapshot
   *
   * @param jobId    job id
   * @param taskId   task id
   * @param operator operator
   */
  override def snapshot(jobId: Long, taskId: Long, operator: String): String = {
    val streamTask = if (taskId > 0) streamTaskMapper.getTaskById(taskId)
      else streamTaskMapper.getLatestByJobId(jobId)
    if (null != streamTask && StringUtils.isNotBlank(streamTask.getLinkisJobId)){
      val jobClient = this.jobLaunchManager.connect(streamTask.getLinkisJobId, streamTask.getLinkisJobInfo)
      return jobClient match {
        case flinkJobClient: FlinkJobClient =>
          Option(flinkJobClient.triggerSavepoint()) match {
            case Some(savepoint) =>
              savepoint.getLocation.toString
          }
      }
    }
    null
  }
  /**
   * @param jobId
   * @return
   */
  def getProgress(jobId:Long, version: String): JobProgressVo ={
    val jobVersion = streamJobMapper.getJobVersionById(jobId, version)
    if(jobVersion == null) return new JobProgressVo
    val tasks = streamTaskMapper.getTasksByJobIdAndJobVersionId(jobVersion.getJobId, jobVersion.getId)
    if(tasks == null || tasks.isEmpty) return new JobProgressVo
    val task = tasks.get(0)
    val jobProgressVO = new JobProgressVo()
    jobProgressVO.setTaskId(task.getId)
    jobProgressVO.setProgress(task.getStatus)
    jobProgressVO
  }

  /**
   * Fetch the status list by job id list
   *
   * @param jobIds job ids
   */
  override def getStatusList(jobIds: util.List[Long]): util.List[JobStatusVo] = {
      val streamTask: util.List[StreamTask] = this.streamTaskMapper.getStatusInfoByJobIds(jobIds.asScala.map(id => {
        id.asInstanceOf[java.lang.Long]
      }).asJava)
      streamTask.asScala.map(task => {
        val statusVo = new JobStatusVo()
        statusVo.setStatusCode(task.getStatus)
        statusVo.setStatus(JobConf.getStatusString(task.getStatus))
        statusVo.setJobId(task.getJobId)
        statusVo.setMessage(task.getErrDesc)
        statusVo
      }).asJava
  }

  def getTask(jobId:Long, version: String): FlinkJobInfo ={
    val str = streamTaskMapper.getTask(jobId, version)
    if (StringUtils.isBlank(str)) {
      return new FlinkJobInfo
    }
    DWSHttpClient.jacksonJson.readValue(str,classOf[FlinkJobInfo])
  }


  /**
   * Update the task status
   *
   * @param jobId  job id
   * @param status status code
   * @return task id of latest task
   */
  @Transactional(rollbackFor = Array(classOf[Exception]))
  override def transitionTaskStatus(jobId: Long, taskId: Long, status: Int): Long = {
    trace(s"Query and lock the StreamJob in [$jobId] before updating status of StreamTask")
    Option(streamJobMapper.queryAndLockJobById(jobId)) match {
      case None => throw new JobTaskErrorException(-1, s"Unable to update status of StreamTask, the StreamJob [$jobId] is not exists.")
      case Some(job) =>
        val streamTask = if(taskId > 0) streamTaskMapper.getTaskById(taskId)
            else streamTaskMapper.getLatestByJobId(jobId)
        if (null == streamTask){
          throw new JobTaskErrorException(-1, s"Unable to find any StreamTask for job [id: ${job.getId}, name: ${job.getName}]")
        }
        if (JobConf.isCompleted(streamTask.getStatus)){
          warn(s"StreamTask [${streamTask.getId}] has been completed for for " +
            s"job [id: ${job.getId}, name: ${job.getName}]")
          // Just return 0
          0
        }else {
          streamTask.setStatus(status)
          streamTask.setLastUpdateTime(Calendar.getInstance.getTime)
          streamTaskMapper.updateTask(streamTask)
          streamTask.getId
        }
    }
  }

  /**
   * Create new task use the latest job version
   *
   * @param jobId   job id
   * @param status  init status
   * @param creator creator
   */
  @Transactional(rollbackFor = Array(classOf[Exception]))
  override def createTask(jobId: Long, status: Int, creator: String): StreamTask = {
     trace(s"Query and lock the StreamJob in [$jobId] before creating StreamTask")
     Option(streamJobMapper.queryAndLockJobById(jobId)) match {
       case None => throw new JobTaskErrorException(-1, s"Unable to create StreamTask, the StreamJob [$jobId] is not exists.")
       case Some(job) =>
          // Then to fetch latest job version
          Option(streamJobMapper.getLatestJobVersion(jobId)) match {
            case None => throw new JobTaskErrorException(-1, s"No versions can be found for job [id: ${job.getId}, name: ${job.getName}]")
            case Some(jobVersion) =>
              info(s"Fetch the latest version: ${jobVersion.getVersion} for job [id: ${job.getId}, name: ${job.getName}]")
              // Get the latest task by job version id
              val latestTask = streamTaskMapper.getLatestByJobVersionId(jobVersion.getId, jobVersion.getVersion)
              if (null == latestTask || JobConf.isCompleted(latestTask.getStatus)){
                 val streamTask = new StreamTask(jobId, jobVersion.getId, jobVersion.getVersion, creator)
                 streamTask.setStatus(status)
                 info(s"Produce a new StreamTask [jobId: $jobId, version: ${jobVersion.getVersion}, creator: $creator, status: ${streamTask.getStatus}]")
                 streamTaskMapper.insertTask(streamTask)
                 streamTask
              } else {
                  throw new JobTaskErrorException(-1, s"Unable to create new task, StreamTask [${latestTask.getId}] is still " +
                    s"not completed for job [id: ${job.getId}, name: ${job.getName}]")
              }
          }
     }
  }

  /**
   * Just launch task by task id
   *
   * @param taskId task id
   */
  override def launch(taskId: Long, execUser: String): Unit = {
      launch(taskId, execUser, null)
  }

  /**
   * Launch with job state
   * @param taskId task id
   * @param execUser executor
   * @param state state
   */
  def launch(taskId: Long, execUser: String, state: JobState):Long = {
    // First to query the task information
    val streamTask = this.streamTaskMapper.getTaskById(taskId)
    if (null == streamTask){
      throw new JobExecuteErrorException(-1, s"Not found the StreamTask [$taskId] to execute, please examined the system runtime status!")
    }
    // Second to query the related job information
    val streamJob = streamJobMapper.getJobById(streamTask.getJobId)
    if (null == streamJob){
      throw new JobExecuteErrorException(-1, s"Not found the related job info in [${streamTask.getJobId}], has been dropped it ?")
    }
    info(s"Start to find the transform builder to process the StreamJob [${streamJob.getName}]")
    val transformJob = streamisTransformJobBuilders.find(_.canBuild(streamJob)).map(_.build(streamJob))
      .getOrElse(throw new TransformFailedErrorException(30408, s"Cannot find a TransformJobBuilder to build StreamJob ${streamJob.getName}."))
    // To avoid the permission problem, use the creator to submit job
    // Use {projectName}.{jobName} as the launch job name
    var launchJob = LaunchJob.builder().setJobName(s"${streamJob.getProjectName}.${streamJob.getName}.${taskId}").setSubmitUser(streamJob.getCreateBy).build()
    launchJob = Transform.getTransforms.foldLeft(launchJob)((job, transform) => transform.transform(transformJob, job))
    info(s"StreamJob [${streamJob.getName}] has transformed with launchJob $launchJob, now to launch it.")
    //TODO getLinkisJobManager should use jobManagerType to instance in future, since not only `simpleFlink` mode is supported in future.
    val jobClient = jobLaunchManager.launch(launchJob, state)
    // Refresh and store the information from JobClient
    Utils.tryCatch {
      // Refresh the job info(If the job shutdown immediately)
      val jobInfo = jobClient.getJobInfo(true)
      info(s"StreamJob [${streamJob.getName}] has launched with linkis_id ${jobInfo.getId}. now to examine its status")
      streamTask.setLinkisJobId(jobInfo.getId)
      StreamTaskUtils.refreshInfo(streamTask, jobInfo)
      // First to store the launched task info
      streamTaskMapper.updateTask(streamTask)
      info(s"StreamJob [${streamJob.getName}] is ${jobInfo.getStatus} with $jobInfo.")
      if (FLINK_JOB_STATUS_FAILED.getValue == streamTask.getStatus){
         throw new JobExecuteErrorException(-1, s"(提交流式应用状态失败, 请检查日志), errorDesc: ${streamTask.getErrDesc}")
      }
      // Drop the temporary configuration
      Utils.tryQuietly(streamJobConfMapper.deleteTemporaryConfValue(streamTask.getJobId), {
        case e: Exception =>
          warn(s"Fail to delete the temporary configuration for job [${streamTask.getJobId}], task [${streamTask.getId}]", e)
      })
    }{case e: Exception =>
      val message = s"Error occurred when to refresh and store the info of StreamJob [${streamJob.getName}] with JobClient"
      warn(s"$message, stop and destroy the Client connection.")
      // Stop the JobClient directly
      Utils.tryAndWarn(jobClient.stop())
      val errExcept =  new JobExecuteErrorException(-1, s"$message, message: ${e.getMessage}")
      errExcept.initCause(e)
      throw errExcept
    }
    streamTask.getId
  }


  /**
   * @param taskId taskId
   * @return
   */
  private def getTaskInfo(taskId: Long): (Long, StreamTask) = {
      val oldStreamTask = streamTaskMapper.getTaskById(taskId)
      if (Option(oldStreamTask).isEmpty){
        throw new JobTaskErrorException(-1, s"Cannot find the StreamTask in id: $taskId")
      }
     (oldStreamTask.getJobId, oldStreamTask)
  }

  /**
   * Sub function
   * @param str str
   * @return
   */
  private def sub(str:String):String = {
    if (StringUtils.isBlank(str) || str.length <= 100){
      str
    }else {
      if (str.contains("message")){
        val subStr = str.substring(str.indexOf("message") - 1)
        if (subStr.length <= 100){
          subStr + "..."
        }else {
          subStr.substring(0,100) + "..."
        }
      }else {
        str.substring(0,100) + "..."
      }
    }
  }

  /**
   * Convert the queue job info into schedule result
   * @param jobInfo job info
   * @param scheduleResult schedule result
   */
  private def queueJobInfoIntoResult(jobInfo: queue.JobInfo, scheduleResult: ScheduleResultVo): Unit = {
    scheduleResult.setScheduleId(jobInfo.getId)
    scheduleResult.setScheduleState(jobInfo.getState)
    scheduleResult.setProgress(jobInfo.getProgress)
    // TODO Set metric info
    scheduleResult.setMessage(jobInfo.getOutput)
  }

  override def getStateInfo(taskId: Long): JobState = {
    getStateInfo(this.streamTaskMapper.getTaskById(taskId))
  }

  private def getStateInfo(streamTask: StreamTask): JobState = {
    Option(streamTask) match {
      case Some(task) =>
        if (StringUtils.isNotBlank(task.getLinkisJobId)) {
          info(s"Try to restore the JobState form taskId [${task.getId}], fetch the state information.")
          // Connect to get the JobInfo
          val jobClient = this.jobLaunchManager.connect(task.getLinkisJobId, task.getLinkisJobInfo)
          val jobInfo = jobClient.getJobInfo
          // Get the JobStateManager
          val jobStateManager = this.jobLaunchManager.getJobStateManager
          val stateList: util.List[JobState] = new util.ArrayList[JobState]()
          // First to fetch the latest Savepoint information
          Option(jobStateManager.getJobState[Savepoint](classOf[Savepoint], jobInfo)).foreach(savepoint => stateList.add(savepoint))
          // Determinate if need the checkpoint information
          this.streamJobConfMapper.getRawConfValue(task.getJobId, JobConfKeyConstants.CHECKPOINT_SWITCH.getValue) match {
            case "ON" =>
              // Then to fetch the latest Checkpoint information
              Option(jobStateManager.getJobState[Checkpoint](classOf[Checkpoint], jobInfo)).foreach(checkpoint => stateList.add(checkpoint))
            case _ =>
          }
          // Fetch the job state info in jobInfo at last
//          Option(jobInfo.getJobStates).foreach(states => states.foreach(state => {
//            val savepoint = new Savepoint(state.getLocation)
//            savepoint.setTimestamp(state.getTimestamp)
//            stateList.add(savepoint)
//          }))
          if (!stateList.isEmpty){
            // Choose the newest job state
            val finalState = stateList.asScala.maxBy(_.getTimestamp)
            info(s"Final choose the JobState: [${finalState.getLocation}] to restore the StreamJob")
            return finalState
          }
        } else {

        }
        null
      case _ => null
    }
  }

}
