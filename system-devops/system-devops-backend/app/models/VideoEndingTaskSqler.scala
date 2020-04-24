package models

import java.sql.Timestamp

import controllers.{VideoEndingTag, VideoTaskStatus, _}
import auto.VideoEndingTask._
import auto.VideoEndingTask.VideoEndingTaskTables.{VideoEndingTask, VideoEndingTaskRow}
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class VideoEndingTaskSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerVideoEndingTaskSqler {

  override def addVideoEndingTask(read: VideoEndingTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = VideoEndingTaskRow(
      0, ts, ts,
      read.aepPath,
      read.name,
      read.videoTime,
      read.videoValue,
      read.preViewPath,
      read.resultPath,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.status,
      read.language,
      read.tag
    )

    if (notExistVideoEndingPath(read.aepPath)) {
      val action = (VideoEndingTask returning VideoEndingTask.map(_.id)).+=(row)
      db.run(action)

    } else {
      Future.successful(-1)
    }
  }

  def notExistVideoEndingPath(fa: String): Boolean = {
    val action = VideoEndingTask
      .filter(_.aepPath === fa)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }

  def updateVideoEndingFields(id: Int, read: VideoEndingUpdateFields): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val frameRate = read.frameRate;
    val pixelLength = read.pixelLength;
    val pixelHeight = read.pixelHeight;
    val videoTime = read.videoTime;
    val resultPath = read.resultPath;
    // update sql
    try {
      db.run(sqlu""" update video_ending_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_ending_task set frame_rate = $frameRate where id = $id and $frameRate != "" """)
      db.run(sqlu""" update video_ending_task set pixel_length = $pixelLength where id = $id and $pixelLength != "" """)
      db.run(sqlu""" update video_ending_task set pixel_height = $pixelHeight where id = $id and $pixelHeight != "" """)
      db.run(sqlu""" update video_ending_task set videoTime = $videoTime where id = $id and $videoTime != "" """)
      db.run(sqlu""" update video_ending_task set resultPath = $resultPath where id = $id and $resultPath != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateVideoEndingStatus(id: Int, read: VideoEndingStatus): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val status = read.status;
    // update sql
    val deleteStatus = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    try {
      db.run(sqlu""" update video_ending_task set status = $status,  update_time = $ts where id = $id and status != $deleteStatus """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateVideoVideoValue(id: Int, read: VideoEndingVideoValue): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val videoValue = read.videoValue;
    // update sql
    try {
      db.run(sqlu""" update video_ending_task set videoValue = $videoValue, update_time = $ts  where id = $id and $videoValue != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateVideoEndingTag(read: VideoEndingTag): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val tag: String = read.tag
    val id: String = read.id
    val deleteStatus = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    // update sql
    try {
      db.run(sqlu""" update video_ending_task set tag = $tag, update_time = $ts where id = $id and status != $deleteStatus """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }


  def deleteVideoEndingTaskExtend(aepPath: String): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val deleteStatus: String = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    db.run(sqlu""" update video_ending_task set status = $deleteStatus, update_time = $ts  where aepPath = $aepPath and status != $deleteStatus """)
  }

  override def deleteVideoEndingTask(id: Int): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val deleteStatus: String = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    db.run(sqlu""" update video_ending_task set status = $deleteStatus, update_time = $ts  where id = $id and status != $deleteStatus""")
  }

  override def findList(read: VideoEndingTaskListRead): Seq[VideoEndingTaskData] = {
    val deleteStatus = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    var where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    if (where == "") {
      where = s"where status != '$deleteStatus'"
    } else {
      where = where + s" and status != '$deleteStatus'"
    }
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from video_ending_task $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    val action = sql"#$querySql".as[VideoEndingTaskRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


}