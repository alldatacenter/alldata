package auto.Favorites

import java.sql.Timestamp

import auto.Favorites.FavoritesTables._
import org.slf4j.{Logger, LoggerFactory}
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait InnerFavoritesSqler extends HasDatabaseConfigProvider[JdbcProfile] with FavoritesJsonFormat {

  val logger: Logger = LoggerFactory.getLogger("application")
  val forbidKeyword = Array(";", "insert", "delete", "update", "drop", "alter", "join")

  def findByIdSync(id: Int): Seq[FavoritesData] = {
    val action = Favorites.filter(_.id === id).result
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

  def findList(read: FavoritesListRead): Seq[FavoritesData] = {
    val where = if (read.where.isDefined && read.where.get.nonEmpty) s"where ${read.where.get}" else ""
    val order = if (read.order.isDefined && read.order.get.nonEmpty) s" order by ${read.order.get} " else "order by  id  desc"
    val offset = read.offset
    val limit = read.limit
    val querySql = s""" select * from favorites $where $order limit $limit offset $offset  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[FavoritesRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

  def checkSqlSecure(sql: String) = {
    val checkSql = sql.toLowerCase
    forbidKeyword.map(word => {
      if (checkSql.contains(word)) throw new Exception(s"not allow ${word} query")
    })
  }

  def addFavorites(read: FavoritesRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = FavoritesRow(
      0, ts, ts,
      read.favorite,
      read.description,
      read.username
    )
    val action = (Favorites returning Favorites.map(_.id)).+=(row)
    db.run(action)
  }


  def updateFavorites(
                       id: Int,
                       read: FavoritesRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val action = Favorites
      .filter(_.id === id)
      .map(read => (
        read.updateTime,
        read.favorite,
        read.description,
        read.username
      ))
      .update(
        ts,
        read.favorite,
        read.description,
        read.username
      )
    action.statements.foreach(println)
    db.run(action)
  }

  def deleteFavorites(
                       id: Int): Future[Int] = {
    val action = Favorites
      .filter(_.id === id)
      .delete
    db.run(action)
  }
}