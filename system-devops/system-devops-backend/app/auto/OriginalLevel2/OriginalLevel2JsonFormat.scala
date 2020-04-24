package auto.OriginalLevel2

import auto.OriginalLevel2.OriginalLevel2Tables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait OriginalLevel2JsonFormat extends BeforeInnerJsonFormat {

  implicit def changeOriginalLevel2DataToOriginalLevel2Row(read: OriginalLevel2Data): OriginalLevel2Row = {
    OriginalLevel2Row(
      read.id,
      read.createTime,
      read.updateTime,
      read.originalLevel1Id,
      read.name
    )
  }

  implicit def changeOriginalLevel2RowToOriginalLevel2Data(read: OriginalLevel2Row): OriginalLevel2Data = {
     OriginalLevel2Data(
              read.id,
              read.createTime,
              read.updateTime,
              read.originalLevel1Id,
              read.name
          )
  }

  implicit def changeOriginalLevel2RowSeqToOriginalLevel2DataSeq(data: Seq[OriginalLevel2Row]): Seq[OriginalLevel2Data] = {
    data.map(a => changeOriginalLevel2RowToOriginalLevel2Data(a))
  }

  implicit def changeOriginalLevel2DataSeqToOriginalLevel2RowSeq(data: Seq[OriginalLevel2Data]): Seq[OriginalLevel2Row] = {
    data.map(a => changeOriginalLevel2DataToOriginalLevel2Row(a))
  }


  implicit val OriginalLevel2DataFormat = Json.format[OriginalLevel2Data]
  implicit val OriginalLevel2ReadFormat = Json.using[Json.WithDefaultValues].format[OriginalLevel2Read]
  implicit val OriginalLevel2ListReadFormat = Json.format[OriginalLevel2ListRead]
  implicit val ReOriginalLevel2Format = Json.format[ReOriginalLevel2]
}