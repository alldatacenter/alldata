package auto.Dict

import auto.Dict.DictTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait DictJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeDictDataToDictRow(read: DictData): DictRow = {
    DictRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.dictName,
      read.dictValue,
      read.dictDesc
    )
  }

  implicit def changeDictRowToDictData(read: DictRow): DictData = {
     DictData(
              read.id,
              read.createTime,
              read.updateTime,
              read.dictName,
              read.dictValue,
              read.dictDesc
          )
  }

  implicit def changeDictRowSeqToDictDataSeq(data: Seq[DictRow]): Seq[DictData] = {
    data.map(a => changeDictRowToDictData(a))
  }

  implicit def changeDictDataSeqToDictRowSeq(data: Seq[DictData]): Seq[DictRow] = {
    data.map(a => changeDictDataToDictRow(a))
  }


  implicit val DictDataFormat = Json.format[DictData]
  implicit val DictReadFormat = Json.using[Json.WithDefaultValues].format[DictRead]
  implicit val DictListReadFormat = Json.format[DictListRead]
  implicit val ReDictFormat = Json.format[ReDict]
}