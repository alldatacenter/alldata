package models

import java.sql.Timestamp

import auto.Favorites.FavoritesTables.Favorites
import auto.Material.MaterialTables.{Material, MaterialRow}
import auto.Material.{InnerMaterialSqler, MaterialCreate, MaterialData}
import controllers.FavoriteItemRead
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class MaterialSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerMaterialSqler {

  def deleteMaterialFromFavorite(read: FavoriteItemRead): Array[Int] = {
    val items = read.items.split(",")
    val reArray = ArrayBuffer[Int]()
    items.map(item => {
      val action = Material
        .filter(_.itemId === item)
        .filter(_.favoriteId === read.id)
        .delete
      val i = Await.result(db.run(action), 5 seconds)
      reArray.+=(i)
    })
    reArray.toArray
  }

  def findItems(id: Int): Seq[MaterialData] = {
    val querySql = s""" select * from material where favorite_id = $id  """
    logger.info(s"query sql: $querySql")
    checkSqlSecure(querySql)
    val action = sql"#$querySql".as[MaterialRow]
    val list = Await.result(db.run(action), 5 seconds)
    list
  }

  def addMaterialCreate(read: MaterialCreate): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = MaterialRow(
      0, ts, ts,
      read.items,
      read.id
    )
    if (!notExistFavorite(read.id) && notExistMaterial(read.items, read.id)) {
      val action = (Material returning Material.map(_.id)).+=(row)
      db.run(action)
    } else {
      Future.successful(-1)
    }
  }

  def notExistMaterial(itemId: String, favoriteId: Int): Boolean = {
    val action = Material
      .filter(_.itemId === itemId)
      .filter(_.favoriteId === favoriteId)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }

  def notExistFavorite(favoriteId: Int): Boolean = {
    val action = Favorites
      .filter(_.id === favoriteId)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }

  override def deleteMaterial(id: Int): Future[Int] = {
    val action = Material
      .filter(_.id === id)
      .delete
    db.run(action)
  }

}