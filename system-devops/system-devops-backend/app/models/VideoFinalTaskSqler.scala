package models

import java.sql.Timestamp

import auto.VideoFinalTask.VideoFinalTaskTables.{VideoFinalTask, VideoFinalTaskRow}
import auto.VideoFinalTask._
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._
import controllers._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class VideoFinalTaskSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerVideoFinalTaskSqler {

  def needRenderEnding(id: Int): String = {
    val taskInfoAction = VideoFinalTask.filter(_.id === id).result
    val taskInfoList = Await.result(db.run(taskInfoAction), 5 seconds)
    var idEnding = ""
    var videoValue = ""
    var language = ""
    var urlEnding = ""
    taskInfoList.map(one => {
      idEnding = one.idEnding
      videoValue = one.videoValue
      language = one.language
    })
    val action = VideoFinalTask.filterNot(_.id === id)
      .filter(_.idEnding === idEnding)
      .filter(_.language === language)
      .filter(_.videoValue === videoValue)
      .result
    val list = Await.result(db.run(action), 5 seconds)

    if (list.isEmpty) {
      ""
    } else {
      val action = VideoFinalTask
        .filterNot(_.urlEnding === "")
        .filter(_.idEnding === idEnding)
        .filter(_.videoValue === videoValue)
        .filter(_.language === language)
        .result
      val list = Await.result(db.run(action), 5 seconds)
      if (list.isEmpty) {
        ""
      } else {
        list.map(one => {
          urlEnding = one.urlEnding
        })
        urlEnding
      }

    }
  }

  def needRenderContent(id: Int): String = {
    val taskInfoAction = VideoFinalTask.filter(_.id === id).result
    val taskInfoList = Await.result(db.run(taskInfoAction), 5 seconds)
    var idContent = ""
    var language = ""
    var urlContent = ""
    taskInfoList.map(one => {
      idContent = one.idContent
      language = one.language
    })
    val action = VideoFinalTask.filterNot(_.id === id)
      .filter(_.idContent === idContent)
      .filter(_.language === language)
      .result
    val list = Await.result(db.run(action), 5 seconds)

    if (list.isEmpty) {
      ""
    } else {
      val action = VideoFinalTask
          .filterNot(_.id === id)
        .filterNot(_.urlContent === "")
        .filter(_.idContent === idContent)
        .filter(_.language === language)
        .result
      val list = Await.result(db.run(action), 5 seconds)
      if (list.isEmpty) {
        ""
      } else {
        list.map(one => {
          urlContent = one.urlContent
        })
        urlContent
      }

    }
  }


  override def addVideoFinalTask(read: VideoFinalTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = VideoFinalTaskRow(
      0, ts, ts,
      read.name,
      read.idContent,
      read.idEnding,
      read.status,
      read.videoTime,
      read.preViewPath,
      read.resultPath,
      read.videoValue,
      read.creator,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.urlEnding,
      read.language,
      read.urlContent
    )
    if (notExistVideoContentPath(read.name)) {
      val action = (VideoFinalTask returning VideoFinalTask.map(_.id)).+=(row)
      db.run(action)
    } else {
      Future.successful(-1)
    }
  }


  def notExistVideoContentPath(fa: String): Boolean = {
    val action = VideoFinalTask
      .filter(_.name === fa)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }

  def updateVideoFinalStatus(id: Int, read: VideoFinalStatus): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = VideoFinalTask
      .filter(_.id === id)
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

  def updateVideoFinalUrlContent(id: Int, read: VideoFinalUrlContent): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val urlContent = read.urlContent;
    // update sql
    try {
      db.run(sqlu""" update video_final_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_final_task set urlContent = $urlContent where id = $id and $urlContent != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateStatusAndResultPath(id: Int, read: VideoStatusAndResultPath): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val status = read.status;
    val resultPath = read.resultPath;
    // update sql
    try {
      db.run(sqlu""" update video_final_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_final_task set status = $status where id = $id and $status != "" """)
      db.run(sqlu""" update video_final_task set resultPath = $resultPath where id = $id and $resultPath != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }

  def updateVideoFinalFields(id: Int, read: VideoFinalUpdateFields): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val urlEnding = read.urlEnding;
    // update sql
    try {
      db.run(sqlu""" update video_final_task set update_time = $ts where id = $id """)
      db.run(sqlu""" update video_final_task set urlEnding = $urlEnding where id = $id and $urlEnding != "" """)
    } catch {
      case e: Exception =>
        logger.info(e.getMessage)
        Future.successful(-1)
    }
  }


  override def findList(read: VideoFinalTaskListRead): Seq[VideoFinalTaskData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from video_final_task $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[VideoFinalTaskRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

}