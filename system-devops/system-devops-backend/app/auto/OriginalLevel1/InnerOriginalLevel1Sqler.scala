package auto.OriginalLevel1

import java.sql.Timestamp

import auto.OriginalLevel1._
import auto.OriginalLevel1.OriginalLevel1Tables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerOriginalLevel1Sqler extends HasDatabaseConfigProvider[JdbcProfile] with OriginalLevel1JsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[OriginalLevel1Data] = {
    val action = OriginalLevel1.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: OriginalLevel1ListRead): Seq[OriginalLevel1Data] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from original_level1 $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[OriginalLevel1Row]
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

  def addOriginalLevel1(read: OriginalLevel1Read): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = OriginalLevel1Row(
      0, ts, ts,
              read.originalGameId,
              read.name
          )
    val action = (OriginalLevel1 returning OriginalLevel1.map(_.id)).+=(row)
    db.run(action)
  }


  def updateOriginalLevel1(
      id: Int,
                     read: OriginalLevel1Read): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = OriginalLevel1
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
          read.originalGameId,
          read.name
          ))
      .update(
        ts,
                  read.originalGameId,
                  read.name
              )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteOriginalLevel1(
      id: Int                   ): Future[Int] = {
    val action = OriginalLevel1
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}