package auto.VideoContentTask

import auto.VideoContentTask.VideoContentTaskTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait VideoContentTaskJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeVideoContentTaskDataToVideoContentTaskRow(read: VideoContentTaskData): VideoContentTaskRow = {
    VideoContentTaskRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.aepPath,
      read.name,
      read.status,
      read.videoTime,
      read.finalTime,
      read.endingTime,
      read.preViewPath,
      read.resultPath,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.language,
      read.videoValue,
      read.tag
    )
  }

  implicit def changeVideoContentTaskRowToVideoContentTaskData(read: VideoContentTaskRow): VideoContentTaskData = {
     VideoContentTaskData(
              read.id,
              read.createTime,
              read.updateTime,
              read.aepPath,
              read.name,
              read.status,
              read.videoTime,
              read.finalTime,
              read.endingTime,
              read.preViewPath,
              read.resultPath,
              read.pixelLength,
              read.pixelHeight,
              read.frameRate,
              read.language,
              read.videoValue,
              read.tag
          )
  }

  implicit def changeVideoContentTaskRowSeqToVideoContentTaskDataSeq(data: Seq[VideoContentTaskRow]): Seq[VideoContentTaskData] = {
    data.map(a => changeVideoContentTaskRowToVideoContentTaskData(a))
  }

  implicit def changeVideoContentTaskDataSeqToVideoContentTaskRowSeq(data: Seq[VideoContentTaskData]): Seq[VideoContentTaskRow] = {
    data.map(a => changeVideoContentTaskDataToVideoContentTaskRow(a))
  }


  implicit val VideoContentTaskDataFormat = Json.format[VideoContentTaskData]
  implicit val VideoContentTaskReadFormat = Json.using[Json.WithDefaultValues].format[VideoContentTaskRead]
  implicit val VideoContentTaskListReadFormat = Json.format[VideoContentTaskListRead]
  implicit val ReVideoContentTaskFormat = Json.format[ReVideoContentTask]
}