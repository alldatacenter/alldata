package auto.OriginalLevel1

import auto.OriginalLevel1.OriginalLevel1Tables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait OriginalLevel1JsonFormat extends BeforeInnerJsonFormat {

  implicit def changeOriginalLevel1DataToOriginalLevel1Row(read: OriginalLevel1Data): OriginalLevel1Row = {
    OriginalLevel1Row(
      read.id,
      read.createTime,
      read.updateTime,
      read.originalGameId,
      read.name
    )
  }

  implicit def changeOriginalLevel1RowToOriginalLevel1Data(read: OriginalLevel1Row): OriginalLevel1Data = {
     OriginalLevel1Data(
              read.id,
              read.createTime,
              read.updateTime,
              read.originalGameId,
              read.name
          )
  }

  implicit def changeOriginalLevel1RowSeqToOriginalLevel1DataSeq(data: Seq[OriginalLevel1Row]): Seq[OriginalLevel1Data] = {
    data.map(a => changeOriginalLevel1RowToOriginalLevel1Data(a))
  }

  implicit def changeOriginalLevel1DataSeqToOriginalLevel1RowSeq(data: Seq[OriginalLevel1Data]): Seq[OriginalLevel1Row] = {
    data.map(a => changeOriginalLevel1DataToOriginalLevel1Row(a))
  }


  implicit val OriginalLevel1DataFormat = Json.format[OriginalLevel1Data]
  implicit val OriginalLevel1ReadFormat = Json.using[Json.WithDefaultValues].format[OriginalLevel1Read]
  implicit val OriginalLevel1ListReadFormat = Json.format[OriginalLevel1ListRead]
  implicit val ReOriginalLevel1Format = Json.format[ReOriginalLevel1]
}