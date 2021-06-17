package auto.Dict

import java.sql.Timestamp

import auto.Dict._
import auto.Dict.DictTables._
import controllers._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerDictSqler extends HasDatabaseConfigProvider[JdbcProfile] with DictJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")

  def findByIdSync(id: Int): Seq[DictData] = {
    val action = Dict.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }


  def findList(read: DictListRead): Seq[DictData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from dict $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[DictRow]
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

  def addDict(read: DictRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = DictRow(
      0, ts, ts,
              read.dictName,
              read.dictValue,
              read.dictDesc
          )
    val action = (Dict returning Dict.map(_.id)).+=(row)
    db.run(action)
  }


  def updateDict(
      id: Int,
                     read: DictRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = Dict
          .filter(_.id === id)
          .map(read => (
        read.updateTime,
          read.dictName,
          read.dictValue,
          read.dictDesc
          ))
      .update(
        ts,
                  read.dictName,
                  read.dictValue,
                  read.dictDesc
              )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteDict(
      id: Int                   ): Future[Int] = {
    val action = Dict
          .filter(_.id === id)
          .delete
    db.run(action)
  }
}