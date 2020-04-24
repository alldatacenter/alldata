package auto.OriginalGame

import auto.OriginalGame.OriginalGameTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait OriginalGameJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeOriginalGameDataToOriginalGameRow(read: OriginalGameData): OriginalGameRow = {
    OriginalGameRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.name
    )
  }

  implicit def changeOriginalGameRowToOriginalGameData(read: OriginalGameRow): OriginalGameData = {
     OriginalGameData(
              read.id,
              read.createTime,
              read.updateTime,
              read.name
          )
  }

  implicit def changeOriginalGameRowSeqToOriginalGameDataSeq(data: Seq[OriginalGameRow]): Seq[OriginalGameData] = {
    data.map(a => changeOriginalGameRowToOriginalGameData(a))
  }

  implicit def changeOriginalGameDataSeqToOriginalGameRowSeq(data: Seq[OriginalGameData]): Seq[OriginalGameRow] = {
    data.map(a => changeOriginalGameDataToOriginalGameRow(a))
  }


  implicit val OriginalGameDataFormat = Json.format[OriginalGameData]
  implicit val OriginalGameReadFormat = Json.using[Json.WithDefaultValues].format[OriginalGameRead]
  implicit val OriginalGameListReadFormat = Json.format[OriginalGameListRead]
  implicit val ReOriginalGameFormat = Json.format[ReOriginalGame]
}