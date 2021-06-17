package auto.Material

import java.sql.Timestamp

import auto.Material.MaterialTables._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerMaterialSqler extends HasDatabaseConfigProvider[JdbcProfile] with MaterialJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")
  val forbidKeyword = Array(";", "insert", "delete", "update", "drop", "alter", "join")

  def findByIdSync(id: Int): Seq[MaterialData] = {
    val action = Material.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

  def findList(read: MaterialListRead): Seq[MaterialData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from material $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[MaterialRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

  def checkSqlSecure(sql: String) = {
    val checkSql = sql.toLowerCase
    forbidKeyword.map(word => {
      if (checkSql.contains(word)) throw new Exception(s"not allow ${word} query")
    })
  }

  def addMaterial(read: MaterialRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = MaterialRow(
      0, ts, ts,
      read.itemId,
      read.favoriteId
    )
    val action = (Material returning Material.map(_.id)).+=(row)
    db.run(action)
  }


  def updateMaterial(
                      id: Int,
                      read: MaterialRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = Material
      .filter(_.id === id)
      .map(read => (
        read.updateTime,
        read.itemId,
        read.favoriteId
      ))
      .update(
        ts,
        read.itemId,
        read.favoriteId
      )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteMaterial(id: Int): Future[Int] = {
    val action = Material
      .filter(_.id === id)
      .delete
    db.run(action)
  }
}