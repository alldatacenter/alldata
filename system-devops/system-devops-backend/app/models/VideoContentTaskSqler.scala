package models

import auto.VideoContentTask._
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._
import java.sql.Timestamp
import controllers._
import auto.VideoContentTask.VideoContentTaskTables.{VideoContentTask, VideoContentTaskRow}
import controllers.{VideoContentTaskStatus, VideoTaskStatus}
import javax.inject.Inject

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}


class VideoContentTaskSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerVideoContentTaskSqler {

  override def addVideoContentTask(read: VideoContentTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = VideoContentTaskRow(
      0, ts, ts,
      read.aepPath,
      read.name,
      read.status,
      read.videoTime,
      read.finalTime,
      read.endingTime,
      read.preViewPath,
      read.resultPath,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.language,
      read.videoValue,
      read.tag
    )
    if (notExistVideoContentPath(read.aepPath)) {
      val action = (VideoContentTask returning VideoContentTask.map(_.id)).+=(row)
      db.run(action)
    } else {
      Future.successful(-1)
    }
  }

  def notExistVideoContentPath(fa: String): Boolean = {
    val action = VideoContentTask
      .filter(_.aepPath === fa)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }


  def updateVideoContentStatus(id: Int, read: VideoContentTaskStatus): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = VideoContentTask
      .filter(_.id === id).filterNot(_.status === VideoTaskStatus.TASK_VIDEO_STATUS_DELETE)
      .map(read => (
        read.updateTime,
        read.status
      ))
      .update(
        ts,
        read.status
      )
    action.statements.foreach(println)
    db.run(action)
  }

  def updateVideoContentFields(id: Int, read: VideoContentUpdateFields): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val resultPath = read.resultPath;
    val frameRate = read.frameRate;
    val pixelLength = read.pixelLength;
    val pixelHeight = read.pixelHeight;
    val videoTime = read.videoTime;

    // update sql
    try {
      db.run(sqlu""" update video_content_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_content_task set resultPath = $resultPath where id = $id and $resultPath != "" """)
      db.run(sqlu""" update video_content_task set frame_rate = $frameRate where id = $id and $frameRate != "" """)
      db.run(sqlu""" update video_content_task set pixel_length = $pixelLength where id = $id and $pixelLength != "" """)
      db.run(sqlu""" update video_content_task set pixel_height = $pixelHeight where id = $id and $pixelHeight != "" """)
      db.run(sqlu""" update video_content_task set videoTime = $videoTime where id = $id and $videoTime != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateVideoFinalAndEndingTime(id: Int, read: VideoFinalAndEndingTime): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val finalTime = read.finalTime;
    val endingTime = read.endingTime;

    // update sql
    try {
      db.run(sqlu""" update video_content_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_content_task set finalTime = $finalTime where id = $id and $finalTime != "" """)
      db.run(sqlu""" update video_content_task set endingTime = $endingTime where id = $id and $endingTime != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def deleteVideoContentTaskExtend(aepPath: String): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val deleteStatus: String = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    db.run(sqlu""" update video_content_task set status = $deleteStatus, update_time = $ts  where aepPath = $aepPath and status != $deleteStatus """)
  }

  override def deleteVideoContentTask(id: Int): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val deleteStatus: String = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    db.run(sqlu""" update video_content_task set status = $deleteStatus, update_time = $ts  where id = $id and status != $deleteStatus """)
  }

  override def findList(read: VideoContentTaskListRead): Seq[VideoContentTaskData] = {
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
    val querySql = s""" select * from video_content_task $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    val action = sql"#$querySql".as[VideoContentTaskRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def updateVideoContentTag(read: VideoContentTag): Future[Int] = {
    val deleteStatus = VideoTaskStatus.TASK_VIDEO_STATUS_DELETE
    val ts = new Timestamp(System.currentTimeMillis())
    val tag: String = read.tag
    val id: String = read.id
    // update sql
    try {
      db.run(sqlu""" update video_content_task set tag = $tag, update_time = $ts where id = $id and status != $deleteStatus""")
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }
}



