package auto.VideoFinalTask

import java.sql.Timestamp

import auto.VideoFinalTask._
import auto.VideoFinalTask.VideoFinalTaskTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerVideoFinalTaskSqler extends HasDatabaseConfigProvider[JdbcProfile] with VideoFinalTaskJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[VideoFinalTaskData] = {
    val action = VideoFinalTask.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: VideoFinalTaskListRead): Seq[VideoFinalTaskData] = {
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

  val forbidKeyword = Array(";", "insert", "delete", "update", "drop", "alter", "join")

  def checkSqlSecure(sql: String) = {
    val checkSql = sql.toLowerCase
    forbidKeyword.map(word => {
      if (checkSql.contains(word)) throw new Exception(s"not allow ${word} query")
    })
  }

  def addVideoFinalTask(read: VideoFinalTaskRead): Future[Int] = {
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
              read.urlEnding,
              read.pixelLength,
              read.pixelHeight,
              read.frameRate,
              read.language,
              read.urlContent
          )
    val action = (VideoFinalTask returning VideoFinalTask.map(_.id)).+=(row)
    db.run(action)
  }


  def updateVideoFinalTask(
      id: Int,
                     read: VideoFinalTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = VideoFinalTask
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
          read.name,
          read.idContent,
          read.idEnding,
          read.status,
          read.videoTime,
          read.preViewPath,
          read.resultPath,
          read.videoValue,
          read.creator,
          read.urlEnding,
          read.pixelLength,
          read.pixelHeight,
          read.frameRate,
          read.language,
          read.urlContent
          ))
      .update(
        ts,
                  read.name,
                  read.idContent,
                  read.idEnding,
                  read.status,
                  read.videoTime,
                  read.preViewPath,
                  read.resultPath,
                  read.videoValue,
                  read.creator,
                  read.urlEnding,
                  read.pixelLength,
                  read.pixelHeight,
                  read.frameRate,
                  read.language,
                  read.urlContent
              )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteVideoFinalTask(
      id: Int                   ): Future[Int] = {
    val action = VideoFinalTask
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}