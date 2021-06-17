package auto.VideoFinalTask

import auto.VideoFinalTask.VideoFinalTaskTables._
import controllers.BeforeInnerJsonFormat
import play.api.libs.json._
import controllers._

trait VideoFinalTaskJsonFormat extends BeforeInnerJsonFormat {

  implicit def changeVideoFinalTaskDataToVideoFinalTaskRow(read: VideoFinalTaskData): VideoFinalTaskRow = {
    VideoFinalTaskRow(
      read.id,
      read.createTime,
      read.updateTime,
      read.name,
      read.idContent,
      read.idEnding,
      read.status,
      read.videoTime,
      read.preViewPath,
      read.resultPath,
      read.videoValue,
      read.creator,
      read.urlEnding,
      read.pixelLength,
      read.pixelHeight,
      read.frameRate,
      read.language,
      read.urlContent
    )
  }

  implicit def changeVideoFinalTaskRowToVideoFinalTaskData(read: VideoFinalTaskRow): VideoFinalTaskData = {
     VideoFinalTaskData(
              read.id,
              read.createTime,
              read.updateTime,
              read.name,
              read.idContent,
              read.idEnding,
              read.status,
              read.videoTime,
              read.preViewPath,
              read.resultPath,
              read.videoValue,
              read.creator,
              read.urlEnding,
              read.pixelLength,
              read.pixelHeight,
              read.frameRate,
              read.language,
              read.urlContent
          )
  }

  implicit def changeVideoFinalTaskRowSeqToVideoFinalTaskDataSeq(data: Seq[VideoFinalTaskRow]): Seq[VideoFinalTaskData] = {
    data.map(a => changeVideoFinalTaskRowToVideoFinalTaskData(a))
  }

  implicit def changeVideoFinalTaskDataSeqToVideoFinalTaskRowSeq(data: Seq[VideoFinalTaskData]): Seq[VideoFinalTaskRow] = {
    data.map(a => changeVideoFinalTaskDataToVideoFinalTaskRow(a))
  }


  implicit val VideoFinalTaskDataFormat = Json.format[VideoFinalTaskData]
  implicit val VideoFinalTaskReadFormat = Json.using[Json.WithDefaultValues].format[VideoFinalTaskRead]
  implicit val VideoFinalTaskListReadFormat = Json.format[VideoFinalTaskListRead]
  implicit val ReVideoFinalTaskFormat = Json.format[ReVideoFinalTask]
}