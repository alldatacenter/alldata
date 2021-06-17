package auto.OriginalLevel2

import java.sql.Timestamp

import auto.OriginalLevel2._
import auto.OriginalLevel2.OriginalLevel2Tables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerOriginalLevel2Sqler extends HasDatabaseConfigProvider[JdbcProfile] with OriginalLevel2JsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[OriginalLevel2Data] = {
    val action = OriginalLevel2.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: OriginalLevel2ListRead): Seq[OriginalLevel2Data] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from original_level2 $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[OriginalLevel2Row]
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

  def addOriginalLevel2(read: OriginalLevel2Read): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = OriginalLevel2Row(
      0, ts, ts,
              read.originalLevel1Id,
              read.name
          )
    val action = (OriginalLevel2 returning OriginalLevel2.map(_.id)).+=(row)
    db.run(action)
  }


  def updateOriginalLevel2(
      id: Int,
                     read: OriginalLevel2Read): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = OriginalLevel2
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
          read.originalLevel1Id,
          read.name
          ))
      .update(
        ts,
                  read.originalLevel1Id,
                  read.name
              )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteOriginalLevel2(
      id: Int                   ): Future[Int] = {
    val action = OriginalLevel2
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}