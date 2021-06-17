package auto.Material

import auto.Material.MaterialTables._
import controllers.{BeforeInnerJsonFormat, FavoriteItemRead}
import play.api.libs.json._

trait MaterialJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeMaterialDataToMaterialRow(read: MaterialData): MaterialRow = {
    MaterialRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.itemId,
      read.favoriteId
    )
  }

  implicit def changeMaterialRowToMaterialData(read: MaterialRow): MaterialData = {
     MaterialData(
              read.id,
              read.createTime,
              read.updateTime,
              read.itemId,
              read.favoriteId
          )
  }

  implicit def changeMaterialRowSeqToMaterialDataSeq(data: Seq[MaterialRow]): Seq[MaterialData] = {
    data.map(a => changeMaterialRowToMaterialData(a))
  }

  implicit def changeMaterialDataSeqToMaterialRowSeq(data: Seq[MaterialData]): Seq[MaterialRow] = {
    data.map(a => changeMaterialDataToMaterialRow(a))
  }


  implicit val MaterialDataFormat = Json.format[MaterialData]
  implicit val MaterialReadFormat = Json.using[Json.WithDefaultValues].format[MaterialRead]
  implicit val MaterialListReadFormat = Json.format[MaterialListRead]
  implicit val ReMaterialFormat = Json.format[ReMaterial]
  implicit val FavoriteItemReadFormat = Json.format[FavoriteItemRead]
}