package auto.VideoContentTask

import java.sql.Timestamp

import auto.VideoContentTask._
import auto.VideoContentTask.VideoContentTaskTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerVideoContentTaskSqler extends HasDatabaseConfigProvider[JdbcProfile] with VideoContentTaskJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[VideoContentTaskData] = {
    val action = VideoContentTask.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: VideoContentTaskListRead): Seq[VideoContentTaskData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from video_content_task $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[VideoContentTaskRow]
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

  def addVideoContentTask(read: VideoContentTaskRead): Future[Int] = {
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
    val action = (VideoContentTask returning VideoContentTask.map(_.id)).+=(row)
    db.run(action)
  }


  def updateVideoContentTask(
      id: Int,
                     read: VideoContentTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = VideoContentTask
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
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
          ))
      .update(
        ts,
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
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteVideoContentTask(
      id: Int                   ): Future[Int] = {
    val action = VideoContentTask
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}