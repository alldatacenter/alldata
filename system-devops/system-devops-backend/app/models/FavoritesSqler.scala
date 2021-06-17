package models

import java.sql.Timestamp

import auto.Favorites.FavoritesTables.{Favorites, FavoritesRow}
import auto.Favorites.{FavoritesCreate, InnerFavoritesSqler}
import auto.Material.MaterialTables.Material
import controllers.FavoritesUpdateRead
import javax.inject.Inject
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class FavoritesSqler @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends InnerFavoritesSqler {

  def deleteFavoritesAndMaterials(id: Int, materialSqler: MaterialSqler): Future[Int] = {
    val items = materialSqler.findItems(id)
    items.map(item => {
      db.run(Material.filter(_.favoriteId === item.favoriteId ).delete)
    })
    val action = Favorites
      .filter(_.id === id)
      .delete
    db.run(action)
  }

  def addFavoritesCreate(read: FavoritesCreate): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    val row = FavoritesRow(
      0, ts, ts,
      read.name,
      read.description,
      read.username
    )
    if (notExistFavoriteName(read.name)) {
      val action = (Favorites returning Favorites.map(_.id)).+=(row)
      db.run(action)

    } else {
      Future.successful(-1)
    }
  }

  def notExistFavoriteName(fa: String): Boolean = {
    val action = Favorites
      .filter(_.favorite === fa)
      .result
    val list = Await.result(db.run(action), 5 seconds)
    list.isEmpty
  }

  def updateFavoritesRead(
                           id: Int,
                           read: FavoritesUpdateRead): Future[Int] = {
    val ts = new Timestamp(System.currentTimeMillis())
    if (notExistFavoriteName(read.name)) {
      val action = Favorites
        .filter(_.id === id)
        .filter(_.username === read.username)
        .map(read => (
          read.updateTime,
          read.favorite
        ))
        .update(
          ts,
          read.name
        )
      action.statements.foreach(println)
      db.run(action)
    } else {
      Future.successful(-1)
    }
  }

}