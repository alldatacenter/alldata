package auto.Favorites

import auto.Favorites.FavoritesTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait FavoritesJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeFavoritesDataToFavoritesRow(read: FavoritesData): FavoritesRow = {
    FavoritesRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.favorite,
      read.description,
      read.username
    )
  }

  implicit def changeFavoritesRowToFavoritesData(read: FavoritesRow): FavoritesData = {
     FavoritesData(
              read.id,
              read.createTime,
              read.updateTime,
              read.favorite,
              read.description,
              read.username
          )
  }

  implicit def changeFavoritesRowSeqToFavoritesDataSeq(data: Seq[FavoritesRow]): Seq[FavoritesData] = {
    data.map(a => changeFavoritesRowToFavoritesData(a))
  }

  implicit def changeFavoritesDataSeqToFavoritesRowSeq(data: Seq[FavoritesData]): Seq[FavoritesRow] = {
    data.map(a => changeFavoritesDataToFavoritesRow(a))
  }


  implicit val FavoritesDataFormat = Json.format[FavoritesData]
  implicit val FavoritesReadFormat = Json.using[Json.WithDefaultValues].format[FavoritesRead]
  implicit val ReFavoritesFormat = Json.format[ReFavorites]
  implicit val FavoritesListReadFormat = Json.format[FavoritesListRead]
}