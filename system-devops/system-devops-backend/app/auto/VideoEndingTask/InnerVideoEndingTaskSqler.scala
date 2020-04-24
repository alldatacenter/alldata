package auto.VideoEndingTask

import java.sql.Timestamp

import auto.VideoEndingTask._
import auto.VideoEndingTask.VideoEndingTaskTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerVideoEndingTaskSqler extends HasDatabaseConfigProvider[JdbcProfile] with VideoEndingTaskJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[VideoEndingTaskData] = {
    val action = VideoEndingTask.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: VideoEndingTaskListRead): Seq[VideoEndingTaskData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from video_ending_task $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[VideoEndingTaskRow]
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

  def addVideoEndingTask(read: VideoEndingTaskRead): Future[Int] = {
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
    val action = (VideoEndingTask returning VideoEndingTask.map(_.id)).+=(row)
    db.run(action)
  }


  def updateVideoEndingTask(
      id: Int,
                     read: VideoEndingTaskRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = VideoEndingTask
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
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
          ))
      .update(
        ts,
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
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteVideoEndingTask(
      id: Int                   ): Future[Int] = {
    val action = VideoEndingTask
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}