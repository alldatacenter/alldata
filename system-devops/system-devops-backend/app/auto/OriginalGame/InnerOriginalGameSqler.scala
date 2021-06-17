package auto.OriginalGame

import java.sql.Timestamp

import auto.OriginalGame._
import auto.OriginalGame.OriginalGameTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerOriginalGameSqler extends HasDatabaseConfigProvider[JdbcProfile] with OriginalGameJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[OriginalGameData] = {
    val action = OriginalGame.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: OriginalGameListRead): Seq[OriginalGameData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from original_game $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[OriginalGameRow]
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

  def addOriginalGame(read: OriginalGameRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = OriginalGameRow(
      0, ts, ts,
              read.name
          )
    val action = (OriginalGame returning OriginalGame.map(_.id)).+=(row)
    db.run(action)
  }


  def updateOriginalGame(
      id: Int,
                     read: OriginalGameRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = OriginalGame
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
          read.name
          ))
      .update(
        ts,
                  read.name
              )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteOriginalGame(
      id: Int                   ): Future[Int] = {
    val action = OriginalGame
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}